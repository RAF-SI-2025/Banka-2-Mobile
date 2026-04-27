package rs.raf.banka2.mobile.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class AccountsListViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsListState())
    val state: StateFlow<AccountsListState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.getMyAccounts()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, accounts = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class AccountsListState(
    val loading: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val error: String? = null
)
