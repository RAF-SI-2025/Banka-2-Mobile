package rs.raf.banka2.mobile.feature.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotState())
    val state: StateFlow<ForgotState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, error = null, success = false)
    }

    fun submit() {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.update { it.copy(error = "Email je obavezan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.requestPasswordReset(current.email.trim())) {
                is ApiResult.Success -> _state.update {
                    it.copy(isSubmitting = false, success = true)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class ForgotState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
