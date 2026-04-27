package rs.raf.banka2.mobile.feature.supervisor.allloans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.loan.LoanDto
import rs.raf.banka2.mobile.data.repository.LoanRepository
import javax.inject.Inject

@HiltViewModel
class AllLoansViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AllLoansState())
    val state: StateFlow<AllLoansState> = _state.asStateFlow()

    init { refresh() }

    fun setStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAllLoans(status = _state.value.statusFilter)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, loans = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class AllLoansState(
    val loading: Boolean = false,
    val loans: List<LoanDto> = emptyList(),
    val statusFilter: String? = null,
    val error: String? = null
)
