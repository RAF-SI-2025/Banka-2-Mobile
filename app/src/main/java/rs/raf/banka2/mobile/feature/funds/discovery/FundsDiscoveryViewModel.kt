package rs.raf.banka2.mobile.feature.funds.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.dto.fundstatistics.FundStatisticsDto
import rs.raf.banka2.mobile.data.repository.FundRepository
import javax.inject.Inject

@HiltViewModel
class FundsDiscoveryViewModel @Inject constructor(
    private val repository: FundRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        FundsDiscoveryState(
            canCreateFund = (sessionManager.state.value as? SessionState.LoggedIn)?.profile?.role?.isSupervisor == true
        )
    )
    val state: StateFlow<FundsDiscoveryState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                _state.update {
                    it.copy(canCreateFund = (session as? SessionState.LoggedIn)?.profile?.role?.isSupervisor == true)
                }
            }
        }
        refresh()
    }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            refresh()
        }
    }

    fun setSort(field: FundSortField) {
        val current = _state.value
        val newDirection = if (current.sortField == field) {
            if (current.sortAscending) "DESC" else "ASC"
        } else "ASC"
        _state.update { it.copy(sortField = field, sortAscending = newDirection == "ASC") }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { loadFunds() }
        viewModelScope.launch { loadMyPositions() }
    }

    /**
     * B12 / Spec C4 §15: za svaki fond u listi povuci statistiku (annualReturn,
     * sharpe, maxDrawdown, volatility). Pokrece se nakon `loadFunds`. Tihi
     * fallback — fond koji nema dovoljno istorije ne pojavljuje se u mapi.
     *
     * R1-596: zahtevi se salju PARALELNO (`async`/`awaitAll`) umesto sekvencijalno
     * jedan-po-jedan. Ranije je N fondova = N serijskih round-trip-ova (npr. 20
     * fondova × ~150ms = ~3s blokade pre nego se statistika pojavi); sada je ~max
     * jednog poziva. Tihi fallback po fondu ostaje (greska ne rusi ostale).
     */
    private suspend fun loadStatistics(funds: List<FundSummaryDto>) {
        if (funds.isEmpty()) {
            _state.update { it.copy(statisticsByFundId = emptyMap()) }
            return
        }
        val results = coroutineScope {
            funds.map { fund ->
                async {
                    when (val r = repository.statistics(fund.id)) {
                        is ApiResult.Success -> fund.id to r.data
                        else -> null // tihi fallback — UI prikazuje "—"
                    }
                }
            }.awaitAll()
        }.filterNotNull().toMap()
        _state.update { it.copy(statisticsByFundId = results) }
    }

    private suspend fun loadFunds() {
        _state.update { it.copy(loading = true, error = null) }
        val current = _state.value
        when (val result = repository.list(
            search = current.search.takeIf { it.isNotBlank() },
            sort = current.sortField.api,
            direction = if (current.sortAscending) "ASC" else "DESC"
        )) {
            is ApiResult.Success -> {
                _state.update { it.copy(loading = false, funds = result.data) }
                // B12: fetch statistics paralelno (tihi fallback ako nema dovoljno istorije).
                loadStatistics(result.data)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadMyPositions() {
        when (val result = repository.myPositions()) {
            is ApiResult.Success -> _state.update { it.copy(myPositions = result.data) }
            else -> Unit
        }
    }
}

enum class FundSortField(val api: String, val label: String) {
    Name("name", "Naziv"),
    Value("totalValue", "Vrednost"),
    Profit("profit", "Profit"),
    Minimum("minimumContribution", "Min ulog")
}

data class FundsDiscoveryState(
    val funds: List<FundSummaryDto> = emptyList(),
    val myPositions: List<FundPositionDto> = emptyList(),
    val search: String = "",
    val sortField: FundSortField = FundSortField.Value,
    val sortAscending: Boolean = false,
    val loading: Boolean = false,
    val canCreateFund: Boolean = false,
    val error: String? = null,
    /** B12: per-fund statistike (annualReturn, sharpe, maxDD, volatility). */
    val statisticsByFundId: Map<Long, FundStatisticsDto> = emptyMap()
)
