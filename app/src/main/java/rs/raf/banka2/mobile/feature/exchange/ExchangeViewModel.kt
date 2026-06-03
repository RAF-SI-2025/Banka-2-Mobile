package rs.raf.banka2.mobile.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeHistoryPointDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val repository: ExchangeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeState())
    val state: StateFlow<ExchangeState> = _state.asStateFlow()

    // R1-582: drzimo referencu na poslednji recalc-job kako bismo otkazali
    // zastareli poziv pre nego sto izdamo novi (latest-wins).
    private var recalcJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { loadRates() }
    }

    fun setAmount(value: String) {
        _state.update { it.copy(amount = value, error = null) }
        recalc()
    }

    fun setFrom(value: String) {
        _state.update { it.copy(fromCurrency = value.uppercase()) }
        recalc()
    }

    fun setTo(value: String) {
        _state.update { it.copy(toCurrency = value.uppercase()) }
        recalc()
    }

    fun swap() {
        _state.update { it.copy(fromCurrency = it.toCurrency, toCurrency = it.fromCurrency) }
        recalc()
    }

    private fun recalc() {
        val current = _state.value
        val parsed = MoneyFormatter.parse(current.amount) ?: return
        if (current.fromCurrency.isBlank() || current.toCurrency.isBlank()) return
        // R1-369: ista valuta (npr. RSD→RSD) nije konverzija — prikazi 1:1
        // bez round-trip-a ka BE-u (koji bi vratio 400/besmislen kurs).
        if (current.fromCurrency.equals(current.toCurrency, ignoreCase = true)) {
            recalcJob?.cancel()
            _state.update { it.copy(calculation = null, error = null) }
            return
        }
        // R1-582: otkazi prethodni recalc da out-of-order odgovor ne pregazi
        // najnoviji kurs/iznos (korisnik brzo kuca → vise letecih poziva).
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            when (val result = repository.calculate(parsed, current.fromCurrency, current.toCurrency)) {
                is ApiResult.Success -> _state.update { it.copy(calculation = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadRates() {
        _state.update { it.copy(loading = true) }
        when (val result = repository.rates()) {
            is ApiResult.Success -> _state.update {
                it.copy(loading = false, rates = result.data)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * Mobile-bonus #5: tap "Istorija" na kursu otvara sparkline panel za tu
     * valutu. Repository graceful-fallback (404/501) → prazna lista pa chart
     * jednostavno nije renderovan.
     */
    fun toggleHistory(currency: String) {
        val current = _state.value
        if (current.expandedCurrency == currency) {
            _state.update { it.copy(expandedCurrency = null) }
            return
        }
        val cached = current.historyByCurrency[currency]
        if (cached != null) {
            _state.update { it.copy(expandedCurrency = currency) }
            return
        }
        _state.update { it.copy(expandedCurrency = currency, historyLoading = true) }
        viewModelScope.launch {
            when (val result = repository.history(currency, days = 30)) {
                is ApiResult.Success -> _state.update {
                    val updated = it.historyByCurrency.toMutableMap().apply {
                        put(currency, result.data)
                    }
                    it.copy(historyByCurrency = updated, historyLoading = false)
                }
                is ApiResult.Failure -> _state.update {
                    val updated = it.historyByCurrency.toMutableMap().apply {
                        put(currency, emptyList())
                    }
                    it.copy(historyByCurrency = updated, historyLoading = false)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class ExchangeState(
    val rates: List<ExchangeRateDto> = emptyList(),
    val loading: Boolean = false,
    val amount: String = "100",
    val fromCurrency: String = "RSD",
    val toCurrency: String = "EUR",
    val calculation: CalculateExchangeResponseDto? = null,
    val error: String? = null,
    /** Mobile-bonus #5: 1-mesec istorija po valuti (cache + expand state). */
    val historyByCurrency: Map<String, List<ExchangeHistoryPointDto>> = emptyMap(),
    val expandedCurrency: String? = null,
    val historyLoading: Boolean = false
)
