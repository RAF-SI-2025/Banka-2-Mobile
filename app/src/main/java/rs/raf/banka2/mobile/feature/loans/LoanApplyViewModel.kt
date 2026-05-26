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
import java.math.BigDecimal
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

    /**
     * ME-09: pre nego pozove BE, otvaramo OTP modal (validacija prolazi → showVerification=true).
     * Sam BE poziv ide preko `submitWithOtp(code)`. BE BE-PAY-06 fix zahteva OTP za apply.
     */
    fun submit() {
        val current = _state.value
        // ME-11: parseBigDecimal — precision iznos kredita (spec C2 §255).
        val amount = MoneyFormatter.parseBigDecimal(current.amount)
        val duration = current.durationMonths.toIntOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _state.update { it.copy(error = "Iznos je obavezan.") }; return
        }
        if (duration == null || duration <= 0) {
            _state.update { it.copy(error = "Trajanje (broj meseci) je obavezno.") }; return
        }
        if (current.purpose.isBlank()) {
            _state.update { it.copy(error = "Svrha kredita je obavezna.") }; return
        }
        _state.update {
            it.copy(
                error = null,
                parsedAmount = amount,
                parsedDuration = duration,
                showVerification = true
            )
        }
    }

    fun closeVerification() = _state.update { it.copy(showVerification = false) }

    /**
     * ME-09: posto je OTP unet u VerificationModal-u, salje request sa otpCode.
     */
    fun submitWithOtp(code: String) {
        val current = _state.value
        val amount = current.parsedAmount ?: return
        val duration = current.parsedDuration ?: return
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
                monthlyIncome = MoneyFormatter.parseBigDecimal(current.monthlyIncome),
                employer = current.employer.takeIf { it.isNotBlank() },
                otpCode = code
            )
            when (val result = loanRepository.apply(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, showVerification = false) }
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
    // ME-09: cuvane vrednosti posle validacije, koriste se u submitWithOtp.
    val parsedAmount: BigDecimal? = null,
    val parsedDuration: Int? = null,
    val showVerification: Boolean = false,
    val error: String? = null
)

sealed interface LoanApplyEvent {
    data object Submitted : LoanApplyEvent
}
