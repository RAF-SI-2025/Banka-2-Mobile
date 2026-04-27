package rs.raf.banka2.mobile.feature.supervisor.accountcreate

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
import rs.raf.banka2.mobile.data.dto.account.CreateAccountDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class CreateAccountForClientViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAccountForClientState())
    val state: StateFlow<CreateAccountForClientState> = _state.asStateFlow()

    private val _events = Channel<CreateAccountForClientEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setOwnerEmail(value: String) = _state.update { it.copy(ownerEmail = value, error = null) }
    fun setAccountType(value: String) = _state.update { it.copy(accountType = value.uppercase()) }
    fun setSubtype(value: String) = _state.update { it.copy(accountSubtype = value) }
    fun setCurrency(value: String) = _state.update { it.copy(currency = value.uppercase()) }
    fun setInitialDeposit(value: String) = _state.update { it.copy(initialDeposit = value) }
    fun setCreateCard(value: Boolean) = _state.update { it.copy(createCard = value) }

    fun submit() {
        val current = _state.value
        if (current.ownerEmail.isBlank() || current.accountType.isBlank() || current.currency.isBlank()) {
            _state.update { it.copy(error = "Email vlasnika, tip i valuta su obavezni.") }
            return
        }
        val deposit = MoneyFormatter.parse(current.initialDeposit) ?: 0.0
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val request = CreateAccountDto(
                accountType = current.accountType,
                accountSubtype = current.accountSubtype.takeIf { it.isNotBlank() },
                currency = current.currency,
                initialDeposit = deposit,
                ownerEmail = current.ownerEmail.trim(),
                createCard = current.createCard
            )
            when (val result = repository.createAccountForClient(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(CreateAccountForClientEvent.Created(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class CreateAccountForClientState(
    val ownerEmail: String = "",
    val accountType: String = "CHECKING",
    val accountSubtype: String = "",
    val currency: String = "RSD",
    val initialDeposit: String = "",
    val createCard: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface CreateAccountForClientEvent {
    data class Created(val accountId: Long) : CreateAccountForClientEvent
}
