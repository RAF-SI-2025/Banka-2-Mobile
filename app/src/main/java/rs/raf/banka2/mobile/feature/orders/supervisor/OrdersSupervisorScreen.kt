package rs.raf.banka2.mobile.feature.orders.supervisor

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
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
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton

private val statusFilters = listOf<Pair<String?, String>>(
    null to "Svi",
    "PENDING" to "Pending",
    "APPROVED" to "Approved",
    "DONE" to "Done",
    "DECLINED" to "Declined"
)

@Composable
fun OrdersSupervisorScreen(
    onBack: () -> Unit,
    viewModel: OrdersSupervisorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var cancelDialog by remember { mutableStateOf<CancelDialogState?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OrdersSupervisorEvent.Toast) snackbar.showSnackbar(event.message)
        }
    }

    BankaScaffold(
        title = "Pregled svih naloga",
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusFilters) { (status, label) ->
                    val isSelected = state.statusFilter == status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { viewModel.setStatusFilter(status) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ErrorBanner(state.error)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.orders, key = { it.id }) { order ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${order.direction} · ${order.listingTicker ?: "?"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Kolicina ${order.quantity} · ${order.orderType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    DateFormatter.formatDateTime(order.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                order.approvedBy?.let {
                                    Text(
                                        "Odobrio: $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                order.onBehalfOfFundName?.let {
                                    Text(
                                        "Fond: $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(order.status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                order.totalValue?.let {
                                    Text(MoneyFormatter.format(it, 2), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        when (order.status) {
                            "PENDING" -> {
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrimaryButton(text = "Odobri", onClick = { viewModel.approve(order.id) }, modifier = Modifier.weight(1f))
                                    DangerButton(text = "Odbij", onClick = { viewModel.decline(order.id) }, modifier = Modifier.weight(1f))
                                }
                            }
                            "APPROVED" -> {
                                val remaining = order.remainingPortions ?: order.quantity
                                if (remaining > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    DangerButton(
                                        text = "Otkazi",
                                        onClick = { cancelDialog = CancelDialogState(order.id, remaining) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    cancelDialog?.let { dialog ->
        var qtyText by remember(dialog.orderId) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { cancelDialog = null },
            title = { Text("Otkazivanje naloga") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preostalo: ${dialog.remaining} jedinica.")
                    Text("Ostavi prazno za potpuno otkazivanje, ili unesi 1..${dialog.remaining - 1} za parcijalno.")
                    AppTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = "Parcijalna kolicina (opciono)",
                        keyboardType = KeyboardType.Number
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = qtyText.toIntOrNull()
                    val partial = parsed?.takeIf { it in 1 until dialog.remaining }
                    viewModel.decline(dialog.orderId, partial)
                    cancelDialog = null
                }) { Text("Potvrdi") }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialog = null }) { Text("Odustani") }
            }
        )
    }
}

private data class CancelDialogState(val orderId: Long, val remaining: Int)
