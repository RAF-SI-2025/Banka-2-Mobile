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
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.CardRepository
import javax.inject.Inject

@HiltViewModel
class CardRequestNewViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardRequestState())
    val state: StateFlow<CardRequestState> = _state.asStateFlow()

    private val _events = Channel<CardRequestEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadAccounts() }
    }

    fun setAccount(account: AccountDto) = _state.update { it.copy(account = account) }
    fun setLimit(value: String) = _state.update { it.copy(limit = value, error = null) }
    fun setType(value: String) = _state.update { it.copy(cardType = value.uppercase()) }

    fun submit() {
        val current = _state.value
        val account = current.account ?: run {
            _state.update { it.copy(error = "Odaberi racun za karticu.") }
            return
        }
        val limit = MoneyFormatter.parse(current.limit) ?: 0.0
        if (limit <= 0.0) {
            _state.update { it.copy(error = "Limit mora biti veci od 0.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (val result = cardRepository.submitRequest(account.id, limit, current.cardType.ifBlank { "DEBIT" })) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        submitting = false,
                        pendingRequestId = result.data.id,
                        awaitingConfirmation = true
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun setConfirmCode(value: String) =
        _state.update { it.copy(confirmCode = value.filter { ch -> ch.isDigit() }.take(6), error = null) }

    fun confirm() {
        val current = _state.value
        val id = current.pendingRequestId ?: return
        if (current.confirmCode.length != 6) {
            _state.update { it.copy(error = "Kod iz email-a mora imati 6 cifara.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (val result = cardRepository.confirmRequest(id, current.confirmCode)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(CardRequestEvent.Submitted)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                it.copy(accounts = result.data, account = result.data.firstOrNull())
            }
            else -> Unit
        }
    }
}

data class CardRequestState(
    val accounts: List<AccountDto> = emptyList(),
    val account: AccountDto? = null,
    val limit: String = "",
    val cardType: String = "DEBIT",
    val submitting: Boolean = false,
    val awaitingConfirmation: Boolean = false,
    val pendingRequestId: Long? = null,
    val confirmCode: String = "",
    val error: String? = null
)

sealed interface CardRequestEvent {
    data object Submitted : CardRequestEvent
}
