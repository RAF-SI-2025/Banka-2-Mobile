package rs.raf.banka2.mobile.feature.payments.create

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
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.InterbankRepository
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import rs.raf.banka2.mobile.data.repository.RecipientRepository
import javax.inject.Inject

/** Routing prefix nase banke (Banka 2). Sve sto je drugacije ide na inter-bank flow. */
private const val OUR_BANK_ROUTING = "222"

@HiltViewModel
class NewPaymentViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val recipientRepository: RecipientRepository,
    private val paymentRepository: PaymentRepository,
    private val interbankRepository: InterbankRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewPaymentState())
    val state: StateFlow<NewPaymentState> = _state.asStateFlow()

    private val _events = Channel<NewPaymentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadAccounts() }
        viewModelScope.launch { loadRecipients() }
    }

    fun selectAccount(account: AccountDto) =
        _state.update { it.copy(fromAccount = account) }

    fun selectRecipient(recipient: RecipientDto?) {
        _state.update {
            if (recipient == null) it.copy(selectedRecipient = null)
            else it.copy(
                selectedRecipient = recipient,
                recipientName = recipient.name,
                toAccountNumber = recipient.accountNumber
            )
        }
    }

    fun setRecipientName(value: String) = _state.update { it.copy(recipientName = value, error = null) }
    fun setToAccountNumber(value: String) = _state.update { it.copy(toAccountNumber = value, error = null) }
    fun setAmount(value: String) = _state.update { it.copy(amount = value, error = null) }
    fun setPurpose(value: String) = _state.update { it.copy(paymentPurpose = value, error = null) }
    fun setPaymentCode(value: String) = _state.update { it.copy(paymentCode = value, error = null) }
    fun setReferenceNumber(value: String) = _state.update { it.copy(referenceNumber = value) }

    fun openVerification() {
        val current = _state.value
        val parsedAmount = MoneyFormatter.parse(current.amount)
        when {
            current.fromAccount == null -> _state.update { it.copy(error = "Odaberi racun pošiljaoca.") }
            current.recipientName.isBlank() -> _state.update { it.copy(error = "Ime primaoca je obavezno.") }
            current.toAccountNumber.isBlank() -> _state.update { it.copy(error = "Broj racuna primaoca je obavezan.") }
            parsedAmount == null || parsedAmount <= 0.0 -> _state.update { it.copy(error = "Iznos mora biti veci od 0.") }
            current.paymentPurpose.isBlank() -> _state.update { it.copy(error = "Svrha placanja je obavezna.") }
            else -> _state.update {
                val routing = AccountFormatter.routingPrefix(current.toAccountNumber)
                val isInter = routing != null && routing != OUR_BANK_ROUTING
                it.copy(
                    error = null,
                    parsedAmount = parsedAmount,
                    showVerification = true,
                    isInterbank = isInter
                )
            }
        }
    }

    fun closeVerification() = _state.update { it.copy(showVerification = false) }

    fun submitWithCode(code: String) {
        val current = _state.value
        val account = current.fromAccount ?: return
        val parsedAmount = current.parsedAmount ?: return
        if (current.isInterbank) {
            startInterbankFlow(account, parsedAmount, code)
        } else {
            startIntraBankFlow(account, parsedAmount, code)
        }
    }

    fun closeInterbankProgress() = _state.update { it.copy(interbankProgress = null) }

    private fun startIntraBankFlow(account: AccountDto, parsedAmount: Double, code: String) {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(verifying = true) }
            val request = CreatePaymentRequestDto(
                fromAccountId = account.id,
                fromAccountNumber = account.accountNumber,
                toAccountNumber = current.toAccountNumber.trim(),
                amount = parsedAmount,
                currency = account.currency,
                recipientName = current.recipientName.trim(),
                paymentCode = current.paymentCode.ifBlank { "289" },
                paymentPurpose = current.paymentPurpose.trim(),
                referenceNumber = current.referenceNumber.takeIf { it.isNotBlank() },
                description = current.paymentPurpose.trim(),
                otpCode = code
            )
            when (val result = paymentRepository.create(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(verifying = false, showVerification = false) }
                    _events.send(NewPaymentEvent.Success(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(verifying = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun startInterbankFlow(account: AccountDto, parsedAmount: Double, code: String) {
        val current = _state.value
        viewModelScope.launch {
            _state.update {
                it.copy(
                    verifying = true,
                    showVerification = false,
                    interbankProgress = InterbankProgress(
                        transactionId = null,
                        status = "INITIATED",
                        message = "Pokrecem 2-Phase Commit transakciju..."
                    )
                )
            }
            val request = InitiateInterbankPaymentDto(
                fromAccountId = account.id,
                toAccountNumber = current.toAccountNumber.trim(),
                amount = parsedAmount,
                recipientName = current.recipientName.trim(),
                paymentPurpose = current.paymentPurpose.trim(),
                paymentCode = current.paymentCode.ifBlank { "289" },
                referenceNumber = current.referenceNumber.takeIf { it.isNotBlank() },
                otpCode = code
            )
            when (val result = interbankRepository.initiate(request)) {
                is ApiResult.Success -> {
                    val tx = result.data
                    _state.update {
                        it.copy(
                            verifying = false,
                            interbankProgress = InterbankProgress(
                                transactionId = tx.transactionId,
                                status = tx.status,
                                message = tx.message,
                                rate = tx.rate,
                                fee = tx.fee,
                                convertedAmount = tx.convertedAmount,
                                convertedCurrency = tx.convertedCurrency
                            )
                        )
                    }
                    if (tx.status !in TERMINAL_STATUSES) pollStatus(tx.transactionId)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(
                        verifying = false,
                        interbankProgress = it.interbankProgress?.copy(
                            status = "ABORTED",
                            message = result.error.message
                        )
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun pollStatus(transactionId: String) {
        repeat(40) {
            delay(3000L)
            when (val result = interbankRepository.status(transactionId)) {
                is ApiResult.Success -> {
                    val tx = result.data
                    _state.update {
                        it.copy(
                            interbankProgress = it.interbankProgress?.copy(
                                status = tx.status,
                                message = tx.message,
                                rate = tx.rate,
                                fee = tx.fee,
                                convertedAmount = tx.convertedAmount,
                                convertedCurrency = tx.convertedCurrency
                            )
                        )
                    }
                    if (tx.status in TERMINAL_STATUSES) return
                }
                else -> Unit
            }
        }
        _state.update {
            it.copy(
                interbankProgress = it.interbankProgress?.copy(
                    status = "STUCK",
                    message = "Status nije potvrdjen u predvidjenom vremenu — banka ce dovrsiti naknadno."
                )
            )
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    accounts = result.data,
                    fromAccount = result.data.firstOrNull { acc -> acc.currency.equals("RSD", true) }
                        ?: result.data.firstOrNull()
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadRecipients() {
        when (val result = recipientRepository.list()) {
            is ApiResult.Success -> _state.update { it.copy(recipients = result.data) }
            else -> Unit
        }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf("COMMITTED", "ABORTED", "STUCK", "NOT_READY")
    }
}

data class NewPaymentState(
    val accounts: List<AccountDto> = emptyList(),
    val recipients: List<RecipientDto> = emptyList(),
    val fromAccount: AccountDto? = null,
    val selectedRecipient: RecipientDto? = null,
    val recipientName: String = "",
    val toAccountNumber: String = "",
    val amount: String = "",
    val parsedAmount: Double? = null,
    val paymentPurpose: String = "",
    val paymentCode: String = "289",
    val referenceNumber: String = "",
    val error: String? = null,
    val showVerification: Boolean = false,
    val verifying: Boolean = false,
    val isInterbank: Boolean = false,
    val interbankProgress: InterbankProgress? = null
)

data class InterbankProgress(
    val transactionId: String?,
    val status: String,
    val message: String? = null,
    val rate: Double? = null,
    val fee: Double? = null,
    val convertedAmount: Double? = null,
    val convertedCurrency: String? = null
)

sealed interface NewPaymentEvent {
    data class Success(val paymentId: Long) : NewPaymentEvent
}
