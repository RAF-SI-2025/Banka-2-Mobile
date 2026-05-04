package rs.raf.banka2.mobile.feature.portfolio

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
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioItemDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto
import rs.raf.banka2.mobile.data.dto.tax.TaxBreakdownItemDto
import rs.raf.banka2.mobile.data.repository.OptionRepository
import rs.raf.banka2.mobile.data.repository.PortfolioRepository
import rs.raf.banka2.mobile.data.repository.TaxRepository
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val optionRepository: OptionRepository,
    private val taxRepository: TaxRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = _state.asStateFlow()

    private val _events = Channel<PortfolioEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { loadPositions() }
        viewModelScope.launch { loadSummary() }
        viewModelScope.launch { loadTaxBreakdown() }
    }

    fun toggleTaxBreakdown() = _state.update { it.copy(taxBreakdownExpanded = !it.taxBreakdownExpanded) }

    fun setPublicQuantity(item: PortfolioItemDto, value: Int) = viewModelScope.launch {
        when (val result = repository.setPublicQuantity(item.id, value)) {
            is ApiResult.Success -> {
                _events.send(PortfolioEvent.Toast("Javna kolicina je azurirana."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun exerciseOption(optionId: Long) = viewModelScope.launch {
        when (val result = optionRepository.exercise(optionId)) {
            is ApiResult.Success -> {
                _events.send(PortfolioEvent.Toast("Opcija je iskoriscena."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadPositions() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.myPortfolio()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, positions = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadSummary() {
        when (val result = repository.summary()) {
            is ApiResult.Success -> _state.update { it.copy(summary = result.data) }
            else -> Unit
        }
    }

    /**
     * Spec Celina 3 §516-518 (P2.4): per-listing porez breakdown za trenutnog
     * korisnika. PortfolioScreen u sekciji "Procenjeni porez" omogucava expand
     * koji prikazuje koje hartije su doprinele profitu i koliko poreza nosi
     * svaka. Greske ne prikazujemo kao banner — samo cuvamo poruku za debug.
     */
    private suspend fun loadTaxBreakdown() {
        when (val result = taxRepository.getMyBreakdown()) {
            is ApiResult.Success -> _state.update {
                it.copy(taxBreakdown = result.data, taxBreakdownError = null)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(taxBreakdown = emptyList(), taxBreakdownError = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class PortfolioState(
    val loading: Boolean = false,
    val positions: List<PortfolioItemDto> = emptyList(),
    val summary: PortfolioSummaryDto? = null,
    val error: String? = null,
    val taxBreakdown: List<TaxBreakdownItemDto> = emptyList(),
    val taxBreakdownExpanded: Boolean = false,
    val taxBreakdownError: String? = null
)

sealed interface PortfolioEvent {
    data class Toast(val message: String) : PortfolioEvent
}
