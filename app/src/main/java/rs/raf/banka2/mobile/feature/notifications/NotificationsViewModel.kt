package rs.raf.banka2.mobile.feature.notifications

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
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter
import rs.raf.banka2.mobile.data.repository.NotificationRepository
import javax.inject.Inject

private const val PAGE_SIZE = 20

/**
 * TODO_final C2 #4 — In-app notifikacije (Mobile portovan iz FE-a).
 *
 * Drzimo `items + page + totalPages + filter + unreadCount` u jednom state-u.
 * UI klik na red okida `markAsRead` (optimisticki) + deep-link event.
 * `mark-all` updejtuje sve i resetuje unread count.
 *
 * Paritet sa `pages/Notifications/NotificationsPage.tsx`.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private val _events = Channel<NotificationsEvent>(Channel.BUFFERED)
    val events get() = _events.receiveAsFlow()

    init {
        refresh()
        refreshUnreadCount()
    }

    fun refresh() = load(_state.value.page, _state.value.filter)

    fun setFilter(filter: NotificationFilter) {
        if (filter == _state.value.filter) return
        load(0, filter)
    }

    fun nextPage() {
        val s = _state.value
        if (s.page + 1 < s.totalPages) load(s.page + 1, s.filter)
    }

    fun prevPage() {
        val s = _state.value
        if (s.page > 0) load(s.page - 1, s.filter)
    }

    private fun load(page: Int, filter: NotificationFilter) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.list(filter, page, PAGE_SIZE)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        items = result.data.content,
                        page = result.data.number,
                        totalPages = result.data.totalPages,
                        filter = filter,
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            when (val result = repository.getUnreadCount()) {
                is ApiResult.Success -> _state.update {
                    it.copy(unreadCount = result.data.count.coerceAtLeast(0))
                }
                else -> Unit // tihi neuspeh — paritet sa FE
            }
        }
    }

    fun onRowClick(notification: NotificationDto) {
        // Optimisticki: oznaci kao procitano u UI-u + posalji deep-link event.
        if (!notification.read) {
            val filterAtClick = _state.value.filter
            _state.update { current ->
                current.copy(items = current.items.map {
                    if (it.id == notification.id) it.copy(read = true) else it
                })
            }
            viewModelScope.launch {
                when (val result = repository.markAsRead(notification.id)) {
                    is ApiResult.Success -> refreshUnreadCount()
                    is ApiResult.Failure -> {
                        // Rollback samo ako filter nije promenjen u medjuvremenu.
                        if (_state.value.filter == filterAtClick) {
                            _state.update { current ->
                                current.copy(items = current.items.map {
                                    if (it.id == notification.id) it.copy(read = false) else it
                                })
                            }
                            _events.trySend(NotificationsEvent.ShowError(result.error.message))
                        }
                    }
                    ApiResult.Loading -> Unit
                }
            }
        }
        val target = NotificationDeepLink.resolve(notification)
        if (target != null) _events.trySend(NotificationsEvent.Navigate(target))
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (val result = repository.markAllAsRead()) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(items = current.items.map { it.copy(read = true) })
                    }
                    refreshUnreadCount()
                    _events.trySend(NotificationsEvent.ShowInfo("Sve notifikacije oznacene kao procitane."))
                }
                is ApiResult.Failure -> _events.trySend(NotificationsEvent.ShowError(result.error.message))
                ApiResult.Loading -> Unit
            }
        }
    }
}

data class NotificationsState(
    val loading: Boolean = false,
    val items: List<NotificationDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val filter: NotificationFilter = NotificationFilter.ALL,
    val unreadCount: Int = 0,
    val error: String? = null,
) {
    val canPrev: Boolean get() = page > 0
    val canNext: Boolean get() = page + 1 < totalPages
}

sealed interface NotificationsEvent {
    data class Navigate(val target: NotificationTarget) : NotificationsEvent
    data class ShowError(val message: String) : NotificationsEvent
    data class ShowInfo(val message: String) : NotificationsEvent
}

/**
 * Sealed deep-link target — UI sloj mapira ovo u konkretnu Route.
 * Cuvamo kao type-safe sealed klasu umesto stringa rute jer Navigation
 * koristi @Serializable rute, a route reference nije dozvoljen
 * van Compose layer-a.
 */
sealed interface NotificationTarget {
    data object Payments : NotificationTarget
    data object Orders : NotificationTarget
    data object Otc : NotificationTarget
    data object Funds : NotificationTarget
    data class Fund(val fundId: Long) : NotificationTarget
    data object Cards : NotificationTarget
    data object Loans : NotificationTarget
    data object Accounts : NotificationTarget
    data class QuickApprovePayment(val paymentId: Long, val notificationCreatedAt: String) :
        NotificationTarget
}

object NotificationDeepLink {
    /**
     * Razresava deep-link cilj iz `relatedEntityType` + `relatedEntityId`.
     * Vraca null ako ne mozemo da rezolvujemo (neki tipovi notifikacija
     * nemaju logican target).
     */
    fun resolve(n: NotificationDto): NotificationTarget? {
        // Specijalan slucaj: PAYMENT_PENDING_APPROVAL -> Quick Approve flow (Mobile bonus #7).
        if (n.type == "PAYMENT_PENDING_APPROVAL" && n.relatedEntityId != null) {
            return NotificationTarget.QuickApprovePayment(n.relatedEntityId, n.createdAt)
        }
        val type = n.relatedEntityType?.uppercase() ?: return null
        return when (type) {
            "PAYMENT" -> NotificationTarget.Payments
            "ORDER" -> NotificationTarget.Orders
            "OTC_OFFER", "OTC_CONTRACT" -> NotificationTarget.Otc
            "FUND" -> n.relatedEntityId?.let { NotificationTarget.Fund(it) }
                ?: NotificationTarget.Funds
            "CARD" -> NotificationTarget.Cards
            "LOAN" -> NotificationTarget.Loans
            "ACCOUNT" -> NotificationTarget.Accounts
            else -> null
        }
    }
}
