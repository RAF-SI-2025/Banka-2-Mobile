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
    val loading: Boolean = false,
    val exercising: Boolean = false,
    val exerciseSuccess: String? = null,
    val error: String? = null
)
