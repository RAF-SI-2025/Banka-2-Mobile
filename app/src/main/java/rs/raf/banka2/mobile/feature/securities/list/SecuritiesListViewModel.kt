package rs.raf.banka2.mobile.feature.securities.list

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
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.repository.ListingRepository
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 350L

@HiltViewModel
class SecuritiesListViewModel @Inject constructor(
    private val repository: ListingRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        SecuritiesListState(
            // Klijenti ne smeju FOREX → backend ce vratiti 403, sakrijemo tab unapred
            availableTypes = computeAvailableTypes(sessionManager.state.value)
        )
    )
    val state: StateFlow<SecuritiesListState> = _state.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                _state.update { it.copy(availableTypes = computeAvailableTypes(session)) }
            }
        }
        load()
    }

    fun setType(type: ListingTypeFilter) {
        _state.update { it.copy(type = type) }
        load()
    }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load()
        }
    }

    fun setExchangePrefix(value: String) {
        _state.update { it.copy(exchangePrefix = value) }
        load()
    }

    fun setPriceRange(min: String, max: String) {
        _state.update { it.copy(priceMin = min, priceMax = max) }
        load()
    }

    fun refresh() = viewModelScope.launch {
        repository.refresh()
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val current = _state.value
            val result = repository.list(
                type = current.type.apiValue,
                search = current.search.takeIf { it.isNotBlank() },
                exchangePrefix = current.exchangePrefix.takeIf { it.isNotBlank() },
                priceMin = MoneyFormatter.parse(current.priceMin),
                priceMax = MoneyFormatter.parse(current.priceMax)
            )
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, listings = result.data) }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun computeAvailableTypes(session: SessionState): List<ListingTypeFilter> {
        val role = (session as? SessionState.LoggedIn)?.profile?.role
        val all = listOf(ListingTypeFilter.Stock, ListingTypeFilter.Futures)
        // FOREX vide samo zaposleni — klijenti dobiju 403 ako pokusaju
        return if (role?.isEmployee == true) all + ListingTypeFilter.Forex else all
    }
}

enum class ListingTypeFilter(val apiValue: String, val label: String) {
    Stock("STOCK", "Akcije"),
    Futures("FUTURES", "Futures"),
    Forex("FOREX", "Forex")
}

data class SecuritiesListState(
    val type: ListingTypeFilter = ListingTypeFilter.Stock,
    val search: String = "",
    val exchangePrefix: String = "",
    val priceMin: String = "",
    val priceMax: String = "",
    val availableTypes: List<ListingTypeFilter> = emptyList(),
    val listings: List<ListingDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
) {
    val anyTestMode: Boolean get() = listings.any { it.isTestMode == true }
}
