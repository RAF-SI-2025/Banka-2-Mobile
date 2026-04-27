package rs.raf.banka2.mobile.feature.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import javax.inject.Inject

/**
 * Mobile-jedinstvena strana — prikazuje aktivni OTP kod (drugi faktor)
 * koji korisnik ukucava na web aplikaciji za autorizaciju placanja.
 *
 * Polluje `/payments/my-otp` svake 5 sekundi i prikazuje countdown.
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OtpState())
    val state: StateFlow<OtpState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.getActiveOtp()) {
                is ApiResult.Success -> _state.update {
                    val data = result.data
                    it.copy(
                        loading = false,
                        active = data.active,
                        code = data.code,
                        secondsLeft = data.secondsLeft ?: 0,
                        attempts = data.attempts,
                        maxAttempts = data.maxAttempts,
                        message = data.message
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun tickOneSecond() = _state.update {
        if (it.secondsLeft > 0) it.copy(secondsLeft = it.secondsLeft - 1) else it
    }
}

data class OtpState(
    val loading: Boolean = false,
    val active: Boolean = false,
    val code: String? = null,
    val secondsLeft: Int = 0,
    val attempts: Int? = null,
    val maxAttempts: Int? = null,
    val message: String? = null,
    val error: String? = null
)
