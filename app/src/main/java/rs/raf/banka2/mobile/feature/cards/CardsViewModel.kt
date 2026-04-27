package rs.raf.banka2.mobile.feature.cards

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
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.repository.CardRepository
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardsState())
    val state: StateFlow<CardsState> = _state.asStateFlow()

    private val _events = Channel<CardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.myCards()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, cards = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun blockCard(id: Long) = viewModelScope.launch {
        when (val result = repository.block(id)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Kartica je blokirana."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun unblockCard(id: Long) = viewModelScope.launch {
        when (val result = repository.unblock(id)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Kartica je odblokirana."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun deactivateCard(id: Long) = viewModelScope.launch {
        when (val result = repository.deactivate(id)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Kartica je deaktivirana."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun updateLimit(id: Long, newLimit: Double) = viewModelScope.launch {
        when (val result = repository.updateLimit(id, newLimit)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Limit je azuriran."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class CardsState(
    val loading: Boolean = false,
    val cards: List<CardDto> = emptyList(),
    val error: String? = null
)

sealed interface CardEvent {
    data class Toast(val message: String) : CardEvent
}
