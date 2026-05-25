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
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.CardRepository
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repository: CardRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardsState())
    val state: StateFlow<CardsState> = _state.asStateFlow()

    private val _events = Channel<CardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
        viewModelScope.launch { loadAccounts() }
    }

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

    /** ME-11: prima BigDecimal. */
    fun updateLimit(id: Long, newLimit: BigDecimal) = viewModelScope.launch {
        when (val result = repository.updateLimit(id, newLimit)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Limit je azuriran."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * ME-03: top-up INTERNET_PREPAID kartice — prebacuje iznos sa Account-a na karticu.
     */
    fun topUpCard(cardId: Long, sourceAccountId: Long, amount: BigDecimal) = viewModelScope.launch {
        when (val result = repository.topUp(cardId, sourceAccountId, amount)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Kartica je dopunjena."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * ME-03: withdraw sa INTERNET_PREPAID kartice nazad na Account.
     */
    fun withdrawFromCard(cardId: Long, targetAccountId: Long, amount: BigDecimal) = viewModelScope.launch {
        when (val result = repository.withdrawFromCard(cardId, targetAccountId, amount)) {
            is ApiResult.Success -> {
                _events.send(CardEvent.Toast("Sredstva su povucena na racun."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update { it.copy(accounts = result.data) }
            else -> Unit
        }
    }
}

data class CardsState(
    val loading: Boolean = false,
    val cards: List<CardDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val error: String? = null
)

sealed interface CardEvent {
    data class Toast(val message: String) : CardEvent
}
