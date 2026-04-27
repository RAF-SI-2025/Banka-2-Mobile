package rs.raf.banka2.mobile.feature.tax

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
import rs.raf.banka2.mobile.data.dto.tax.TaxRecordDto
import rs.raf.banka2.mobile.data.repository.TaxRepository
import javax.inject.Inject

@HiltViewModel
class TaxViewModel @Inject constructor(
    private val repository: TaxRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaxState())
    val state: StateFlow<TaxState> = _state.asStateFlow()

    private val _events = Channel<TaxEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAll()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, records = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun calculate() = viewModelScope.launch {
        when (val result = repository.calculate()) {
            is ApiResult.Success -> {
                _events.send(TaxEvent.Toast("Obracun pokrenut."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class TaxState(
    val loading: Boolean = false,
    val records: List<TaxRecordDto> = emptyList(),
    val error: String? = null
)

sealed interface TaxEvent {
    data class Toast(val message: String) : TaxEvent
}
