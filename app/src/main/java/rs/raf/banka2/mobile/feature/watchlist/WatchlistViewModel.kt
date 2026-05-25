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
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistFilterType
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistItemDto
import rs.raf.banka2.mobile.data.repository.WatchlistRepository
import javax.inject.Inject

/**
 * [FE2 Mobile port — Watchlist] Glavni VM za WatchlistScreen.
 *
 * Drzi listu korisnikovih watchlist-a (lista), izabrani watchlist (items),
 * filter chip po tipu hartije, plus state masina za 3 dijaloga (create / rename /
 * delete-confirm).
 *
 * 409 (Conflict) na addItem-u prikazujemo kao posebnu poruku — duplikat.
 */
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: WatchlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistState())
    val state: StateFlow<WatchlistState> = _state.asStateFlow()

    init {
        refreshLists()
    }

    fun refreshLists() {
        viewModelScope.launch {
            _state.update { it.copy(loadingLists = true, error = null) }
            when (val result = repository.listMyWatchlists()) {
                is ApiResult.Success -> {
                    val previousSelected = _state.value.selectedId
                    val newSelected = previousSelected?.takeIf { id -> result.data.any { it.id == id } }
                        ?: result.data.firstOrNull()?.id
                    _state.update {
                        it.copy(
                            loadingLists = false,
                            watchlists = result.data,
                            selectedId = newSelected,
                        )
                    }
                    newSelected?.let { loadItems(it) }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loadingLists = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun selectWatchlist(id: Long) {
        if (_state.value.selectedId == id) return
        _state.update { it.copy(selectedId = id) }
        loadItems(id)
    }

    fun setFilter(filter: WatchlistFilterType) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        _state.value.selectedId?.let { loadItems(it) }
    }

    fun openCreateDialog() = _state.update { it.copy(createDialogOpen = true, createNameInput = "") }
    fun dismissCreateDialog() = _state.update { it.copy(createDialogOpen = false, createNameInput = "") }
    fun setCreateNameInput(name: String) = _state.update { it.copy(createNameInput = name) }

    fun submitCreate() {
        val name = _state.value.createNameInput.trim()
        if (name.isBlank()) {
            _state.update { it.copy(error = "Naziv ne moze biti prazan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = repository.create(name)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            createDialogOpen = false,
                            createNameInput = "",
                            watchlists = it.watchlists + result.data,
                            selectedId = result.data.id,
                            items = emptyList(),
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = errorMessage(result.error, "Neuspeh kreiranja liste."))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun openRenameDialog(watchlist: WatchlistDto) = _state.update {
        it.copy(renameTarget = watchlist, renameNameInput = watchlist.name)
    }
    fun dismissRenameDialog() = _state.update { it.copy(renameTarget = null, renameNameInput = "") }
    fun setRenameNameInput(name: String) = _state.update { it.copy(renameNameInput = name) }

    fun submitRename() {
        val target = _state.value.renameTarget ?: return
        val name = _state.value.renameNameInput.trim()
        if (name.isBlank()) {
            _state.update { it.copy(error = "Naziv ne moze biti prazan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = repository.rename(target.id, name)) {
                is ApiResult.Success -> _state.update { st ->
                    st.copy(
                        submitting = false,
                        renameTarget = null,
                        renameNameInput = "",
                        watchlists = st.watchlists.map { if (it.id == target.id) result.data else it },
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = errorMessage(result.error, "Neuspeh preimenovanja."))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun openDeleteConfirm(watchlist: WatchlistDto) = _state.update {
        it.copy(deleteTarget = watchlist)
    }
    fun dismissDeleteConfirm() = _state.update { it.copy(deleteTarget = null) }

    fun submitDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = repository.delete(target.id)) {
                is ApiResult.Success -> {
                    val newLists = _state.value.watchlists.filterNot { it.id == target.id }
                    val newSelected = if (_state.value.selectedId == target.id) newLists.firstOrNull()?.id
                    else _state.value.selectedId
                    _state.update {
                        it.copy(
                            submitting = false,
                            deleteTarget = null,
                            watchlists = newLists,
                            selectedId = newSelected,
                            items = if (newSelected == null) emptyList() else it.items,
                        )
                    }
                    newSelected?.let { loadItems(it) }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, deleteTarget = null, error = errorMessage(result.error, "Neuspeh brisanja."))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun removeItem(itemListingId: Long) {
        val watchlistId = _state.value.selectedId ?: return
        viewModelScope.launch {
            when (val result = repository.removeItem(watchlistId, itemListingId)) {
                is ApiResult.Success -> _state.update {
                    val newItems = it.items.filterNot { itm -> itm.listingId == itemListingId }
                    it.copy(
                        items = newItems,
                        watchlists = it.watchlists.map { wl ->
                            if (wl.id == watchlistId) wl.copy(itemCount = newItems.size) else wl
                        },
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = errorMessage(result.error, "Neuspeh uklanjanja stavke."))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun loadItems(watchlistId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loadingItems = true) }
            when (val result = repository.listItems(watchlistId, _state.value.filter)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loadingItems = false, items = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loadingItems = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    companion object {
        /** Public za testabilnost — mapira ApiError u user-facing poruku. */
        internal fun errorMessage(error: ApiError, default: String): String = when (error.kind) {
            ApiError.Kind.Conflict -> "Stavka vec postoji u listi."
            ApiError.Kind.Validation -> error.message
            ApiError.Kind.Unauthorized -> "Niste prijavljeni."
            ApiError.Kind.Forbidden -> "Nemate dozvolu za ovu akciju."
            ApiError.Kind.NotFound -> "Lista nije pronadjena."
            else -> error.message.takeIf { it.isNotBlank() } ?: default
        }
    }
}

data class WatchlistState(
    val watchlists: List<WatchlistDto> = emptyList(),
    val selectedId: Long? = null,
    val items: List<WatchlistItemDto> = emptyList(),
    val filter: WatchlistFilterType = WatchlistFilterType.ALL,
    val loadingLists: Boolean = false,
    val loadingItems: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val createDialogOpen: Boolean = false,
    val createNameInput: String = "",
    val renameTarget: WatchlistDto? = null,
    val renameNameInput: String = "",
    val deleteTarget: WatchlistDto? = null,
) {
    val selectedWatchlist: WatchlistDto?
        get() = watchlists.find { it.id == selectedId }
}
