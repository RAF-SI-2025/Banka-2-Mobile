package rs.raf.banka2.mobile.feature.margin.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginTransactionDto
import rs.raf.banka2.mobile.data.repository.MarginRepository
import javax.inject.Inject

@HiltViewModel
class MarginTransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MarginRepository
) : ViewModel() {

    private val accountId: Long = savedStateHandle["accountId"] ?: 0L

    private val _state = MutableStateFlow(MarginTransactionsState())
    val state: StateFlow<MarginTransactionsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { fetchAccount() }
        viewModelScope.launch { fetchTransactions() }
    }

    private suspend fun fetchAccount() {
        when (val result = repository.byId(accountId)) {
            is ApiResult.Success -> _state.update { it.copy(account = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchTransactions() {
        _state.update { it.copy(loading = true) }
        when (val result = repository.transactions(accountId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, transactions = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class MarginTransactionsState(
    val loading: Boolean = false,
    val account: MarginAccountDto? = null,
    val transactions: List<MarginTransactionDto> = emptyList(),
    val error: String? = null
)
