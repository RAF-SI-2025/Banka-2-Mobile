package rs.raf.banka2.mobile.feature.accounts

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
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import javax.inject.Inject

@HiltViewModel
class AccountDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val accountId: Long = savedStateHandle["accountId"] ?: 0L

    private val _state = MutableStateFlow(AccountDetailsState())
    val state: StateFlow<AccountDetailsState> = _state.asStateFlow()

    private val _events = Channel<AccountDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { fetchAccount() }
        viewModelScope.launch { fetchTransactions() }
    }

    fun renameAccount(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isRenaming = true) }
            when (val result = accountRepository.renameAccount(accountId, newName.trim())) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isRenaming = false, account = result.data, generalError = null) }
                    _events.send(AccountDetailsEvent.Toast("Naziv racuna je promenjen."))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isRenaming = false, generalError = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun submitLimitChange(daily: Double?, monthly: Double?, otpCode: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSavingLimit = true, limitError = null) }
            when (val result = accountRepository.updateLimits(accountId, daily, monthly, otpCode)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSavingLimit = false, account = result.data) }
                    _events.send(AccountDetailsEvent.LimitSaved)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isSavingLimit = false, limitError = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun fetchAccount() {
        _state.update { it.copy(loading = true, generalError = null) }
        when (val result = accountRepository.getAccountById(accountId)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, account = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, generalError = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun fetchTransactions() {
        when (val result = paymentRepository.getMyPayments(page = 0, limit = 30)) {
            is ApiResult.Success -> {
                val accountNumber = _state.value.account?.accountNumber
                val filtered = if (accountNumber == null) result.data
                else result.data.filter {
                    it.fromAccount?.contains(accountNumber) == true ||
                        it.toAccount?.contains(accountNumber) == true
                }
                _state.update { it.copy(transactions = filtered) }
            }
            else -> Unit
        }
    }
}

data class AccountDetailsState(
    val loading: Boolean = false,
    val account: AccountDto? = null,
    val transactions: List<PaymentListItemDto> = emptyList(),
    val generalError: String? = null,
    val isRenaming: Boolean = false,
    val isSavingLimit: Boolean = false,
    val limitError: String? = null
)

sealed interface AccountDetailsEvent {
    data class Toast(val message: String) : AccountDetailsEvent
    data object LimitSaved : AccountDetailsEvent
}
