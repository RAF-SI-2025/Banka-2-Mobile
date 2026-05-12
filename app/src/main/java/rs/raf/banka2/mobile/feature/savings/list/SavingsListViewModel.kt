package rs.raf.banka2.mobile.feature.savings.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SavingsListViewModel @Inject constructor(
    private val repository: SavingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavingsListState())
    val state: StateFlow<SavingsListState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.listMy()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, deposits = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class SavingsListState(
    val loading: Boolean = false,
    val deposits: List<SavingsDepositDto> = emptyList(),
    val error: String? = null
) {
    val totalPrincipalRsd: BigDecimal
        get() = deposits.filter { it.currencyCode == "RSD" }
            .fold(BigDecimal.ZERO) { acc, d -> acc + d.principalAmount }

    val totalInterestPaid: BigDecimal
        get() = deposits.fold(BigDecimal.ZERO) { acc, d -> acc + d.totalInterestPaid }

    val activeCount: Int
        get() = deposits.count { it.status == "ACTIVE" }
}
