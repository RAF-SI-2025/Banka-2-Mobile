package rs.raf.banka2.mobile.feature.orders.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.order.OrderDto

@Composable
fun MyOrdersScreen(
    onBack: () -> Unit,
    viewModel: MyOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var cancelTarget by remember { mutableStateOf<OrderDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is MyOrdersEvent.Toast) snackbar.showSnackbar(event.message)
        }
    }

    BankaScaffold(
        title = "Moji nalozi",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Osvezi", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        backgroundDecoration = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.error != null) item { ErrorBanner(state.error) }
            if (state.orders.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Receipt,
                        title = "Nema naloga",
                        description = "Tvoj prvi BUY/SELL nalog ce se pojaviti ovde."
                    )
                }
            }
            items(state.orders, key = { it.id }) { order ->
                OrderCard(order = order, onCancel = { cancelTarget = order })
            }
        }
    }

    cancelTarget?.let { order ->
        CancelDialog(
            order = order,
            onDismiss = { cancelTarget = null },
            onConfirm = { qty ->
                viewModel.cancel(order, qty)
                cancelTarget = null
            }
        )
    }
}

@Composable
private fun OrderCard(order: OrderDto, onCancel: () -> Unit) {
    val statusColor = when (order.status) {
        "DONE" -> MaterialTheme.colorScheme.tertiary
        "PENDING", "APPROVED" -> MaterialTheme.colorScheme.primary
        "DECLINED", "CANCELLED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val canCancel = order.status == "PENDING" || (order.status == "APPROVED" && order.filledQuantity != null && order.filledQuantity < order.quantity)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${order.direction} · ${order.listingTicker ?: order.listingId?.toString() ?: "?"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${order.orderType} · kolicina ${order.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (order.filledQuantity != null) {
                    Text(
                        "Izvrseno: ${order.filledQuantity}/${order.quantity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (order.approvedBy != null) {
                    Text("Odobrio: ${order.approvedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    DateFormatter.formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    order.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
                order.totalValue?.let {
                    Text(
                        MoneyFormatter.format(it, 2),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                order.fee?.let {
                    Text(
                        "Provizija: ${MoneyFormatter.format(it, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (canCancel) {
            Spacer(Modifier.height(8.dp))
            DangerButton(text = "Otkazi", onClick = onCancel, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CancelDialog(
    order: OrderDto,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val remaining = order.remainingPortions ?: (order.quantity - (order.filledQuantity ?: 0))
    var partialText by remember { mutableStateOf("") }
    val partial = partialText.toIntOrNull()
    val canPartial = partial != null && partial in 1 until remaining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otkazivanje naloga") },
        text = {
            Column {
                Text(
                    "Preostalo ${remaining} kom za izvrsenje. Mozes da otkazes ceo nalog ili samo deo (parcijalni cancel).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = partialText,
                    onValueChange = { partialText = it.filter { ch -> ch.isDigit() } },
                    label = "Parcijalna kolicina (1..${remaining - 1})",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (canPartial) partial else null) }) {
                Text(if (canPartial) "Parcijalno" else "Otkazi nalog")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Nazad") } }
    )
}
