package rs.raf.banka2.mobile.feature.pricealerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertLabels
import rs.raf.banka2.mobile.data.repository.PriceAlertRepository
import javax.inject.Inject

/**
 * [FE2 Mobile port — Price Alert] VM za PriceAlertsScreen.
 *
 * 3-state filter (Aktivni/Istorija/Sve) je FE-strana. BE `listMy` ima
 * `active` query parametar, ali za UI je jednostavnije fetch-ovati sve i
 * filtrirati lokalno (jedna lista) — paritet sa FE web PriceAlertsPage.
 */
@HiltViewModel
class PriceAlertsViewModel @Inject constructor(
    private val repository: PriceAlertRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PriceAlertsState())
    val state: StateFlow<PriceAlertsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.listMy(active = null)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, alerts = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun setFilter(tab: PriceAlertLabels.FilterTab) {
        if (_state.value.filter == tab) return
        _state.update { it.copy(filter = tab) }
    }

    fun openDeleteConfirm(alert: PriceAlertDto) =
        _state.update { it.copy(deleteTarget = alert) }

    fun dismissDeleteConfirm() = _state.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(deletingId = target.id, error = null) }
            when (val result = repository.delete(target.id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        deletingId = null,
                        deleteTarget = null,
                        alerts = it.alerts.filterNot { a -> a.id == target.id },
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(deletingId = null, deleteTarget = null, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

data class PriceAlertsState(
    val alerts: List<PriceAlertDto> = emptyList(),
    val filter: PriceAlertLabels.FilterTab = PriceAlertLabels.FilterTab.ACTIVE,
    val loading: Boolean = false,
    val error: String? = null,
    val deletingId: Long? = null,
    val deleteTarget: PriceAlertDto? = null,
) {
    val filteredAlerts: List<PriceAlertDto>
        get() = when (filter) {
            PriceAlertLabels.FilterTab.ACTIVE -> alerts.filter { it.active }
            PriceAlertLabels.FilterTab.HISTORY -> alerts.filterNot { it.active }
            PriceAlertLabels.FilterTab.ALL -> alerts
        }

    val activeCount: Int get() = alerts.count { it.active }
    val historyCount: Int get() = alerts.count { !it.active }
    val totalCount: Int get() = alerts.size
}
