package rs.raf.banka2.mobile.feature.payments.quickapprove

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * TODO_final Mobile bonus #7 — Quick Approve.
 *
 * Korisnik dolazi ovde preko deep-link-a iz notifikacije tipa
 * `PAYMENT_PENDING_APPROVAL`. Imamo paymentId + timestamp notifikacije.
 * Ako je proteklo > 5 minuta, link se smatra isteklim i Odobri je
 * disabled.
 *
 * Odobrenje gadja BE `POST /payments/{id}/approve` sa 6-cifrenim OTP-om
 * (TOTP). Na uspeh emituje toast + NavigateBack; na gresku prikazuje BE
 * poruku (401 pogresan OTP, 410 istekao link, 409 vec finalizovan, ...).
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
        // R3-1490: pre-compute expiry pa ga periodicno re-evaluiraj (vise nije
        // racunato samo jednom u init-u). Korisnik koji ostane na ekranu preko
        // 5 min sada vidi disabled "Odobri" cim link istekne.
        _state.update { it.copy(expired = computeExpired(notificationCreatedAt)) }
        loadPayment()
        startExpiryTicker()
    }

    fun refresh() = loadPayment()

    private fun startExpiryTicker() {
        // Bez timestamp-a nema sta da se tika (BE je tada autoritet za istek).
        if (notificationCreatedAt.isNullOrBlank()) return
        // Ako je vec istekao (ili je timestamp neparsabilan → fail-closed), ne tikamo.
        if (_state.value.expired) return
        // Izracunaj koliko jos sekundi do isteka pa zakazi tacno jedan flip.
        // (Bez beskonacne while-delay petlje — deterministicno za testove.)
        val remaining = runCatching {
            val created = Instant.parse(notificationCreatedAt)
            val deadline = created.plus(Duration.ofMinutes(5))
            Duration.between(Instant.now(), deadline)
        }.getOrNull() ?: return
        if (remaining.isNegative || remaining.isZero) {
            _state.update { it.copy(expired = true) }
            return
        }
        viewModelScope.launch {
            delay(remaining.toMillis() + 1000L)
            if (computeExpired(notificationCreatedAt)) {
                _state.update { it.copy(expired = true) }
            }
        }
    }

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

    fun setOtpCode(value: String) {
        // Dozvoljavamo samo cifre, max 6 (BE validira `\d{6}`).
        val sanitized = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(otpCode = sanitized) }
    }

    /**
     * Odobrava payment preko `POST /payments/{id}/approve` sa OTP-om koji je
     * korisnik uneo. Na uspeh: toast + NavigateBack. Na gresku: prikazi BE poruku.
     */
    fun onApproveRequested() {
        val otp = _state.value.otpCode
        if (paymentId <= 0L) {
            _state.update { it.copy(error = "Nevazeci paymentId u deep-link-u.") }
            return
        }
        // R1-587 / R3-1490: ne pokusavaj odobravanje ako je link istekao —
        // BE bi svakako vratio 410, ali ranija provera stedi round-trip i jasniji
        // je UX. `expired` se sada azurira preko tickera (vidi startExpiryTicker).
        if (_state.value.expired) {
            _state.update { it.copy(error = "Link za odobravanje je istekao.") }
            return
        }
        if (otp.length != 6) {
            _state.update { it.copy(error = "Verifikacioni kod mora imati 6 cifara.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(approving = true, error = null) }
            when (val result = paymentRepository.approveQuick(paymentId, otp)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(approving = false, status = result.data.status) }
                    _events.trySend(QuickApproveEvent.ShowMessage("Placanje je odobreno."))
                    _events.trySend(QuickApproveEvent.NavigateBack)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(approving = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun computeExpired(timestamp: String?): Boolean {
        // Bez timestamp-a ne mozemo izracunati istek — prepustamo BE-u (ne
        // zakljucavamo korisnika preventivno).
        if (timestamp.isNullOrBlank()) return false
        // R1-587: ako timestamp POSTOJI ali se ne moze parsirati, fail-CLOSED
        // (tretiramo kao istekao). Ranije je `getOrDefault(false)` znacilo da bi
        // korumpiran/nepoznat format bio tretiran kao svez → odobravanje isteklog
        // linka. Bezbednije je odbiti i traziti osvezavanje.
        return runCatching {
            val created = Instant.parse(timestamp)
            Duration.between(created, Instant.now()) > Duration.ofMinutes(5)
        }.getOrDefault(true)
    }
}

data class QuickApproveState(
    val paymentId: Long = -1L,
    val notificationCreatedAt: String? = null,
    val expired: Boolean = false,
    val loading: Boolean = true,
    val approving: Boolean = false,
    val otpCode: String = "",
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
