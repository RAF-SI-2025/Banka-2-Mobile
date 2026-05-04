package rs.raf.banka2.mobile.feature.securities.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.listing.ListingDailyPriceDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.option.OptionChainDto
import rs.raf.banka2.mobile.data.repository.ListingRepository
import rs.raf.banka2.mobile.data.repository.OptionRepository
import javax.inject.Inject

@HiltViewModel
class SecuritiesDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listingRepository: ListingRepository,
    private val optionRepository: OptionRepository
) : ViewModel() {

    private val listingId: Long = savedStateHandle["listingId"] ?: 0L

    private val _state = MutableStateFlow(SecuritiesDetailsState())
    val state: StateFlow<SecuritiesDetailsState> = _state.asStateFlow()

    init {
        load()
    }

    fun setPeriod(period: ChartPeriod) {
        _state.update { it.copy(period = period) }
        viewModelScope.launch { fetchHistory(period) }
    }

    fun setStrikeRowFilter(rows: Int) {
        // Spec Celina 3 §467: korisnik bira broj strike redova iznad i ispod Shared Price.
        // Min 1 da uvek bar 1 red bude vidljiv, max 20 da UI ne ode u beskraj.
        val clamped = rows.coerceIn(STRIKE_FILTER_MIN, STRIKE_FILTER_MAX)
        _state.update { it.copy(strikeRowsAroundPrice = clamped) }
    }

    fun load() {
        viewModelScope.launch { fetchListing() }
        viewModelScope.launch { fetchHistory(_state.value.period) }
    }

    fun exerciseOption(optionId: Long) = viewModelScope.launch {
        _state.update { it.copy(exercising = true, exerciseSuccess = null) }
        when (val result = optionRepository.exercise(optionId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(exercising = false, exerciseSuccess = "Opcija je iskoriscena.") }
                fetchOptions()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(exercising = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun clearExerciseSuccess() = _state.update { it.copy(exerciseSuccess = null) }

    private suspend fun fetchListing() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = listingRepository.byId(listingId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(loading = false, listing = result.data) }
                if (result.data.listingType.equals("STOCK", true)) {
                    fetchOptions()
                }
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchHistory(period: ChartPeriod) {
        when (val result = listingRepository.history(listingId, period.apiValue)) {
            is ApiResult.Success -> _state.update { it.copy(history = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchOptions() {
        when (val result = optionRepository.chainFor(listingId)) {
            is ApiResult.Success -> _state.update { it.copy(optionChains = result.data) }
            else -> Unit
        }
    }
}

/**
 * Spec Celina 3 §467: filtrira strike redove tako da se prikaze `rowsAroundPrice`
 * iznad i ispod `currentPrice` (Shared Price). Ako je ukupan broj <= 2*rows,
 * vraca sve. Ulaz mora biti sortiran po strike-u rastuce.
 *
 * Pure funkcija — odvojena od composable-a radi unit-test pokrivanja.
 */
fun <T> pickVisibleStrikeEntries(
    sortedByStrike: List<T>,
    rowsAroundPrice: Int,
    strikeOf: (T) -> Double,
    currentPrice: Double
): List<T> {
    if (rowsAroundPrice <= 0) return emptyList()
    if (sortedByStrike.size <= rowsAroundPrice * 2) return sortedByStrike
    val pivot = sortedByStrike.indexOfFirst { strikeOf(it) >= currentPrice }
        .let { if (it < 0) sortedByStrike.lastIndex else it }
    val from = (pivot - rowsAroundPrice).coerceAtLeast(0)
    val to = (pivot + rowsAroundPrice).coerceAtMost(sortedByStrike.size - 1)
    return sortedByStrike.subList(from, to + 1)
}

enum class ChartPeriod(val apiValue: String, val label: String) {
    Day("DAY", "1D"),
    Week("WEEK", "1N"),
    Month("MONTH", "1M"),
    Year("YEAR", "1G"),
    FiveYears("FIVE_YEARS", "5G")
}

data class SecuritiesDetailsState(
    val listing: ListingDto? = null,
    val history: List<ListingDailyPriceDto> = emptyList(),
    val optionChains: List<OptionChainDto> = emptyList(),
    val period: ChartPeriod = ChartPeriod.Month,
    val strikeRowsAroundPrice: Int = DEFAULT_STRIKE_ROWS,
    val loading: Boolean = false,
    val exercising: Boolean = false,
    val exerciseSuccess: String? = null,
    val error: String? = null
)

const val DEFAULT_STRIKE_ROWS: Int = 5
const val STRIKE_FILTER_MIN: Int = 1
const val STRIKE_FILTER_MAX: Int = 20
