package rs.raf.banka2.mobile.feature.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistDto
import rs.raf.banka2.mobile.data.repository.WatchlistRepository
import javax.inject.Inject

/**
 * [FE2 Mobile port — AddToWatchlistDialog] VM koji UCITAVA listu korisnikovih
 * watchlist-a i salje POST/items zahtev za izabranu listu.
 *
 * Dialog handluje inline kreiranje nove liste — `pendingNewListName` cuva input
 * dok ne klikne "Kreiraj i dodaj".
 *
 * 409 (Conflict) iz BE-a -> friendly "vec u listi" poruka (BE ima unique
 * constraint na watchlist_id + listing_id).
 */
@HiltViewModel
class AddToWatchlistViewModel @Inject constructor(
    private val repository: WatchlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddToWatchlistState())
    val state: StateFlow<AddToWatchlistState> = _state.asStateFlow()

    fun reset(listingId: Long, listingTicker: String) {
        _state.update {
            AddToWatchlistState(
                listingId = listingId,
                listingTicker = listingTicker,
            )
        }
        loadWatchlists()
    }

    fun setNewListName(name: String) = _state.update { it.copy(newListName = name) }

    fun toggleInlineCreate() = _state.update {
        it.copy(showInlineCreate = !it.showInlineCreate, newListName = if (it.showInlineCreate) "" else it.newListName)
    }

    fun addToExisting(watchlistId: Long) {
        val listingId = _state.value.listingId ?: return
        viewModelScope.launch {
            _state.update { it.copy(submittingWatchlistId = watchlistId, error = null) }
            when (val result = repository.addItem(watchlistId, listingId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(submittingWatchlistId = null, successMessage = "Dodato u listu.")
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submittingWatchlistId = null, error = friendly(result.error))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun submitInlineCreate() {
        val listingId = _state.value.listingId ?: return
        val name = _state.value.newListName.trim()
        if (name.isBlank()) {
            _state.update { it.copy(error = "Naziv ne moze biti prazan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(creatingList = true, error = null) }
            when (val createResult = repository.create(name)) {
                is ApiResult.Success -> {
                    val newList = createResult.data
                    when (val addResult = repository.addItem(newList.id, listingId)) {
                        is ApiResult.Success -> _state.update {
                            it.copy(
                                creatingList = false,
                                showInlineCreate = false,
                                newListName = "",
                                watchlists = it.watchlists + newList,
                                successMessage = "Nova lista \"${newList.name}\" kreirana i hartija dodata.",
                            )
                        }
                        is ApiResult.Failure -> _state.update {
                            it.copy(
                                creatingList = false,
                                watchlists = it.watchlists + newList,
                                error = "Lista kreirana, ali dodavanje hartije nije uspelo: ${friendly(addResult.error)}",
                            )
                        }
                        ApiResult.Loading -> Unit
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(creatingList = false, error = friendly(createResult.error))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }

    private fun loadWatchlists() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.listMyWatchlists()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, watchlists = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = friendly(result.error))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    companion object {
        internal fun friendly(error: ApiError): String = when (error.kind) {
            ApiError.Kind.Conflict -> "Hartija je vec u toj listi."
            ApiError.Kind.Validation -> error.message
            ApiError.Kind.Unauthorized -> "Niste prijavljeni."
            ApiError.Kind.Forbidden -> "Nemate dozvolu za ovu akciju."
            ApiError.Kind.NotFound -> "Lista nije pronadjena."
            else -> error.message.ifBlank { "Doslo je do greske." }
        }
    }
}

data class AddToWatchlistState(
    val listingId: Long? = null,
    val listingTicker: String = "",
    val watchlists: List<WatchlistDto> = emptyList(),
    val loading: Boolean = false,
    val submittingWatchlistId: Long? = null,
    val creatingList: Boolean = false,
    val showInlineCreate: Boolean = false,
    val newListName: String = "",
    val error: String? = null,
    val successMessage: String? = null,
)
