package rs.raf.banka2.mobile.feature.tax

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
import rs.raf.banka2.mobile.data.dto.tax.TaxBreakdownItemDto
import rs.raf.banka2.mobile.data.dto.tax.TaxRecordDto
import rs.raf.banka2.mobile.data.repository.TaxRepository
import javax.inject.Inject

@HiltViewModel
class TaxViewModel @Inject constructor(
    private val repository: TaxRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaxState())
    val state: StateFlow<TaxState> = _state.asStateFlow()

    private val _events = Channel<TaxEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAll()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, records = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun calculate() = viewModelScope.launch {
        when (val result = repository.calculate()) {
            is ApiResult.Success -> {
                _events.send(TaxEvent.Toast("Obracun pokrenut."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * Spec Celina 3 §516-518: ucitaj per-listing breakdown za izabranog korisnika.
     * UI prikazuje modal sa listom hartija koje su doprinele profitu/gubitku.
     */
    fun openBreakdown(record: TaxRecordDto) = viewModelScope.launch {
        val userId = record.userId
        val userType = record.userType
        if (userId == null || userType.isNullOrBlank()) {
            _state.update { it.copy(breakdownError = "Nema validnog userId/userType za breakdown.") }
            return@launch
        }
        _state.update {
            it.copy(
                breakdownTarget = record,
                breakdownLoading = true,
                breakdownError = null,
                breakdownItems = emptyList()
            )
        }
        when (val result = repository.getBreakdown(userId, userType)) {
            is ApiResult.Success -> _state.update {
                it.copy(breakdownLoading = false, breakdownItems = result.data)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(breakdownLoading = false, breakdownError = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun closeBreakdown() = _state.update {
        it.copy(
            breakdownTarget = null,
            breakdownItems = emptyList(),
            breakdownError = null,
            breakdownLoading = false
        )
    }
}

data class TaxState(
    val loading: Boolean = false,
    val records: List<TaxRecordDto> = emptyList(),
    val error: String? = null,
    val breakdownTarget: TaxRecordDto? = null,
    val breakdownLoading: Boolean = false,
    val breakdownItems: List<TaxBreakdownItemDto> = emptyList(),
    val breakdownError: String? = null
)

sealed interface TaxEvent {
    data class Toast(val message: String) : TaxEvent
}
