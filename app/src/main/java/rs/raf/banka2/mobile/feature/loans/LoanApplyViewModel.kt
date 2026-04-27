package rs.raf.banka2.mobile.feature.loans

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
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.LoanRepository
import javax.inject.Inject

@HiltViewModel
class LoanApplyViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanApplyState())
    val state: StateFlow<LoanApplyState> = _state.asStateFlow()

    private val _events = Channel<LoanApplyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { viewModelScope.launch { loadAccounts() } }

    fun setType(value: String) = _state.update { it.copy(loanType = value.uppercase()) }
    fun setAmount(value: String) = _state.update { it.copy(amount = value, error = null) }
    fun setDuration(value: String) = _state.update { it.copy(durationMonths = value.filter { ch -> ch.isDigit() }) }
    fun setPurpose(value: String) = _state.update { it.copy(purpose = value, error = null) }
    fun setIncome(value: String) = _state.update { it.copy(monthlyIncome = value) }
    fun setEmployer(value: String) = _state.update { it.copy(employer = value) }
    fun setAccount(account: AccountDto) = _state.update { it.copy(account = account) }

    fun submit() {
        val current = _state.value
        val amount = MoneyFormatter.parse(current.amount)
        val duration = current.durationMonths.toIntOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(error = "Iznos je obavezan.") }; return
        }
        if (duration == null || duration <= 0) {
            _state.update { it.copy(error = "Trajanje (broj meseci) je obavezno.") }; return
        }
        if (current.purpose.isBlank()) {
            _state.update { it.copy(error = "Svrha kredita je obavezna.") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val request = LoanApplicationDto(
                loanType = current.loanType.ifBlank { "CASH" },
                amount = amount,
                durationMonths = duration,
                purpose = current.purpose.trim(),
                accountId = current.account?.id,
                accountNumber = current.account?.accountNumber,
                currency = current.account?.currency,
                monthlyIncome = MoneyFormatter.parse(current.monthlyIncome),
                employer = current.employer.takeIf { it.isNotBlank() }
            )
            when (val result = loanRepository.apply(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(LoanApplyEvent.Submitted)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                it.copy(accounts = result.data, account = result.data.firstOrNull())
            }
            else -> Unit
        }
    }
}

data class LoanApplyState(
    val accounts: List<AccountDto> = emptyList(),
    val account: AccountDto? = null,
    val loanType: String = "CASH",
    val amount: String = "",
    val durationMonths: String = "",
    val purpose: String = "",
    val monthlyIncome: String = "",
    val employer: String = "",
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface LoanApplyEvent {
    data object Submitted : LoanApplyEvent
}
