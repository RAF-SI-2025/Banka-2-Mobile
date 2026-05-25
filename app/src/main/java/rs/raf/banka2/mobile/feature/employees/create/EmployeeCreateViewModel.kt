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
    fun setUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun setFirstName(value: String) = _state.update { it.copy(firstName = value, error = null) }
    fun setLastName(value: String) = _state.update { it.copy(lastName = value, error = null) }
    fun setDateOfBirth(value: String) = _state.update { it.copy(dateOfBirth = value, error = null) }
    fun setPhone(value: String) = _state.update { it.copy(phoneNumber = value) }
    fun setAddress(value: String) = _state.update { it.copy(address = value) }
    fun setGender(value: String) = _state.update { it.copy(gender = value) }
    fun setPosition(value: String) = _state.update { it.copy(position = value) }
    fun setDepartment(value: String) = _state.update { it.copy(department = value) }
    fun setIsAgent(value: Boolean) = _state.update { it.copy(isAgent = value) }
    fun setIsSupervisor(value: Boolean) = _state.update { it.copy(isSupervisor = value) }
    fun setIsAdmin(value: Boolean) = _state.update { it.copy(isAdmin = value) }

    /**
     * ME-12 fix: BE @NotBlank na svim poljima username/dateOfBirth/gender/phone/address/position/department.
     * Validacija mora pokriti sva ova polja ili BE odbija sa 400 + lista property/message parova.
     * dateOfBirth dodatno validira u proslosti (BE @Past) — ovde laksu provera (mora biti != prazno).
     */
    fun submit() {
        val current = _state.value
        val missing = buildList {
            if (current.email.isBlank()) add("email")
            if (current.username.isBlank()) add("username")
            if (current.firstName.isBlank()) add("ime")
            if (current.lastName.isBlank()) add("prezime")
            if (current.dateOfBirth.isBlank()) add("datum rodjenja")
            if (current.gender.isBlank()) add("pol")
            if (current.phoneNumber.isBlank()) add("telefon")
            if (current.address.isBlank()) add("adresu")
            if (current.position.isBlank()) add("poziciju")
            if (current.department.isBlank()) add("departman")
        }
        if (missing.isNotEmpty()) {
            _state.update { it.copy(error = "Popuni obavezna polja: ${missing.joinToString(", ")}.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val permissions = buildList {
                if (current.isAdmin) add("ADMIN")
                if (current.isSupervisor) add("SUPERVISOR")
                if (current.isAgent) add("AGENT")
            }
            val request = CreateEmployeeRequestDto(
                email = current.email.trim(),
                username = current.username.trim(),
                firstName = current.firstName.trim(),
                lastName = current.lastName.trim(),
                dateOfBirth = current.dateOfBirth.trim(),
                gender = current.gender.trim().uppercase(),
                phone = current.phoneNumber.trim(),
                address = current.address.trim(),
                position = current.position.trim(),
                department = current.department.trim(),
                active = true,
                permissions = permissions
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
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val gender: String = "",
    val position: String = "",
    val department: String = "",
    val isAgent: Boolean = false,
    val isSupervisor: Boolean = false,
    val isAdmin: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface EmployeeCreateEvent {
    data class Created(val employeeId: Long) : EmployeeCreateEvent
}
