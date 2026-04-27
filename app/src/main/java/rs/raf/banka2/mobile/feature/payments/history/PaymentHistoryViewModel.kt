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

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = paymentRepository.getMyPayments(page = 0, limit = 50)) {
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
    val error: String? = null
)
