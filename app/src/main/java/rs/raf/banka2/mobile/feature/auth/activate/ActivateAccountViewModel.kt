package rs.raf.banka2.mobile.feature.auth.activate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.auth.PasswordPolicy
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Spec Sc 9 + ad-hoc bug 12.05.2026: pre nego sto Mobile renderuje formu za
 * aktivaciju, proverava status tokena. Bez ovog koraka, korisnik koji je vec
 * uspesno aktivirao nalog moze osveziti stranicu i ponovo videti formu —
 * submit puca sa BE 400 "Activation token already used or invalidated."
 *
 * Sad UI prikazuje:
 *   - VALID         → forma sa password + confirm
 *   - USED          → "Link je vec iskoriscen" + CTA "Idi na prijavu"
 *   - EXPIRED       → "Link za aktivaciju je istekao" + CTA "Nazad na prijavu"
 *   - INVALID       → "Nevazeci link" + CTA "Nazad na prijavu"
 *   - ALREADY_ACTIVE → "Nalog je vec aktivan" + CTA "Idi na prijavu"
 */
@HiltViewModel
class ActivateAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        ActivateState(token = savedStateHandle["token"] ?: "")
    )
    val state: StateFlow<ActivateState> = _state.asStateFlow()

    private val _events = Channel<ActivateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val initialToken = _state.value.token
        if (initialToken.isBlank()) {
            _state.update { it.copy(tokenStatus = TokenStatus.INVALID) }
        } else {
            checkTokenStatus(initialToken)
        }
    }

    /**
     * Pre-check stanja tokena. Endpoint je javan (bez auth). Greske
     * tretiramo kao INVALID — verovatno korisnik nema mreze ili je BE
     * nedostupan, ali u oba slucaja korisnik ne treba da vidi formu.
     */
    private fun checkTokenStatus(token: String) {
        viewModelScope.launch {
            when (val result = authRepository.activationTokenStatus(token)) {
                is ApiResult.Success -> {
                    val status = parseStatus(result.data.status)
                    _state.update {
                        it.copy(
                            tokenStatus = status,
                            tokenEmail = result.data.email
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(tokenStatus = TokenStatus.INVALID)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun parseStatus(raw: String): TokenStatus = when (raw.uppercase()) {
        "VALID" -> TokenStatus.VALID
        "USED" -> TokenStatus.USED
        "EXPIRED" -> TokenStatus.EXPIRED
        "ALREADY_ACTIVE" -> TokenStatus.ALREADY_ACTIVE
        else -> TokenStatus.INVALID
    }

    fun onTokenChange(value: String) = _state.update { it.copy(token = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmChange(value: String) = _state.update { it.copy(confirm = value, error = null) }

    fun submit() {
        val current = _state.value
        if (current.token.isBlank()) {
            _state.update { it.copy(error = "Token je obavezan.") }
            return
        }
        val passwordError = PasswordPolicy.validate(current.password)
        if (passwordError != null) {
            _state.update { it.copy(error = passwordError) }
            return
        }
        if (current.password != current.confirm) {
            _state.update { it.copy(error = "Lozinke se ne poklapaju.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.activateAccount(current.token.trim(), current.password)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(ActivateEvent.Success)
                }
                is ApiResult.Failure -> {
                    val msg = result.error.message ?: "Aktivacija nije uspela."
                    // Race condition: ako BE javi da je token vec iskoriscen ili
                    // istekao izmedju mount-pre-check i submit-a (drugi tab, admin
                    // invalidacija), refresh-uj tokenStatus umesto generic toast-a.
                    val refreshNeeded = msg.contains("already used", ignoreCase = true)
                            || msg.contains("invalidated", ignoreCase = true)
                            || msg.contains("expired", ignoreCase = true)
                            || msg.contains("Activation token", ignoreCase = true)
                    if (refreshNeeded) {
                        _state.update { it.copy(isSubmitting = false) }
                        checkTokenStatus(current.token.trim())
                    } else {
                        _state.update { it.copy(isSubmitting = false, error = msg) }
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

enum class TokenStatus {
    CHECKING,
    VALID,
    USED,
    EXPIRED,
    INVALID,
    ALREADY_ACTIVE
}

data class ActivateState(
    val token: String = "",
    val password: String = "",
    val confirm: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val tokenStatus: TokenStatus = TokenStatus.CHECKING,
    val tokenEmail: String? = null
)

sealed interface ActivateEvent {
    data object Success : ActivateEvent
}
