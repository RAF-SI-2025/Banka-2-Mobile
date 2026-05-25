package rs.raf.banka2.mobile.feature.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto
import rs.raf.banka2.mobile.data.repository.AuditRepository
import javax.inject.Inject

/**
 * B7 / Spec C3 §69 — Audit log portal (supervisor/admin only).
 *
 * State sadrzi 4 filtera (tip akcije / actor email / datum od / datum do) +
 * paginirani odgovor. BE vraca 403 ako korisnik nije supervisor/admin —
 * UI prikazuje "Nemate pristup" alert.
 */
@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val repository: AuditRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuditLogState())
    val state: StateFlow<AuditLogState> = _state.asStateFlow()

    init { refresh() }

    fun setActionType(value: String?) {
        _state.update { it.copy(actionType = value, page = 0) }
        refresh()
    }

    fun setActorEmail(value: String) = _state.update { it.copy(actorEmail = value) }
    fun setDateFrom(value: String) = _state.update { it.copy(dateFrom = value) }
    fun setDateTo(value: String) = _state.update { it.copy(dateTo = value) }

    fun applyFilters() {
        _state.update { it.copy(page = 0) }
        refresh()
    }

    fun resetFilters() {
        _state.update {
            it.copy(
                actionType = null,
                actorEmail = "",
                dateFrom = "",
                dateTo = "",
                page = 0
            )
        }
        refresh()
    }

    fun nextPage() {
        val s = _state.value
        if (s.page + 1 < s.totalPages) {
            _state.update { it.copy(page = s.page + 1) }
            refresh()
        }
    }

    fun previousPage() {
        val s = _state.value
        if (s.page > 0) {
            _state.update { it.copy(page = s.page - 1) }
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = _state.value
        when (val result = repository.query(
            actionType = s.actionType,
            actorEmail = s.actorEmail,
            dateFrom = s.dateFrom,
            dateTo = s.dateTo,
            page = s.page,
            size = PAGE_SIZE
        )) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    logs = result.data.content,
                    totalPages = result.data.totalPages,
                    totalElements = result.data.totalElements,
                    page = result.data.number
                )
            }
            is ApiResult.Failure -> _state.update {
                val forbidden = result.error.httpCode == 403
                it.copy(
                    loading = false,
                    error = if (forbidden) "Nemate dozvolu za audit log." else result.error.message,
                    forbidden = forbidden,
                    logs = emptyList()
                )
            }
            ApiResult.Loading -> Unit
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}

data class AuditLogState(
    val loading: Boolean = false,
    val logs: List<AuditLogDto> = emptyList(),
    val actionType: String? = null,
    val actorEmail: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0L,
    val error: String? = null,
    val forbidden: Boolean = false
)
