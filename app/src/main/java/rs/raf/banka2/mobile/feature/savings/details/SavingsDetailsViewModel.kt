package rs.raf.banka2.mobile.feature.savings.details

import androidx.lifecycle.SavedStateHandle
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
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsTransactionDto
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import javax.inject.Inject

@HiltViewModel
class SavingsDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SavingsRepository
) : ViewModel() {

    private val depositId: Long = savedStateHandle["depositId"] ?: 0L

    private val _state = MutableStateFlow(SavingsDetailsState())
    val state: StateFlow<SavingsDetailsState> = _state.asStateFlow()

    private val _events = Channel<SavingsDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() {
        viewModelScope.launch { fetchDeposit() }
        viewModelScope.launch { fetchTransactions() }
    }

    fun toggleAutoRenew(value: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(autoRenewSaving = true) }
            when (val result = repository.toggleAutoRenew(depositId, value)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(autoRenewSaving = false, deposit = result.data) }
                    _events.send(
                        SavingsDetailsEvent.Toast(
                            if (value) "Auto-obnova ukljucena." else "Auto-obnova iskljucena."
                        )
                    )
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(autoRenewSaving = false, error = result.error.message) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun openWithdrawDialog() = _state.update { it.copy(showWithdraw = true) }

    fun dismissWithdrawDialog() = _state.update { it.copy(showWithdraw = false) }

    fun confirmWithdraw(otpCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(withdrawing = true) }
            when (val result = repository.withdrawEarly(depositId, otpCode)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(withdrawing = false, showWithdraw = false, deposit = result.data)
                    }
                    _events.send(SavingsDetailsEvent.Toast("Depozit raskinut. Sredstva su vracena na racun."))
                    fetchTransactions()
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(withdrawing = false, showWithdraw = false, error = result.error.message)
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun fetchDeposit() {
        _state.update { it.copy(loading = true) }
        when (val result = repository.getDeposit(depositId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, deposit = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchTransactions() {
        when (val result = repository.getTransactions(depositId)) {
            is ApiResult.Success -> _state.update { it.copy(transactions = result.data) }
            else -> Unit
        }
    }
}

data class SavingsDetailsState(
    val loading: Boolean = false,
    val deposit: SavingsDepositDto? = null,
    val transactions: List<SavingsTransactionDto> = emptyList(),
    val error: String? = null,
    val autoRenewSaving: Boolean = false,
    val showWithdraw: Boolean = false,
    val withdrawing: Boolean = false
)

sealed interface SavingsDetailsEvent {
    data class Toast(val message: String) : SavingsDetailsEvent
}
