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

    fun earlyRepay(loanId: Long) = viewModelScope.launch {
        when (val result = repository.earlyRepay(loanId)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class LoansState(
    val loading: Boolean = false,
    val loans: List<LoanDto> = emptyList(),
    val applications: List<LoanApplicationResponseDto> = emptyList(),
    val error: String? = null
)
