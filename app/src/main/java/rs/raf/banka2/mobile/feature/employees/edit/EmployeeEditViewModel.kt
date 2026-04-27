package rs.raf.banka2.mobile.feature.employees.edit

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
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.UpdateEmployeeRequestDto
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import javax.inject.Inject

@HiltViewModel
class EmployeeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeAdminRepository
) : ViewModel() {

    private val employeeId: Long = savedStateHandle["employeeId"] ?: 0L

    private val _state = MutableStateFlow(EmployeeEditState())
    val state: StateFlow<EmployeeEditState> = _state.asStateFlow()

    private val _events = Channel<EmployeeEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        when (val result = repository.byId(employeeId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, employee = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun update(name: String, lastName: String, phone: String, address: String, position: String, department: String,
               isAgent: Boolean, isSupervisor: Boolean, active: Boolean) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        val originalAgent = _state.value.employee?.isAgent ?: false
        val originalSupervisor = _state.value.employee?.isSupervisor ?: false
        val request = UpdateEmployeeRequestDto(
            firstName = name.takeIf { it.isNotBlank() },
            lastName = lastName.takeIf { it.isNotBlank() },
            phoneNumber = phone.takeIf { it.isNotBlank() },
            address = address.takeIf { it.isNotBlank() },
            position = position.takeIf { it.isNotBlank() },
            department = department.takeIf { it.isNotBlank() },
            active = active
        )
        // 1) Update profila zaposlenog (bez permisija)
        when (val result = repository.update(employeeId, request)) {
            is ApiResult.Success -> _state.update { it.copy(employee = result.data) }
            is ApiResult.Failure -> {
                _state.update { it.copy(submitting = false, error = result.error.message) }
                return@launch
            }
            ApiResult.Loading -> Unit
        }
        // 2) Permisije idu kroz zaseban PATCH endpoint (po spec-u Celine 1)
        if (isAgent != originalAgent || isSupervisor != originalSupervisor) {
            when (val permResult = repository.updatePermissions(employeeId, isAgent, isSupervisor)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, employee = permResult.data) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(submitting = false, error = permResult.error.message) }
                    return@launch
                }
                ApiResult.Loading -> Unit
            }
        } else {
            _state.update { it.copy(submitting = false) }
        }
        _events.send(EmployeeEditEvent.Toast("Zaposleni je azuriran."))
    }

    fun deactivate() = viewModelScope.launch {
        when (val result = repository.deactivate(employeeId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(employee = result.data) }
                _events.send(EmployeeEditEvent.Toast("Zaposleni je deaktiviran."))
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class EmployeeEditState(
    val loading: Boolean = false,
    val employee: EmployeeDto? = null,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface EmployeeEditEvent {
    data class Toast(val message: String) : EmployeeEditEvent
}
