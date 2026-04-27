package rs.raf.banka2.mobile.feature.supervisor.accountcards

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
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.repository.CardRepository
import javax.inject.Inject

@HiltViewModel
class AccountCardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CardRepository
) : ViewModel() {

    private val accountId: Long = savedStateHandle["accountId"] ?: 0L

    private val _state = MutableStateFlow(AccountCardsState())
    val state: StateFlow<AccountCardsState> = _state.asStateFlow()

    private val _events = Channel<AccountCardsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.cardsForAccount(accountId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, cards = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun block(id: Long) = viewModelScope.launch {
        when (val result = repository.block(id)) {
            is ApiResult.Success -> { _events.send(AccountCardsEvent.Toast("Blokirana.")); refresh() }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun unblock(id: Long) = viewModelScope.launch {
        when (val result = repository.unblock(id)) {
            is ApiResult.Success -> { _events.send(AccountCardsEvent.Toast("Odblokirana.")); refresh() }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun deactivate(id: Long) = viewModelScope.launch {
        when (val result = repository.deactivate(id)) {
            is ApiResult.Success -> { _events.send(AccountCardsEvent.Toast("Deaktivirana.")); refresh() }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class AccountCardsState(
    val loading: Boolean = false,
    val cards: List<CardDto> = emptyList(),
    val error: String? = null
)

sealed interface AccountCardsEvent {
    data class Toast(val message: String) : AccountCardsEvent
}
