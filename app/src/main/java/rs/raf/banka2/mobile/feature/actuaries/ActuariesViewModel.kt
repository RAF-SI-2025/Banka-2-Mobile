package rs.raf.banka2.mobile.feature.actuaries

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
import rs.raf.banka2.mobile.data.dto.actuary.ActuaryDto
import rs.raf.banka2.mobile.data.repository.ActuaryRepository
import javax.inject.Inject

@HiltViewModel
class ActuariesViewModel @Inject constructor(
    private val repository: ActuaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ActuariesState())
    val state: StateFlow<ActuariesState> = _state.asStateFlow()

    private val _events = Channel<ActuariesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAgents()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, agents = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun updateLimit(employeeId: Long, dailyLimit: Double, needApproval: Boolean) = viewModelScope.launch {
        when (val result = repository.updateLimit(employeeId, dailyLimit, needApproval)) {
            is ApiResult.Success -> {
                _events.send(ActuariesEvent.Toast("Limit aktuara je azuriran."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun resetLimit(employeeId: Long) = viewModelScope.launch {
        when (val result = repository.resetLimit(employeeId)) {
            is ApiResult.Success -> {
                _events.send(ActuariesEvent.Toast("Iskoristeni limit je resetovan."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class ActuariesState(
    val loading: Boolean = false,
    val agents: List<ActuaryDto> = emptyList(),
    val error: String? = null
)

sealed interface ActuariesEvent {
    data class Toast(val message: String) : ActuariesEvent
}
