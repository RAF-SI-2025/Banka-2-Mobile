package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.NotificationRepository
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 30_000L

/**
 * TODO_final C2 #4 — In-app notifikacije.
 *
 * Bell ikona sa unread badge-om. Polling 30s, isto kao FE.
 * Badge trim "9+" za vrednosti >= 10. Klik vodi na Notifications ekran
 * preko `onClick` callback-a (parent kontroliše navigaciju).
 *
 * Paritet sa `Banka-2-Frontend/src/components/shared/NotificationBell.tsx`.
 */
@Composable
fun NotificationBellButton(
    onClick: () -> Unit,
    viewModel: NotificationBellViewModel = hiltViewModel(),
) {
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    Box(modifier = Modifier.padding(4.dp)) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = if (unread > 0) "Notifikacije, $unread neprocitanih"
                else "Notifikacije",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (unread > 0) {
            val label = if (unread > 9) "9+" else unread.toString()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Mini-VM koji samo poll-uje unread count. Ne deli stanje sa
 * NotificationsViewModel (jer one ne moraju da koegzistuju).
 */
@HiltViewModel
class NotificationBellViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            fetchOnce()
            while (true) {
                delay(POLL_INTERVAL_MS)
                fetchOnce()
            }
        }
    }

    private suspend fun fetchOnce() {
        when (val result = repository.getUnreadCount()) {
            is ApiResult.Success -> _unreadCount.update {
                result.data.count.coerceAtLeast(0)
            }
            else -> Unit // tihi neuspeh
        }
    }
}
