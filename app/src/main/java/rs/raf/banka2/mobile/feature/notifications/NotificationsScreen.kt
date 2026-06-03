package rs.raf.banka2.mobile.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter
import rs.raf.banka2.mobile.data.dto.notification.NotificationType

/**
 * TODO_final C2 #4 — Mobile portovan in-app inbox.
 *
 * Paritet sa `pages/Notifications/NotificationsPage.tsx`:
 * filter pill-ovi (Sve/Neprocitane) + paginacija + ikon-po-tipu +
 * deep-link na referenced entity. "Mark all" dugme ako ima > 0 unread.
 *
 * Deep-link mapiranje radi se na callback-u `onTarget`, parent
 * (AppNavHost) ga prevodi u konkretnu Routes destinaciju.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onTarget: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationsEvent.Navigate -> onTarget(event.target)
                is NotificationsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is NotificationsEvent.ShowInfo -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BankaScaffold(
        title = "Notifikacije",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        backgroundDecoration = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            FilterRow(
                filter = state.filter,
                unreadCount = state.unreadCount,
                onFilter = viewModel::setFilter,
                onMarkAll = viewModel::markAllAsRead,
                markAllEnabled = state.unreadCount > 0,
            )
            Spacer(Modifier.height(12.dp))

            if (state.error != null) {
                ErrorBanner(state.error)
                Spacer(Modifier.height(12.dp))
            }

            if (state.items.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = if (state.filter == NotificationFilter.UNREAD)
                        "Nema neprocitanih"
                    else "Nemate notifikacija",
                    description = if (state.filter == NotificationFilter.UNREAD)
                        "Sve notifikacije su procitane."
                        else "Cim se nesto desi na tvom nalogu, javicemo ti ovde."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { n ->
                        NotificationRow(n = n, onClick = { viewModel.onRowClick(n) })
                    }
                    if (state.totalPages > 1) {
                        item {
                            PaginationRow(
                                page = state.page,
                                totalPages = state.totalPages,
                                canPrev = state.canPrev,
                                canNext = state.canNext,
                                onPrev = viewModel::prevPage,
                                onNext = viewModel::nextPage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filter: NotificationFilter,
    unreadCount: Int,
    onFilter: (NotificationFilter) -> Unit,
    onMarkAll: () -> Unit,
    markAllEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(
            onClick = { onFilter(NotificationFilter.ALL) },
            label = { Text("Sve") },
            colors = if (filter == NotificationFilter.ALL)
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onPrimary,
                )
            else AssistChipDefaults.assistChipColors()
        )
        val unreadLabel = if (unreadCount > 0) "Neprocitane ($unreadCount)" else "Neprocitane"
        AssistChip(
            onClick = { onFilter(NotificationFilter.UNREAD) },
            label = { Text(unreadLabel) },
            colors = if (filter == NotificationFilter.UNREAD)
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onPrimary,
                )
            else AssistChipDefaults.assistChipColors()
        )
        Spacer(Modifier.weight(1f))
        FilledTonalButton(
            onClick = onMarkAll,
            enabled = markAllEnabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Oznaci sve")
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationDto, onClick: () -> Unit) {
    val icon = iconFor(n.type)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (!n.read) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!n.read) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = n.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!n.read) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = n.message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = NotificationType.labelOf(n.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = n.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!n.read) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}

@Composable
private fun PaginationRow(
    page: Int,
    totalPages: Int,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrev, enabled = canPrev) { Text("Prethodna") }
        Text(
            text = "Strana ${page + 1} od $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onNext, enabled = canNext) { Text("Sledeca") }
    }
}

// P1-mobile-banking-1 (R1-151): grane uskladjene sa BE NotificationType enum imenima.
private fun iconFor(type: String): ImageVector = when (NotificationType.normalize(type)) {
    NotificationType.PAYMENT, NotificationType.TRANSFER,
    NotificationType.LIMIT_CHANGE -> Icons.Filled.Paid
    NotificationType.ORDER_PENDING, NotificationType.ORDER_APPROVED,
    NotificationType.ORDER_DECLINED, NotificationType.ORDER_EXECUTED,
    NotificationType.ORDER_PARTIAL_FILL, NotificationType.ORDER_CANCELLED ->
        Icons.Filled.ShoppingCart
    NotificationType.OTC_COUNTER_OFFER, NotificationType.OTC_ACCEPTED,
    NotificationType.OTC_DECLINED, NotificationType.OTC_CONTRACT_EXPIRING ->
        Icons.Outlined.Handshake
    NotificationType.LOAN_CREATED, NotificationType.LOAN_APPROVED,
    NotificationType.LOAN_REJECTED -> Icons.Filled.PriceCheck
    NotificationType.CARD_BLOCKED, NotificationType.CARD_UNBLOCKED -> Icons.Filled.CreditCard
    NotificationType.ACCOUNT_LOCKED -> Icons.Filled.Lock
    NotificationType.PRICE_ALERT_TRIGGERED -> Icons.Filled.Bolt
    else -> Icons.Outlined.Notifications
}
