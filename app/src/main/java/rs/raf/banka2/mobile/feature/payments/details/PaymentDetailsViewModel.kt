package rs.raf.banka2.mobile.feature.payments.details

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
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import javax.inject.Inject

@HiltViewModel
class PaymentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PaymentRepository
) : ViewModel() {

    private val paymentId: Long = savedStateHandle["paymentId"] ?: 0L

    private val _state = MutableStateFlow(PaymentDetailsState())
    val state: StateFlow<PaymentDetailsState> = _state.asStateFlow()

    private val _events = Channel<PaymentDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.getPaymentById(paymentId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, payment = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun downloadReceipt() = viewModelScope.launch {
        _state.update { it.copy(downloading = true) }
        when (val result = repository.downloadReceipt(paymentId)) {
            is ApiResult.Success -> {
                val bytes = runCatching { result.data.bytes() }.getOrNull()
                _state.update { it.copy(downloading = false) }
                if (bytes != null) {
                    _events.send(PaymentDetailsEvent.ReceiptDownloaded(bytes))
                } else {
                    _events.send(PaymentDetailsEvent.Toast("Greska pri citanju potvrde."))
                }
            }
            is ApiResult.Failure -> {
                _state.update { it.copy(downloading = false) }
                _events.send(PaymentDetailsEvent.Toast(result.error.message))
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class PaymentDetailsState(
    val loading: Boolean = false,
    val payment: PaymentResponseDto? = null,
    val downloading: Boolean = false,
    val error: String? = null
)

sealed interface PaymentDetailsEvent {
    data class Toast(val message: String) : PaymentDetailsEvent
    data class ReceiptDownloaded(val pdfBytes: ByteArray) : PaymentDetailsEvent
}
