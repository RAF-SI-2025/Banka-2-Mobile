package rs.raf.banka2.mobile.feature.employees.list

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
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import javax.inject.Inject

@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val repository: EmployeeAdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeListState())
    val state: StateFlow<EmployeeListState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init { refresh() }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350L)
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val current = _state.value.search.takeIf { it.isNotBlank() }
        when (val result = repository.list(
            email = current,
            firstName = current,
            lastName = current
        )) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, employees = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class EmployeeListState(
    val loading: Boolean = false,
    val employees: List<EmployeeDto> = emptyList(),
    val search: String = "",
    val error: String? = null
)
