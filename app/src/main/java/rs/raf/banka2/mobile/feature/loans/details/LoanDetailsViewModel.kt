package rs.raf.banka2.mobile.feature.loans.details

import androidx.lifecycle.SavedStateHandle
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
import rs.raf.banka2.mobile.data.dto.loan.LoanDto
import rs.raf.banka2.mobile.data.dto.loan.LoanInstallmentDto
import rs.raf.banka2.mobile.data.repository.LoanRepository
import javax.inject.Inject

@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LoanRepository
) : ViewModel() {

    private val loanId: Long = savedStateHandle["loanId"] ?: 0L

    private val _state = MutableStateFlow(LoanDetailsState())
    val state: StateFlow<LoanDetailsState> = _state.asStateFlow()

    private val _events = Channel<LoanDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() {
        viewModelScope.launch { fetchLoan() }
        viewModelScope.launch { fetchInstallments() }
    }

    fun earlyRepay() = viewModelScope.launch {
        _state.update { it.copy(submitting = true) }
        when (val result = repository.earlyRepay(loanId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false, loan = result.data) }
                _events.send(LoanDetailsEvent.Toast("Prevremena otplata zatrazena."))
                fetchInstallments()
            }
            is ApiResult.Failure -> {
                _state.update { it.copy(submitting = false, error = result.error.message) }
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchLoan() {
        _state.update { it.copy(loading = true) }
        when (val result = repository.loanDetails(loanId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, loan = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchInstallments() {
        when (val result = repository.installments(loanId)) {
            is ApiResult.Success -> _state.update { it.copy(installments = result.data) }
            else -> Unit
        }
    }
}

data class LoanDetailsState(
    val loading: Boolean = false,
    val loan: LoanDto? = null,
    val installments: List<LoanInstallmentDto> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface LoanDetailsEvent {
    data class Toast(val message: String) : LoanDetailsEvent
}
