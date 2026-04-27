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
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.AuthRepository
import javax.inject.Inject

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

    fun onTokenChange(value: String) = _state.update { it.copy(token = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmChange(value: String) = _state.update { it.copy(confirm = value, error = null) }

    fun submit() {
        val current = _state.value
        if (current.token.isBlank()) {
            _state.update { it.copy(error = "Token je obavezan.") }
            return
        }
        if (current.password.length < 8) {
            _state.update { it.copy(error = "Lozinka mora imati bar 8 karaktera.") }
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
                is ApiResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class ActivateState(
    val token: String = "",
    val password: String = "",
    val confirm: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

sealed interface ActivateEvent {
    data object Success : ActivateEvent
}
