package rs.raf.banka2.mobile.feature.otc.history

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
import rs.raf.banka2.mobile.data.dto.otchistory.OtcNegotiationHistoryDto
import rs.raf.banka2.mobile.data.repository.OtcRepository
import javax.inject.Inject

/**
 * B10 / Spec C4 §13 — istorija OTC pregovora (supervisor/admin only).
 *
 * Glavna lista je paginirana lista poslednjih zapisa.
 * Tap na red expanduje pun lanac kontraponuda za taj negotiationId.
 */
@HiltViewModel
class OtcNegotiationHistoryViewModel @Inject constructor(
    private val repository: OtcRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OtcNegotiationHistoryState())
    val state: StateFlow<OtcNegotiationHistoryState> = _state.asStateFlow()

    init { refresh() }

    fun setStatus(value: String?) {
        _state.update { it.copy(status = value, page = 0) }
        refresh()
    }

    fun setDateFrom(value: String) = _state.update { it.copy(dateFrom = value) }
    fun setDateTo(value: String) = _state.update { it.copy(dateTo = value) }

    fun applyFilters() {
        val s = _state.value
        // R1-597: validiraj datume pre BE poziva (slobodan unos → BE
        // `LocalDateTime.parse` → 400/500).
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
                status = null,
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

    fun toggleChain(negotiationId: Long) {
        val current = _state.value.expandedNegotiation
        if (current == negotiationId) {
            _state.update {
                it.copy(
                    expandedNegotiation = null,
                    chain = emptyList(),
                    chainLoading = false,
                    chainError = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    expandedNegotiation = negotiationId,
                    chain = emptyList(),
                    chainLoading = true,
                    chainError = null
                )
            }
            loadChain(negotiationId)
        }
    }

    private fun loadChain(negotiationId: Long) = viewModelScope.launch {
        when (val result = repository.negotiationHistoryChain(negotiationId)) {
            is ApiResult.Success -> _state.update {
                if (it.expandedNegotiation == negotiationId) {
                    it.copy(chain = result.data, chainLoading = false)
                } else it
            }
            is ApiResult.Failure -> _state.update {
                if (it.expandedNegotiation == negotiationId) {
                    it.copy(chainLoading = false, chainError = result.error.message)
                } else it
            }
            ApiResult.Loading -> Unit
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = _state.value
        when (val result = repository.negotiationHistory(
            status = s.status,
            from = s.dateFrom,
            to = s.dateTo,
            page = s.page,
            size = PAGE_SIZE
        )) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    entries = result.data.content,
                    totalPages = result.data.totalPages,
                    totalElements = result.data.totalElements,
                    page = result.data.number
                )
            }
            is ApiResult.Failure -> _state.update {
                val forbidden = result.error.httpCode == 403
                val notFound = result.error.httpCode == 404
                it.copy(
                    loading = false,
                    error = when {
                        forbidden -> "Nemate dozvolu za istoriju pregovora."
                        notFound -> "Servis za istoriju pregovora jos nije dostupan."
                        else -> result.error.message
                    },
                    forbidden = forbidden,
                    entries = emptyList()
                )
            }
            ApiResult.Loading -> Unit
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}

data class OtcNegotiationHistoryState(
    val loading: Boolean = false,
    val entries: List<OtcNegotiationHistoryDto> = emptyList(),
    val status: String? = null,
    val dateFrom: String = "",
    val dateTo: String = "",
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0L,
    val expandedNegotiation: Long? = null,
    val chain: List<OtcNegotiationHistoryDto> = emptyList(),
    val chainLoading: Boolean = false,
    val chainError: String? = null,
    val error: String? = null,
    val forbidden: Boolean = false
)
