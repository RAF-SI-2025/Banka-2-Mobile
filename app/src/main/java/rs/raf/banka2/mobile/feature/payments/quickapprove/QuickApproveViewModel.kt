package rs.raf.banka2.mobile.feature.payments.quickapprove

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
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * TODO_final Mobile bonus #7 — Quick Approve placeholder.
 *
 * Korisnik dolazi ovde preko deep-link-a iz notifikacije tipa
 * `PAYMENT_PENDING_APPROVAL`. Imamo paymentId + timestamp notifikacije.
 * Ako je proteklo > 5 minuta, link se smatra isteklim i Odobri je
 * disabled.
 *
 * NAPOMENA: BE jos uvek nema dedikovan `POST /payments/{id}/approve`
 * endpoint koji bi prihvatio OTP iz Quick Approve flow-a (FCM nije
 * konfigurisan, nema cetiri-faznog approve flow-a). Za sada ovaj
 * placeholder samo cita Payment preko `paymentRepository.getById`
 * + pokazuje countdown + onClick na "Odobri" prikazuje toast da
 * je BE endpoint TODO. Kad BE bude implementirao endpoint,
 * `approve()` metoda ce ga zvati direktno.
 */
@HiltViewModel
class QuickApproveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val paymentId: Long = savedStateHandle.get<Long>("paymentId") ?: -1L
    private val notificationCreatedAt: String? =
        savedStateHandle.get<String>("notificationCreatedAt")

    private val _state = MutableStateFlow(
        QuickApproveState(paymentId = paymentId, notificationCreatedAt = notificationCreatedAt)
    )
    val state: StateFlow<QuickApproveState> = _state.asStateFlow()

    private val _events = Channel<QuickApproveEvent>(Channel.BUFFERED)
    val events get() = _events.receiveAsFlow()

    init {
        // Pre-compute expiry status iz timestamp-a (best-effort parse).
        _state.update { it.copy(expired = computeExpired(notificationCreatedAt)) }
        loadPayment()
    }

    fun refresh() = loadPayment()

    private fun loadPayment() {
        if (paymentId <= 0L) {
            _state.update { it.copy(loading = false, error = "Nevazeci paymentId u deep-link-u.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = paymentRepository.getPaymentById(paymentId)) {
                is ApiResult.Success -> {
                    val p = result.data
                    _state.update {
                        it.copy(
                            loading = false,
                            amount = p.amount.toString(),
                            currencyCode = p.currency,
                            recipientName = p.recipientName,
                            recipientAccount = p.toAccount,
                            purpose = p.description,
                            status = p.status,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _events.trySend(QuickApproveEvent.NavigateBack)
        }
    }

    /**
     * BE endpoint placeholder — kad bude implementiran, ovde poziva
     * `paymentRepository.approveQuick(paymentId, otpCode)`.
     */
    fun onApproveRequested() {
        viewModelScope.launch {
            _events.trySend(
                QuickApproveEvent.ShowMessage(
                    "Quick Approve endpoint jos nije implementiran na BE-u. " +
                        "U punoj integraciji bi se ovde otvorio OTP modal + POST /payments/$paymentId/approve."
                )
            )
        }
    }

    private fun computeExpired(timestamp: String?): Boolean {
        if (timestamp.isNullOrBlank()) return false
        return runCatching {
            val created = Instant.parse(timestamp)
            Duration.between(created, Instant.now()) > Duration.ofMinutes(5)
        }.getOrDefault(false)
    }
}

data class QuickApproveState(
    val paymentId: Long = -1L,
    val notificationCreatedAt: String? = null,
    val expired: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val amount: String? = null,
    val currencyCode: String? = null,
    val recipientName: String? = null,
    val recipientAccount: String? = null,
    val purpose: String? = null,
    val status: String? = null,
)

sealed interface QuickApproveEvent {
    data class ShowMessage(val message: String) : QuickApproveEvent
    data object NavigateBack : QuickApproveEvent
}
