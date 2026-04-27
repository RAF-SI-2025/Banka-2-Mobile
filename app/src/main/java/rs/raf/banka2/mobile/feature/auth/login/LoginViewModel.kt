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
                    it.copy(isSubmitting = false, generalError = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
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
