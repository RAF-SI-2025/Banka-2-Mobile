package rs.raf.banka2.mobile.feature.watchlist

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistDto
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistFilterType
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistItemDto
import java.math.RoundingMode

/**
 * [FE2 Mobile port — Watchlist] Glavni ekran watchlist-a.
 *
 * Mobilna verzija je vertikalna (mobile-first): gornja sekcija sa user's
 * watchlist-ima (LazyRow chip-ovi sa Edit/Delete iconama), srednja sa filter
 * chip-ovima (ALL/STOCK/...), donja LazyColumn stavki sa Trguj/Ukloni dugmadima.
 *
 * Razlika od FE web verzije (split 1/3 + 2/3): mobile koristi tab pristup jer
 * portrait ne moze da prikaze obe panele istovremeno.
 */
@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onTradeListing: (Long) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Watchlist",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::openCreateDialog) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Nova lista",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
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

            // Watchlists row
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Moje liste",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = viewModel::openCreateDialog) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Nova lista")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.loadingLists) {
                        Text(
                            "Ucitavanje...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (state.watchlists.isEmpty()) {
                        Text(
                            "Nemate listi. Pritisnite + da kreirate prvu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.watchlists, key = { it.id }) { wl ->
                                WatchlistChip(
                                    watchlist = wl,
                                    selected = wl.id == state.selectedId,
                                    onClick = { viewModel.selectWatchlist(wl.id) },
                                    onRename = { viewModel.openRenameDialog(wl) },
                                    onDelete = { viewModel.openDeleteConfirm(wl) },
                                )
                            }
                        }
                    }
                }
            }

            // Filter chips (tip hartije)
            if (state.selectedId != null) {
                item { FilterChipsRow(state.filter, viewModel::setFilter) }
            }

            // Items
            if (state.selectedWatchlist != null) {
                val items = state.items
                if (state.loadingItems) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Ucitavanje stavki...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (items.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.BookmarkBorder,
                            title = "Lista je prazna",
                            description = "Otvori berzu i dodaj hartiju u ovu listu preko dugmeta na detaljima hartije.",
                        )
                    }
                } else {
                    items(items, key = { it.id }) { itm ->
                        WatchlistItemRow(
                            item = itm,
                            onTrade = { onTradeListing(itm.listingId) },
                            onRemove = { viewModel.removeItem(itm.listingId) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ---- Dijalozi ----
    if (state.createDialogOpen) {
        WatchlistNameDialog(
            title = "Nova lista pracenja",
            value = state.createNameInput,
            onValueChange = viewModel::setCreateNameInput,
            confirmText = if (state.submitting) "Kreiranje..." else "Kreiraj",
            confirmEnabled = !state.submitting && state.createNameInput.isNotBlank(),
            onConfirm = viewModel::submitCreate,
            onDismiss = viewModel::dismissCreateDialog,
        )
    }
    state.renameTarget?.let { target ->
        WatchlistNameDialog(
            title = "Preimenuj listu",
            value = state.renameNameInput,
            onValueChange = viewModel::setRenameNameInput,
            confirmText = if (state.submitting) "Cuvanje..." else "Sacuvaj",
            confirmEnabled = !state.submitting && state.renameNameInput.isNotBlank() && state.renameNameInput.trim() != target.name,
            onConfirm = viewModel::submitRename,
            onDismiss = viewModel::dismissRenameDialog,
        )
    }
    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Obrisi listu") },
            text = { Text("Da li sigurno zelite da obrisete listu \"${target.name}\"? Sve stavke ce biti uklonjene.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::submitDelete,
                    enabled = !state.submitting,
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
private fun WatchlistChip(
    watchlist: WatchlistDto,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            watchlist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${watchlist.itemCount} stavki",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, "Preimenuj", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        "Obrisi",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        // Border ring (use Spacer with background trick — simplest)
        if (selected) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(borderColor)
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: WatchlistFilterType,
    onChange: (WatchlistFilterType) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FilterList,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Filter:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WatchlistFilterType.entries.toList()) { f ->
                val isSelected = f == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onChange(f) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        f.labelSr,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistItemRow(
    item: WatchlistItemDto,
    onTrade: () -> Unit,
    onRemove: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.ticker,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${item.listingName ?: "—"} · ${item.securityType ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.exchangeName != null) {
                    Text(
                        item.exchangeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.currentPrice?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "—",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val change = item.dailyChange?.toDouble() ?: 0.0
                if (item.dailyChange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (change >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = if (change >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.size(2.dp))
                        // R1 812: dailyChange je PROCENAT (BE changePercent) -> prikazi sa %.
                        Text(
                            "${item.dailyChange.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (change >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                item.volume?.let {
                    Text(
                        "Vol: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Trguj",
                onClick = onTrade,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Filled.ShoppingCart,
                height = 40.dp,
            )
            SecondaryButton(
                text = "Ukloni",
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Filled.Close,
                height = 40.dp,
            )
        }
    }
}

@Composable
private fun WatchlistNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmText: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Naziv liste") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Otkazi") }
        },
    )
}
