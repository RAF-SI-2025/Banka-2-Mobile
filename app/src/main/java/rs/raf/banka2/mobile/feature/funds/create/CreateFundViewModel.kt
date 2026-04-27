package rs.raf.banka2.mobile.feature.funds.create

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
import rs.raf.banka2.mobile.data.repository.FundRepository
import javax.inject.Inject

@HiltViewModel
class CreateFundViewModel @Inject constructor(
    private val repository: FundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateFundState())
    val state: StateFlow<CreateFundState> = _state.asStateFlow()

    private val _events = Channel<CreateFundEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setMinContribution(value: String) = _state.update { it.copy(minContribution = value, error = null) }

    fun submit() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(error = "Naziv fonda je obavezan.") }; return
        }
        val min = MoneyFormatter.parse(current.minContribution)
        if (min == null || min <= 0.0) {
            _state.update { it.copy(error = "Min ulog mora biti veci od 0.") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (val result = repository.create(current.name.trim(), current.description.takeIf { it.isNotBlank() }, min)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(CreateFundEvent.Created(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class CreateFundState(
    val name: String = "",
    val description: String = "",
    val minContribution: String = "",
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface CreateFundEvent {
    data class Created(val fundId: Long) : CreateFundEvent
}
