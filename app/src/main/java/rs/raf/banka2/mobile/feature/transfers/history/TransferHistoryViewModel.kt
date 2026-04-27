package rs.raf.banka2.mobile.feature.transfers.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.transfer.TransferResponseDto
import rs.raf.banka2.mobile.data.repository.TransferRepository
import javax.inject.Inject

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val repository: TransferRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransferHistoryState())
    val state: StateFlow<TransferHistoryState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.listMyTransfers()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, transfers = result.data) }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class TransferHistoryState(
    val loading: Boolean = false,
    val transfers: List<TransferResponseDto> = emptyList(),
    val error: String? = null
)
