package rs.raf.banka2.mobile.feature.payments.recipients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.repository.RecipientRepository
import javax.inject.Inject

@HiltViewModel
class RecipientsViewModel @Inject constructor(
    private val repository: RecipientRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipientsState())
    val state: StateFlow<RecipientsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.list()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, recipients = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun create(name: String, accountNumber: String, description: String?) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        when (val result = repository.create(name, accountNumber, description)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false) }
                refresh()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun update(id: Long, name: String, accountNumber: String, description: String?) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        when (val result = repository.update(id, name, accountNumber, description)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false) }
                refresh()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        when (val result = repository.delete(id)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class RecipientsState(
    val loading: Boolean = false,
    val recipients: List<RecipientDto> = emptyList(),
    val error: String? = null,
    val submitting: Boolean = false
)
