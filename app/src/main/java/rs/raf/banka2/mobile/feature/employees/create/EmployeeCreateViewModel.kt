package rs.raf.banka2.mobile.feature.employees.create

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
import rs.raf.banka2.mobile.data.dto.common.CreateEmployeeRequestDto
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import javax.inject.Inject

@HiltViewModel
class EmployeeCreateViewModel @Inject constructor(
    private val repository: EmployeeAdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeCreateState())
    val state: StateFlow<EmployeeCreateState> = _state.asStateFlow()

    private val _events = Channel<EmployeeCreateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun setFirstName(value: String) = _state.update { it.copy(firstName = value, error = null) }
    fun setLastName(value: String) = _state.update { it.copy(lastName = value, error = null) }
    fun setPhone(value: String) = _state.update { it.copy(phoneNumber = value) }
    fun setAddress(value: String) = _state.update { it.copy(address = value) }
    fun setGender(value: String) = _state.update { it.copy(gender = value) }
    fun setPosition(value: String) = _state.update { it.copy(position = value) }
    fun setDepartment(value: String) = _state.update { it.copy(department = value) }
    fun setIsAgent(value: Boolean) = _state.update { it.copy(isAgent = value) }
    fun setIsSupervisor(value: Boolean) = _state.update { it.copy(isSupervisor = value) }

    fun submit() {
        val current = _state.value
        if (current.email.isBlank() || current.firstName.isBlank() || current.lastName.isBlank()) {
            _state.update { it.copy(error = "Email, ime i prezime su obavezni.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val request = CreateEmployeeRequestDto(
                email = current.email.trim(),
                firstName = current.firstName.trim(),
                lastName = current.lastName.trim(),
                phoneNumber = current.phoneNumber.takeIf { it.isNotBlank() },
                address = current.address.takeIf { it.isNotBlank() },
                gender = current.gender.takeIf { it.isNotBlank() },
                position = current.position.takeIf { it.isNotBlank() },
                department = current.department.takeIf { it.isNotBlank() },
                isAgent = current.isAgent,
                isSupervisor = current.isSupervisor
            )
            when (val result = repository.create(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false) }
                    _events.send(EmployeeCreateEvent.Created(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class EmployeeCreateState(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val gender: String = "",
    val position: String = "",
    val department: String = "",
    val isAgent: Boolean = false,
    val isSupervisor: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface EmployeeCreateEvent {
    data class Created(val employeeId: Long) : EmployeeCreateEvent
}
