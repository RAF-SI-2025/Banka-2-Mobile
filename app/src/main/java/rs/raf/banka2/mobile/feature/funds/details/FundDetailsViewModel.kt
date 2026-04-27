package rs.raf.banka2.mobile.feature.funds.details

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
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.fund.FundDetailDto
import rs.raf.banka2.mobile.data.dto.fund.FundPerformancePointDto
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import javax.inject.Inject

@HiltViewModel
class FundDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fundRepository: FundRepository,
    private val accountRepository: AccountRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val fundId: Long = savedStateHandle["fundId"] ?: 0L

    private val _state = MutableStateFlow(
        FundDetailsState(
            isSupervisor = (sessionManager.state.value as? SessionState.LoggedIn)?.profile?.role?.isSupervisor == true
        )
    )
    val state: StateFlow<FundDetailsState> = _state.asStateFlow()

    private val _events = Channel<FundDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                _state.update {
                    it.copy(isSupervisor = (session as? SessionState.LoggedIn)?.profile?.role?.isSupervisor == true)
                }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch { fetchDetails() }
        viewModelScope.launch { fetchPerformance() }
        viewModelScope.launch { fetchAccounts() }
        viewModelScope.launch { fetchPosition() }
    }

    fun invest(sourceAccountId: Long, amount: Double) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        when (val result = fundRepository.invest(fundId, sourceAccountId, amount)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false) }
                _events.send(FundDetailsEvent.Toast("Ulaganje uspesno."))
                load()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun withdraw(destinationAccountId: Long, amount: Double?, withdrawAll: Boolean) = viewModelScope.launch {
        _state.update { it.copy(submitting = true, error = null) }
        when (val result = fundRepository.withdraw(fundId, destinationAccountId, amount, withdrawAll)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false) }
                _events.send(FundDetailsEvent.Toast("Povlacenje pokrenuto."))
                load()
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchDetails() {
        _state.update { it.copy(loading = true) }
        when (val result = fundRepository.details(fundId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, fund = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchPerformance() {
        when (val result = fundRepository.performance(fundId)) {
            is ApiResult.Success -> _state.update { it.copy(performance = result.data) }
            else -> Unit
        }
    }

    private suspend fun fetchAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update { it.copy(accounts = result.data) }
            else -> Unit
        }
    }

    private suspend fun fetchPosition() {
        when (val result = fundRepository.myPositions()) {
            is ApiResult.Success -> _state.update {
                it.copy(myPosition = result.data.firstOrNull { p -> p.fundId == fundId })
            }
            else -> Unit
        }
    }
}

data class FundDetailsState(
    val loading: Boolean = false,
    val fund: FundDetailDto? = null,
    val performance: List<FundPerformancePointDto> = emptyList(),
    val myPosition: FundPositionDto? = null,
    val accounts: List<AccountDto> = emptyList(),
    val isSupervisor: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed interface FundDetailsEvent {
    data class Toast(val message: String) : FundDetailsEvent
}
