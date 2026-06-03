package rs.raf.banka2.mobile.feature.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationResponseDto
import rs.raf.banka2.mobile.data.dto.loan.LoanDto
import rs.raf.banka2.mobile.data.repository.LoanRepository
import javax.inject.Inject

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoansState())
    val state: StateFlow<LoansState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val loansResult = repository.myLoans()
            val appsResult = repository.myApplications()
            _state.update {
                it.copy(
                    loading = false,
                    loans = (loansResult as? ApiResult.Success)?.data.orEmpty(),
                    applications = (appsResult as? ApiResult.Success)?.data.orEmpty(),
                    error = (loansResult as? ApiResult.Failure)?.error?.message
                )
            }
        }
    }

    /**
     * P1-mobile-banking-1 (R1-263): prevremena otplata je OTP-gated (BE-PAY-06).
     * Otvori VerificationModal; stvarni poziv ide kroz [earlyRepayWithOtp].
     */
    fun requestEarlyRepay(loanId: Long) = _state.update {
        it.copy(earlyRepayLoanId = loanId, showVerification = true, error = null)
    }

    fun closeVerification() = _state.update { it.copy(showVerification = false, earlyRepayLoanId = null) }

    fun earlyRepayWithOtp(code: String) = viewModelScope.launch {
        val loanId = _state.value.earlyRepayLoanId ?: return@launch
        _state.update { it.copy(submitting = true) }
        when (val result = repository.earlyRepay(loanId, code)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false, showVerification = false, earlyRepayLoanId = null) }
                refresh()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class LoansState(
    val loading: Boolean = false,
    val loans: List<LoanDto> = emptyList(),
    val applications: List<LoanApplicationResponseDto> = emptyList(),
    val error: String? = null,
    val showVerification: Boolean = false,
    val earlyRepayLoanId: Long? = null,
    val submitting: Boolean = false
)
