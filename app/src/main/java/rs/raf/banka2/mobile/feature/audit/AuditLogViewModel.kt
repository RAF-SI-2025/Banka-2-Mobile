package rs.raf.banka2.mobile.feature.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto
import rs.raf.banka2.mobile.data.repository.AuditRepository
import javax.inject.Inject

/**
 * B7 / Spec C3 §69 — Audit log portal (supervisor/admin only).
 *
 * State sadrzi 4 filtera (tip akcije / ID aktera / datum od / datum do) +
 * paginirani odgovor. BE vraca 403 ako korisnik nije supervisor/admin —
 * UI prikazuje "Nemate pristup" alert.
 *
 * R1-599: BE `/audit` list endpoint filtrira aktera SAMO po numerickom `actorId`
 * (Long) — NEMA email filter. Ranije je VM nosio `actorEmail` koji se tiho gubio
 * (`@Suppress UNUSED` u repo-u). Sada forma trazi ID aktera (kao FE), pa se filter
 * stvarno primenjuje. `actionType` filter pokriva svih 30 BE tipova (R1-594).
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

    /** R1-599: filter aktera je numericki ID (BE nema email filter). */
    fun setActorIdText(value: String) =
        _state.update { it.copy(actorIdText = value.filter { ch -> ch.isDigit() }) }
    fun setDateFrom(value: String) = _state.update { it.copy(dateFrom = value) }
    fun setDateTo(value: String) = _state.update { it.copy(dateTo = value) }

    fun applyFilters() {
        val s = _state.value
        // R1-597: validiraj datume pre BE poziva (slobodan unos → BE parse 400/500).
        if (!DateFormatter.isValidIsoDate(s.dateFrom) || !DateFormatter.isValidIsoDate(s.dateTo)) {
            _state.update { it.copy(error = "Datum mora biti u formatu YYYY-MM-DD.") }
            return
        }
        _state.update { it.copy(page = 0, error = null) }
        refresh()
    }

    fun resetFilters() {
        _state.update {
            it.copy(
                actionType = null,
                actorIdText = "",
                dateFrom = "",
                dateTo = "",
                page = 0,
                error = null
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
            actorId = s.actorIdText.toLongOrNull(),
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
    /** R1-599: numericki ID aktera (BE nema email filter). */
    val actorIdText: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0L,
    val error: String? = null,
    val forbidden: Boolean = false
)
