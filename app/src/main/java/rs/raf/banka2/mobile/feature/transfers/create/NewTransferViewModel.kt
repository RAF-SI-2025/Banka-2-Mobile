package rs.raf.banka2.mobile.feature.transfers.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferFxRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferInternalRequestDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import rs.raf.banka2.mobile.data.repository.TransferRepository
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class NewTransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val exchangeRepository: ExchangeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewTransferState())
    val state: StateFlow<NewTransferState> = _state.asStateFlow()

    private val _events = Channel<NewTransferEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // R1-582: latest-wins za FX procenu — otkazujemo zastareli rate poziv.
    private var rateJob: Job? = null

    init {
        viewModelScope.launch { loadAccounts() }
    }

    fun setSource(account: AccountDto) {
        _state.update {
            // Auto-clear destination ako je ista kao novi izvor
            val target = if (it.toAccount?.id == account.id) null else it.toAccount
            it.copy(fromAccount = account, toAccount = target)
        }
        recalculateRate()
    }

    fun setDestination(account: AccountDto) {
        _state.update { it.copy(toAccount = account) }
        recalculateRate()
    }

    fun swap() {
        val from = _state.value.fromAccount
        val to = _state.value.toAccount
        _state.update { it.copy(fromAccount = to, toAccount = from) }
        recalculateRate()
    }

    fun setAmount(value: String) {
        _state.update { it.copy(amount = value, error = null) }
        recalculateRate()
    }

    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    fun openVerification() {
        val current = _state.value
        // ME-11: parseBigDecimal — precision iznos transfera (spec C2 §255).
        val parsed = MoneyFormatter.parseBigDecimal(current.amount)
        val from = current.fromAccount
        when {
            from == null -> _state.update { it.copy(error = "Odaberi izvorni racun.") }
            current.toAccount == null -> _state.update { it.copy(error = "Odaberi ciljni racun.") }
            from.id == current.toAccount.id -> _state.update {
                it.copy(error = "Izvor i cilj moraju biti razliciti racuni.")
            }
            parsed == null || parsed <= BigDecimal.ZERO -> _state.update { it.copy(error = "Unesi validan iznos.") }
            // R1 865: soft pre-check raspolozivog salda PRE OTP-a (UX) — iznos je u
            // valuti izvornog racuna pa poredimo direktno. BE je autoritet (rezervacije,
            // FX provizija) i i dalje moze odbiti, ali ovde sprecavamo nepotrebni OTP
            // korak za ocigledno nedovoljan saldo.
            parsed > from.availableBalance -> _state.update {
                it.copy(error = "Iznos prevazilazi raspolozivi saldo izvornog racuna.")
            }
            else -> _state.update { it.copy(parsedAmount = parsed, error = null, showVerification = true) }
        }
    }

    fun closeVerification() = _state.update { it.copy(showVerification = false) }

    fun submitWithCode(code: String) {
        val current = _state.value
        val from = current.fromAccount ?: return
        val to = current.toAccount ?: return
        val amount = current.parsedAmount ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            // P1-mobile-banking-1 (R1-125): BE trazi broj racuna (NE id) za oba kraja.
            val result = if (current.isFx) {
                transferRepository.fx(
                    TransferFxRequestDto(
                        fromAccountNumber = from.accountNumber,
                        toAccountNumber = to.accountNumber,
                        amount = amount,
                        otpCode = code
                    )
                )
            } else {
                transferRepository.internal(
                    TransferInternalRequestDto(
                        fromAccountNumber = from.accountNumber,
                        toAccountNumber = to.accountNumber,
                        amount = amount,
                        otpCode = code
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, showVerification = false) }
                    _events.send(NewTransferEvent.Success(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun recalculateRate() {
        val current = _state.value
        val from = current.fromAccount ?: return
        val to = current.toAccount ?: return
        if (from.currency.equals(to.currency, true)) {
            rateJob?.cancel()
            _state.update { it.copy(estimatedConverted = null, exchangeRate = null) }
            return
        }
        val parsed = MoneyFormatter.parse(current.amount) ?: return // exchange API koristi Double (UI estimate)
        rateJob?.cancel()
        rateJob = viewModelScope.launch {
            when (val result = exchangeRepository.calculate(parsed, from.currency, to.currency.orEmpty())) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        estimatedConverted = result.data.convertedAmount.toDouble(),
                        exchangeRate = (result.data.rate ?: result.data.exchangeRate)?.toDouble(),
                        // R1-578: prikazi i proviziju u confirm summary-ju (bila skrivena).
                        estimatedFee = result.data.fee
                    )
                }
                else -> Unit
            }
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                val first = result.data.firstOrNull { acc -> acc.currency.equals("RSD", true) }
                    ?: result.data.firstOrNull()
                val second = result.data.firstOrNull { acc -> acc.id != first?.id }
                it.copy(accounts = result.data, fromAccount = first, toAccount = second)
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class NewTransferState(
    val accounts: List<AccountDto> = emptyList(),
    val fromAccount: AccountDto? = null,
    val toAccount: AccountDto? = null,
    val amount: String = "",
    val parsedAmount: BigDecimal? = null,
    val description: String = "",
    val estimatedConverted: Double? = null,
    val exchangeRate: Double? = null,
    val estimatedFee: BigDecimal? = null, // R1-578: provizija za FX prenos (prikaz)
    val error: String? = null,
    val showVerification: Boolean = false,
    val submitting: Boolean = false
) {
    val isFx: Boolean
        get() = fromAccount != null && toAccount != null &&
            !fromAccount.currency.equals(toAccount.currency, true)
}

sealed interface NewTransferEvent {
    data class Success(val transferId: Long) : NewTransferEvent
}
