package rs.raf.banka2.mobile.feature.clients.edit

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
import rs.raf.banka2.mobile.data.dto.common.ClientDto
import rs.raf.banka2.mobile.data.dto.common.UpdateClientRequestDto
import rs.raf.banka2.mobile.data.repository.ClientRepository
import javax.inject.Inject

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ClientRepository
) : ViewModel() {

    private val clientId: Long = savedStateHandle["clientId"] ?: 0L

    private val _state = MutableStateFlow(ClientEditState())
    val state: StateFlow<ClientEditState> = _state.asStateFlow()

    private val _events = Channel<ClientEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        when (val result = repository.byId(clientId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, client = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun save(firstName: String, lastName: String, phone: String, address: String) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        val request = UpdateClientRequestDto(
            firstName = firstName.takeIf { it.isNotBlank() },
            lastName = lastName.takeIf { it.isNotBlank() },
            phoneNumber = phone.takeIf { it.isNotBlank() },
            address = address.takeIf { it.isNotBlank() }
        )
        when (val result = repository.update(clientId, request)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false, client = result.data) }
                _events.send(ClientEditEvent.Toast("Klijent je azuriran."))
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class ClientEditState(
    val loading: Boolean = false,
    val client: ClientDto? = null,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface ClientEditEvent {
    data class Toast(val message: String) : ClientEditEvent
}
