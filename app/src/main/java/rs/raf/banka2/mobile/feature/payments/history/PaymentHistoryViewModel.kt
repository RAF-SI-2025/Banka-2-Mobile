package rs.raf.banka2.mobile.feature.payments.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import javax.inject.Inject

@HiltViewModel
class PaymentHistoryViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentHistoryState())
    val state: StateFlow<PaymentHistoryState> = _state.asStateFlow()

    init { refresh() }

    /** R1-585: filter po broju racuna (prazno = svi racuni). */
    fun setAccountFilter(accountNumber: String?) {
        _state.update { it.copy(accountNumber = accountNumber?.takeIf { v -> v.isNotBlank() }) }
        refresh()
    }

    /** R1-585: filter po statusu placanja (null = svi statusi). */
    fun setStatusFilter(status: String?) {
        _state.update { it.copy(status = status?.takeIf { v -> v.isNotBlank() }) }
        refresh()
    }

    fun refresh() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            // R1-585: prosledjujemo aktivne filtere (account/status) BE-u umesto
            // da uvek dohvatamo prvih 50 bez filtriranja.
            when (
                val result = paymentRepository.getMyPayments(
                    page = 0,
                    limit = 50,
                    accountNumber = current.accountNumber,
                    status = current.status
                )
            ) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, payments = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class PaymentHistoryState(
    val loading: Boolean = false,
    val payments: List<PaymentListItemDto> = emptyList(),
    val accountNumber: String? = null,
    val status: String? = null,
    val error: String? = null
)
