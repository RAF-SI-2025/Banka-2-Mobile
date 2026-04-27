package rs.raf.banka2.mobile.feature.exchanges

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
import rs.raf.banka2.mobile.data.dto.listing.ExchangeManagementDto
import rs.raf.banka2.mobile.data.repository.ExchangeManagementRepository
import javax.inject.Inject

@HiltViewModel
class ExchangesViewModel @Inject constructor(
    private val repository: ExchangeManagementRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangesState())
    val state: StateFlow<ExchangesState> = _state.asStateFlow()

    private val _events = Channel<ExchangesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.list()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, exchanges = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun toggleTestMode(acronym: String, enable: Boolean) = viewModelScope.launch {
        when (val result = repository.toggleTestMode(acronym, enable)) {
            is ApiResult.Success -> {
                _events.send(ExchangesEvent.Toast(if (enable) "Test mode UKLJUCEN za $acronym" else "Test mode iskljucen za $acronym"))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class ExchangesState(
    val loading: Boolean = false,
    val exchanges: List<ExchangeManagementDto> = emptyList(),
    val error: String? = null
)

sealed interface ExchangesEvent {
    data class Toast(val message: String) : ExchangesEvent
}
