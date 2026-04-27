package rs.raf.banka2.mobile.feature.supervisor.margincreate

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
import rs.raf.banka2.mobile.data.repository.MarginRepository
import javax.inject.Inject

@HiltViewModel
class CreateMarginViewModel @Inject constructor(
    private val repository: MarginRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateMarginState())
    val state: StateFlow<CreateMarginState> = _state.asStateFlow()

    private val _events = Channel<CreateMarginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setUserId(value: String) = _state.update { it.copy(userId = value.filter { ch -> ch.isDigit() }, error = null) }
    fun setCompanyId(value: String) = _state.update { it.copy(companyId = value.filter { ch -> ch.isDigit() }, error = null) }
    fun setInitialMargin(value: String) = _state.update { it.copy(initialMargin = value, error = null) }
    fun setMaintenanceMargin(value: String) = _state.update { it.copy(maintenanceMargin = value, error = null) }
    fun setBankParticipation(value: String) = _state.update { it.copy(bankParticipation = value, error = null) }

    fun submit() {
        val current = _state.value
        val initial = MoneyFormatter.parse(current.initialMargin)
        val maintenance = MoneyFormatter.parse(current.maintenanceMargin)
        val participation = MoneyFormatter.parse(current.bankParticipation)
        val userId = current.userId.toLongOrNull()
        val companyId = current.companyId.toLongOrNull()

        when {
            initial == null || initial <= 0 -> _state.update { it.copy(error = "Initial margin je obavezan i pozitivan.") }
            maintenance == null || maintenance <= 0 -> _state.update { it.copy(error = "Maintenance margin je obavezan i pozitivan.") }
            participation == null || participation < 0 || participation > 1 ->
                _state.update { it.copy(error = "Bank participation mora biti u [0..1] (0.5 = 50%).") }
            userId == null && companyId == null ->
                _state.update { it.copy(error = "Unesi userId ili companyId (samo jedan).") }
            userId != null && companyId != null ->
                _state.update { it.copy(error = "Unesi samo userId ILI companyId, ne oba.") }
            else -> {
                viewModelScope.launch {
                    _state.update { it.copy(submitting = true) }
                    when (val result = repository.create(initial, maintenance, participation, userId, companyId)) {
                        is ApiResult.Success -> {
                            _state.update { it.copy(submitting = false) }
                            _events.send(CreateMarginEvent.Created(result.data.id))
                        }
                        is ApiResult.Failure -> _state.update {
                            it.copy(submitting = false, error = result.error.message)
                        }
                        ApiResult.Loading -> Unit
                    }
                }
            }
        }
    }
}

data class CreateMarginState(
    val userId: String = "",
    val companyId: String = "",
    val initialMargin: String = "",
    val maintenanceMargin: String = "",
    val bankParticipation: String = "0.5",
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface CreateMarginEvent {
    data class Created(val accountId: Long) : CreateMarginEvent
}
