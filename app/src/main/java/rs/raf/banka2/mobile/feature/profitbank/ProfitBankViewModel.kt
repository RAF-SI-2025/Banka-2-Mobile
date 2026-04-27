package rs.raf.banka2.mobile.feature.profitbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.profitbank.ActuaryProfitDto
import rs.raf.banka2.mobile.data.dto.profitbank.BankFundPositionDto
import rs.raf.banka2.mobile.data.repository.ProfitBankRepository
import javax.inject.Inject

@HiltViewModel
class ProfitBankViewModel @Inject constructor(
    private val repository: ProfitBankRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfitBankState())
    val state: StateFlow<ProfitBankState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { loadActuaries() }
        viewModelScope.launch { loadFundPositions() }
    }

    private suspend fun loadActuaries() {
        when (val result = repository.actuaryProfits()) {
            is ApiResult.Success -> _state.update { it.copy(actuaries = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadFundPositions() {
        when (val result = repository.bankFundPositions()) {
            is ApiResult.Success -> _state.update { it.copy(fundPositions = result.data) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class ProfitBankState(
    val actuaries: List<ActuaryProfitDto> = emptyList(),
    val fundPositions: List<BankFundPositionDto> = emptyList(),
    val error: String? = null
)
