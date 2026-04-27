package rs.raf.banka2.mobile.feature.funds.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.repository.FundRepository
import javax.inject.Inject

@HiltViewModel
class MyFundsViewModel @Inject constructor(
    private val repository: FundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyFundsState())
    val state: StateFlow<MyFundsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.myPositions()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, positions = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class MyFundsState(
    val loading: Boolean = false,
    val positions: List<FundPositionDto> = emptyList(),
    val error: String? = null
)
