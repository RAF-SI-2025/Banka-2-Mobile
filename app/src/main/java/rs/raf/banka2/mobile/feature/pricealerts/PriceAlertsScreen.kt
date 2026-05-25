package rs.raf.banka2.mobile.feature.pricealerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertLabels
import java.math.RoundingMode

/**
 * [FE2 Mobile port — Price Alerts] Glavni ekran cenovnih alarma.
 *
 * Lista alarma sa 3-state filter chip-om (Aktivni/Istorija/Sve), brisanje sa
 * confirm dialog-om, empty/loading/error stanjima. Kreiranje se radi iz
 * SecuritiesDetailsScreen-a ("Postavi cenovni alarm" dugme) — ovde nema FAB-a.
 */
@Composable
fun PriceAlertsScreen(
    onBack: () -> Unit,
    onOpenSecurities: () -> Unit,
    viewModel: PriceAlertsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Cenovni alarmi",
        onBack = onBack,
        backgroundDecoration = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Postavi obavestenje kad cena hartije dosegne zadati prag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatChip("Aktivnih", state.activeCount, MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                        StatChip("Istorija", state.historyCount, MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        StatChip("Ukupno", state.totalCount, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Filter chips
            item { FilterTabsRow(state.filter, viewModel::setFilter, state) }

            val filtered = state.filteredAlerts
            if (state.loading) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Ucitavanje...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.NotificationsNone,
                        title = when (state.filter) {
                            PriceAlertLabels.FilterTab.ACTIVE -> "Nemate aktivnih alarma"
                            PriceAlertLabels.FilterTab.HISTORY -> "Nema okidnutih alarma"
                            PriceAlertLabels.FilterTab.ALL -> "Nemate cenovnih alarma"
                        },
                        description = "Otvori berzu, izaberi hartiju i klikni \"Postavi cenovni alarm\".",
                        action = {
                            TextButton(onClick = onOpenSecurities) {
                                Text("Otvori berzu")
                            }
                        },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { alert ->
                    AlertCard(
                        alert = alert,
                        onDelete = { viewModel.openDeleteConfirm(alert) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Obrisi cenovni alarm") },
            text = {
                Text(
                    "Da li sigurno zelite da obrisete alarm za ${target.listingTicker} (${PriceAlertLabels.conditionLabel(target.condition)} ${target.threshold.toPlainString()})?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    enabled = state.deletingId == null,
                ) {
                    Text("Obrisi", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Otkazi") }
            },
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun FilterTabsRow(
    selected: PriceAlertLabels.FilterTab,
    onChange: (PriceAlertLabels.FilterTab) -> Unit,
    state: PriceAlertsState,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PriceAlertLabels.FilterTab.entries.toList()) { tab ->
            val count = when (tab) {
                PriceAlertLabels.FilterTab.ACTIVE -> state.activeCount
                PriceAlertLabels.FilterTab.HISTORY -> state.historyCount
                PriceAlertLabels.FilterTab.ALL -> state.totalCount
            }
            val isSelected = tab == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onChange(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tab.labelSr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: PriceAlertDto,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alert.listingTicker,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusBadge(active = alert.active)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (alert.condition.equals("ABOVE", true)) Icons.Filled.TrendingUp
                        else Icons.Filled.TrendingDown,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (alert.condition.equals("ABOVE", true)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        PriceAlertLabels.conditionLabel(alert.condition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Prag: ${alert.threshold.setScale(4, RoundingMode.HALF_UP).toPlainString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                if (alert.listingType != null) {
                    Text(
                        "${alert.listingType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                alert.createdAt?.let {
                    Text(
                        "Kreiran: ${DateFormatter.formatDate(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                alert.triggeredAt?.let {
                    Text(
                        "Okidnut: ${DateFormatter.formatDate(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    "Obrisi alarm",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean) {
    val (label, bg, fg) = if (active) {
        Triple("Aktivan", MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), MaterialTheme.colorScheme.tertiary)
    } else {
        Triple("Okidnut", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
            null,
            modifier = Modifier.size(12.dp),
            tint = fg,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
