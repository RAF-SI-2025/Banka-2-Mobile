package rs.raf.banka2.mobile.feature.auth.login

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
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = null, generalError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = null, generalError = null)
    }

    fun submit() {
        val current = _state.value
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, generalError = null) }
            when (val result = authRepository.login(current.email.trim(), current.password)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(LoginEvent.LoggedIn(result.data.role))
                }
                is ApiResult.Failure -> _state.update {
                    // Opciono.2: prepoznaj BE account-lockout poruku ("Account temporarily
                    // locked. Try again in N seconds.") i prevedi je u serpski UX tekst.
                    val raw = result.error.message
                    it.copy(isSubmitting = false, generalError = mapLoginError(raw))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    /**
     * Opciono.2: BE vraca poruku oblika "Account temporarily locked. Try again
     * in N seconds." kad je 5+ uzastopno pogresnih login pokusaja okidalo
     * `AuthRateLimitFilter`/lockout logiku. UI prikazuje serpski equivalent
     * sa minutama umesto sekundi (zaokruzeno na 1 min minimum).
     */
    private fun mapLoginError(raw: String?): String? {
        if (raw == null) return null
        if (!raw.startsWith("Account temporarily locked", ignoreCase = true)) return raw
        val seconds = Regex("(\\d+)\\s*seconds?", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val minutes = seconds?.let { ((it + 59) / 60).coerceAtLeast(1) }
        return if (minutes != null) {
            "Nalog je privremeno zakljucan. Pokusajte ponovo za $minutes min."
        } else {
            "Nalog je privremeno zakljucan zbog vise neuspesnih pokusaja. Pokusajte kasnije."
        }
    }

    private fun validateEmail(value: String): String? {
        if (value.isBlank()) return "Email je obavezan."
        val pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!pattern.matches(value.trim())) return "Email format nije ispravan."
        return null
    }

    private fun validatePassword(value: String): String? {
        if (value.isBlank()) return "Lozinka je obavezna."
        if (value.length < 6) return "Lozinka mora imati bar 6 karaktera."
        return null
    }
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false
)

sealed interface LoginEvent {
    data class LoggedIn(val role: UserRole) : LoginEvent
}
