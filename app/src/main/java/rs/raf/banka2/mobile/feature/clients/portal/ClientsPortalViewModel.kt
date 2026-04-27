package rs.raf.banka2.mobile.feature.clients.portal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.common.ClientDto
import rs.raf.banka2.mobile.data.repository.ClientRepository
import javax.inject.Inject

@HiltViewModel
class ClientsPortalViewModel @Inject constructor(
    private val repository: ClientRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ClientsPortalState())
    val state: StateFlow<ClientsPortalState> = _state.asStateFlow()

    private var job: Job? = null

    init { refresh() }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value) }
        job?.cancel()
        job = viewModelScope.launch {
            delay(350L)
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = _state.value.search.takeIf { it.isNotBlank() }
        when (val result = repository.list(firstName = s, lastName = s, email = s)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, clients = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class ClientsPortalState(
    val loading: Boolean = false,
    val clients: List<ClientDto> = emptyList(),
    val search: String = "",
    val error: String? = null
)
