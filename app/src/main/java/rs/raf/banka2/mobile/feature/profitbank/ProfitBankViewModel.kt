package rs.raf.banka2.mobile.feature.profitbank

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
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.profitbank.ActuaryProfitDto
import rs.raf.banka2.mobile.data.dto.profitbank.BankFundPositionDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import rs.raf.banka2.mobile.data.repository.ProfitBankRepository
import javax.inject.Inject

/**
 * Spec Celina 4 (Nova) §4593-4630: pored svake bankine pozicije u fondu
 * supervizor moze da pokrene "Uplata u fond" ili "Povlacenje" koristeci
 * jedan od bankinih racuna. Ovaj ViewModel mi orchestriira oba flow-a
 * preko `FundRepository.invest`/`withdraw` koji dele isti BE endpoint
 * kao i klijentske akcije (BE razlikuje banku po `owner_client_id` polju).
 */
@HiltViewModel
class ProfitBankViewModel @Inject constructor(
    private val repository: ProfitBankRepository,
    private val accountRepository: AccountRepository,
    private val fundRepository: FundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfitBankState())
    val state: StateFlow<ProfitBankState> = _state.asStateFlow()

    private val _events = Channel<ProfitBankEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
        viewModelScope.launch { loadAccounts() }
    }

    fun refresh() {
        viewModelScope.launch { loadActuaries() }
        viewModelScope.launch { loadFundPositions() }
    }

    fun openInvestDialog(position: BankFundPositionDto) =
        _state.update { it.copy(investTarget = position) }

    fun openWithdrawDialog(position: BankFundPositionDto) =
        _state.update { it.copy(withdrawTarget = position) }

    fun closeDialogs() = _state.update { it.copy(investTarget = null, withdrawTarget = null) }

    fun invest(fundId: Long, sourceAccountId: Long, amount: Double) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (val result = fundRepository.invest(fundId, sourceAccountId, amount)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, investTarget = null) }
                    _events.send(ProfitBankEvent.Toast("Uplata u fond uspesna."))
                    refresh()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun withdraw(fundId: Long, destinationAccountId: Long, amount: Double?, withdrawAll: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (val result = fundRepository.withdraw(fundId, destinationAccountId, amount, withdrawAll)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, withdrawTarget = null) }
                    _events.send(ProfitBankEvent.Toast("Povlacenje pokrenuto."))
                    refresh()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadActuaries() {
        when (val result = repository.actuaryProfits()) {
            is ApiResult.Success -> _state.update { it.copy(actuaries = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadFundPositions() {
        when (val result = repository.bankFundPositions()) {
            is ApiResult.Success -> _state.update { it.copy(fundPositions = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * Supervizor bira jedan od bankinih racuna za uplatu/isplatu. BE filter:
     * `accounts/all` vraca samo racune kojima supervizor ima pristup; bankini
     * racuni dolaze sa `ownerName == "Banka"` ili imaju karakteristican prefix
     * (222...). UI prikazuje listu, bez filtriranja na FE strani.
     */
    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update { it.copy(accounts = result.data) }
            is ApiResult.Failure -> Unit
            ApiResult.Loading -> Unit
        }
    }
}

data class ProfitBankState(
    val actuaries: List<ActuaryProfitDto> = emptyList(),
    val fundPositions: List<BankFundPositionDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val investTarget: BankFundPositionDto? = null,
    val withdrawTarget: BankFundPositionDto? = null,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface ProfitBankEvent {
    data class Toast(val message: String) : ProfitBankEvent
}
