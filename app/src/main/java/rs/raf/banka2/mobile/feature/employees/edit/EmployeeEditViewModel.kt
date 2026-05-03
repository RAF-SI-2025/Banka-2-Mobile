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
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import javax.inject.Inject

/**
 * Spec Celina 4 (Nova) §3850-3878:
 *  > Ako admin ukloni permisiju isSupervisor supervizoru koji upravlja nekim
 *  > fondovima, vlasnistvo fondova se prebacuje na tog admina.
 *
 * UI flow: kad admin pokuse update sa `isSupervisor=false` na zaposlenom kome je
 * pre toga vrednost bila `true`, prvo proveravamo da li taj zaposleni manage-uje
 * fondove. Ako da, otvaramo confirmation dialog koji navodi listu fondova; klik
 * "Potvrdi" nastavlja update (BE ce uraditi reassign na trenutnog admin-a koji
 * radi promenu).
 */
@HiltViewModel
class EmployeeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeAdminRepository,
    private val fundRepository: FundRepository
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

    /**
     * Pokrece update flow. Ako admin pokusava da ukloni `isSupervisor` permisiju
     * sa zaposlenog koji upravlja fondovima, prvo otvaramo `pendingReassign`
     * confirmation. UI ce pozvati `confirmReassign(...)` kad korisnik klikne
     * Potvrdi.
     */
    fun update(name: String, lastName: String, phone: String, address: String, position: String, department: String,
               isAgent: Boolean, isSupervisor: Boolean, active: Boolean) {
        val employee = _state.value.employee ?: return
        val originalSupervisor = employee.isSupervisor ?: false
        val isRemovingSupervisor = originalSupervisor && !isSupervisor

        if (isRemovingSupervisor) {
            viewModelScope.launch {
                _state.update { it.copy(submitting = true, error = null) }
                val managed = fetchManagedFunds(employeeId)
                _state.update { it.copy(submitting = false) }
                if (managed.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            pendingReassign = PendingReassign(
                                managedFunds = managed,
                                input = UpdateInput(name, lastName, phone, address, position, department, isAgent, isSupervisor, active)
                            )
                        )
                    }
                    return@launch
                }
                runUpdate(name, lastName, phone, address, position, department, isAgent, isSupervisor, active)
            }
        } else {
            viewModelScope.launch {
                runUpdate(name, lastName, phone, address, position, department, isAgent, isSupervisor, active)
            }
        }
    }

    fun confirmReassign() {
        val pending = _state.value.pendingReassign ?: return
        _state.update { it.copy(pendingReassign = null) }
        val input = pending.input
        viewModelScope.launch {
            runUpdate(
                input.firstName, input.lastName, input.phone, input.address,
                input.position, input.department, input.isAgent, input.isSupervisor, input.active
            )
        }
    }

    fun cancelReassign() = _state.update { it.copy(pendingReassign = null) }

    /**
     * Defensive read — BE jos nema "fondovi po manageru" filter; koristimo full
     * list i filtriramo po `managerId`. Ako BE bude vracao 403/501 (jer je flow
     * jos u razvoju), tihi fallback na prazan list znaci da supervizor nema
     * fondove i admin moze direktno da update-uje.
     */
    private suspend fun fetchManagedFunds(employeeId: Long): List<FundSummaryDto> {
        return when (val result = fundRepository.list()) {
            is ApiResult.Success -> result.data.filter { it.managerId == employeeId }
            else -> emptyList()
        }
    }

    private suspend fun runUpdate(
        name: String, lastName: String, phone: String, address: String,
        position: String, department: String, isAgent: Boolean, isSupervisor: Boolean, active: Boolean
    ) {
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
        when (val result = repository.update(employeeId, request)) {
            is ApiResult.Success -> _state.update { it.copy(employee = result.data) }
            is ApiResult.Failure -> {
                _state.update { it.copy(submitting = false, error = result.error.message) }
                return
            }
            ApiResult.Loading -> Unit
        }
        if (isAgent != originalAgent || isSupervisor != originalSupervisor) {
            when (val permResult = repository.updatePermissions(employeeId, isAgent, isSupervisor)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, employee = permResult.data) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(submitting = false, error = permResult.error.message) }
                    return
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
    val pendingReassign: PendingReassign? = null,
    val error: String? = null
)

data class PendingReassign(
    val managedFunds: List<FundSummaryDto>,
    val input: UpdateInput
)

data class UpdateInput(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val address: String,
    val position: String,
    val department: String,
    val isAgent: Boolean,
    val isSupervisor: Boolean,
    val active: Boolean
)

sealed interface EmployeeEditEvent {
    data class Toast(val message: String) : EmployeeEditEvent
}
