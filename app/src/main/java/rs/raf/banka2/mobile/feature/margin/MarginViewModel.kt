package rs.raf.banka2.mobile.feature.margin

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
import rs.raf.banka2.mobile.data.repository.MarginRepository
import javax.inject.Inject

@HiltViewModel
class MarginViewModel @Inject constructor(
    private val repository: MarginRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarginState())
    val state: StateFlow<MarginState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.myAccounts()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, accounts = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun deposit(id: Long, amount: Double) = viewModelScope.launch {
        when (val result = repository.deposit(id, amount)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun withdraw(id: Long, amount: Double) = viewModelScope.launch {
        when (val result = repository.withdraw(id, amount)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class MarginState(
    val loading: Boolean = false,
    val accounts: List<MarginAccountDto> = emptyList(),
    val error: String? = null
)
