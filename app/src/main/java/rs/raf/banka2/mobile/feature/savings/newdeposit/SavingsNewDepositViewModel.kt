package rs.raf.banka2.mobile.feature.savings.newdeposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.savings.OpenDepositRequest
import rs.raf.banka2.mobile.data.dto.savings.SavingsConstants
import rs.raf.banka2.mobile.data.dto.savings.SavingsRateDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import rs.raf.banka2.mobile.feature.savings.SavingsMath
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SavingsNewDepositViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavingsNewDepositState())
    val state: StateFlow<SavingsNewDepositState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadAccounts() }
        viewModelScope.launch { loadRates() }
    }

    fun setSource(id: Long) {
        val account = _state.value.accounts.find { it.id == id } ?: return
        _state.update { it.copy(sourceAccountId = id) }
        // reload rates for the currency of the selected source account
        val currency = account.currency
        if (!currency.isNullOrBlank()) {
            viewModelScope.launch { loadRates(currency) }
        }
    }

    fun setLinked(id: Long) = _state.update { it.copy(linkedAccountId = id) }

    fun setPrincipal(amount: BigDecimal) = _state.update { it.copy(principalAmount = amount) }

    fun setPrincipalText(text: String) {
        val parsed = text.toBigDecimalOrNull() ?: BigDecimal.ZERO
        _state.update { it.copy(principalAmount = parsed) }
    }

    fun setTerm(months: Int) = _state.update { it.copy(termMonths = months) }

    fun setAutoRenew(value: Boolean) = _state.update { it.copy(autoRenew = value) }

    fun openOtpDialog() {
        val s = _state.value
        val sourceId = s.sourceAccountId
        val linkedId = s.linkedAccountId
        val min = s.minAmount
        when {
            sourceId == null -> _state.update { it.copy(error = "Odaberi izvorni racun.") }
            linkedId == null -> _state.update { it.copy(error = "Odaberi vezani racun za isplatu.") }
            s.principalAmount < min -> _state.update {
                it.copy(error = "Minimalni iznos je ${min.toPlainString()} ${s.currencyCode}.")
            }
            else -> _state.update { it.copy(error = null, showOtp = true) }
        }
    }

    fun dismissOtp() = _state.update { it.copy(showOtp = false) }

    fun submit(otpCode: String) {
        val s = _state.value
        val sourceId = s.sourceAccountId ?: return
        val linkedId = s.linkedAccountId ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val req = OpenDepositRequest(
                sourceAccountId = sourceId,
                linkedAccountId = linkedId,
                principalAmount = s.principalAmount,
                termMonths = s.termMonths,
                autoRenew = s.autoRenew,
                otpCode = otpCode
            )
            when (val result = savingsRepository.openDeposit(req)) {
                is ApiResult.Success -> _state.update {
                    it.copy(submitting = false, showOtp = false, submitSuccess = true)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, showOtp = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> {
                val accounts = result.data
                val defaultSource = accounts.firstOrNull { it.currency.equals("RSD", true) }
                    ?: accounts.firstOrNull()
                _state.update {
                    it.copy(
                        accounts = accounts,
                        sourceAccountId = defaultSource?.id,
                        linkedAccountId = defaultSource?.id
                    )
                }
                val currency = defaultSource?.currency
                if (!currency.isNullOrBlank()) loadRates(currency)
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadRates(currency: String? = null) {
        when (val result = savingsRepository.getRates(currency)) {
            is ApiResult.Success -> _state.update { it.copy(rates = result.data.filter { r -> r.active }) }
            else -> Unit
        }
    }
}

data class SavingsNewDepositState(
    val accounts: List<AccountDto> = emptyList(),
    val rates: List<SavingsRateDto> = emptyList(),
    val sourceAccountId: Long? = null,
    val linkedAccountId: Long? = null,
    val principalAmount: BigDecimal = BigDecimal.ZERO,
    val termMonths: Int = 12,
    val autoRenew: Boolean = false,
    val submitting: Boolean = false,
    val showOtp: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false
) {
    val sourceAccount: AccountDto?
        get() = accounts.find { it.id == sourceAccountId }

    val currencyCode: String
        get() = sourceAccount?.currency ?: "RSD"

    val annualRate: BigDecimal
        get() = rates.firstOrNull { it.termMonths == termMonths }?.annualRate ?: BigDecimal.ZERO

    val monthlyInterest: BigDecimal
        get() = if (principalAmount > BigDecimal.ZERO && annualRate > BigDecimal.ZERO)
            SavingsMath.monthlyInterest(principalAmount, annualRate)
        else BigDecimal.ZERO

    val totalInterest: BigDecimal
        get() = if (principalAmount > BigDecimal.ZERO && annualRate > BigDecimal.ZERO)
            SavingsMath.totalInterestOverTerm(principalAmount, annualRate, termMonths)
        else BigDecimal.ZERO

    val penalty: BigDecimal
        get() = if (principalAmount > BigDecimal.ZERO)
            SavingsMath.earlyWithdrawalPenalty(principalAmount)
        else BigDecimal.ZERO

    val minAmount: BigDecimal
        get() = SavingsConstants.minDepositFor(currencyCode)
}
