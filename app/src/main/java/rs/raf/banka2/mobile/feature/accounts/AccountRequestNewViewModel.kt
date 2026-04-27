package rs.raf.banka2.mobile.feature.accounts

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
import rs.raf.banka2.mobile.data.dto.account.AccountRequestDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class AccountRequestNewViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountRequestState())
    val state: StateFlow<AccountRequestState> = _state.asStateFlow()

    private val _events = Channel<AccountRequestEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setType(value: String) = _state.update { it.copy(accountType = value) }
    fun setSubtype(value: String) = _state.update { it.copy(accountSubtype = value) }
    fun setCurrency(value: String) = _state.update { it.copy(currency = value) }
    fun setInitialDeposit(value: String) = _state.update { it.copy(initialDeposit = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }
    fun setCreateCard(value: Boolean) = _state.update { it.copy(createCard = value) }

    fun submit() {
        val current = _state.value
        if (current.accountType.isBlank()) {
            _state.update { it.copy(error = "Tip racuna je obavezan.") }
            return
        }
        val deposit = MoneyFormatter.parse(current.initialDeposit) ?: 0.0
        val request = AccountRequestDto(
            accountType = current.accountType,
            accountSubtype = current.accountSubtype.takeIf { it.isNotBlank() },
            currency = current.currency.ifBlank { "RSD" },
            initialDeposit = deposit,
            createCard = current.createCard,
            note = current.note.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = accountRepository.submitAccountRequest(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(AccountRequestEvent.Submitted)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class AccountRequestState(
    val accountType: String = "CHECKING",
    val accountSubtype: String = "",
    val currency: String = "RSD",
    val initialDeposit: String = "",
    val note: String = "",
    val createCard: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface AccountRequestEvent {
    data object Submitted : AccountRequestEvent
}
