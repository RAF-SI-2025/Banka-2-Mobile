package rs.raf.banka2.mobile.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val repository: ExchangeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeState())
    val state: StateFlow<ExchangeState> = _state.asStateFlow()

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
        viewModelScope.launch {
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
}

data class ExchangeState(
    val rates: List<ExchangeRateDto> = emptyList(),
    val loading: Boolean = false,
    val amount: String = "100",
    val fromCurrency: String = "RSD",
    val toCurrency: String = "EUR",
    val calculation: CalculateExchangeResponseDto? = null,
    val error: String? = null
)
