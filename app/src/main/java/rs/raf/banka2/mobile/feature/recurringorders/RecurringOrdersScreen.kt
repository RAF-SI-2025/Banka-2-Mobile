package rs.raf.banka2.mobile.feature.recurringorders

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringCadence
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringDirection
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringMode
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderDto
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderLabels
import java.math.RoundingMode

/**
 * [FE3 Mobile port — Trajni nalozi (DCA)] Glavni ekran.
 *
 * Mobile portrait koristi vertikalni layout: forma za novi nalog je u Card na
 * vrhu (collapsible), ispod toga tab filter + lista postojecih naloga.
 */
@Composable
fun RecurringOrdersScreen(
    onBack: () -> Unit,
    viewModel: RecurringOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var formExpanded by remember { mutableStateOf(false) }

    BankaScaffold(
        title = "Trajni nalozi (DCA)",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Novi trajni nalog",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Automatski kupuj/prodavaj na zadati period.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { formExpanded = !formExpanded }) {
                            Text(if (formExpanded) "Sakrij" else "Otvori")
                        }
                    }
                    if (formExpanded) {
                        Spacer(Modifier.height(8.dp))
                        NewOrderForm(state, viewModel)
                    }
                }
            }

            // Stat chips + filter tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatChip("Aktivni", state.activeCount, MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    StatChip("Pauzirani", state.pausedCount, MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    StatChip("Ukupno", state.totalCount, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                }
            }
            item { FilterTabsRow(state.filter, viewModel::setFilter, state) }

            val filtered = state.filteredOrders
            if (state.loading) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Ucitavanje...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Repeat,
                        title = "Nema trajnih naloga",
                        description = "Kreiraj prvi nalog otvaranjem forme iznad.",
                    )
                }
            } else {
                items(filtered, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        submitting = state.submittingId == order.id,
                        onPause = { viewModel.pause(order.id) },
                        onResume = { viewModel.resume(order.id) },
                        onCancel = { viewModel.openCancelConfirm(order) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    state.cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelConfirm,
            title = { Text("Otkazi trajni nalog") },
            text = {
                Text(
                    "Da li sigurno zelite da otkazete trajni nalog za ${target.listingTicker ?: "hartiju"}? " +
                            "Akcija je nepovratna."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmCancel,
                    enabled = state.submittingId == null,
                ) {
                    Text("Otkazi nalog", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelConfirm) { Text("Nazad") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOrderForm(
    state: RecurringOrdersState,
    viewModel: RecurringOrdersViewModel,
) {
    val form = state.form

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Listing search/autocomplete
        OutlinedTextField(
            value = form.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("Pretraga hartije (ticker)") },
            placeholder = { Text("npr. AAPL, MSFT") },
            singleLine = true,
            trailingIcon = if (form.selectedListing != null) {
                {
                    IconButton(onClick = viewModel::clearListing) {
                        Icon(Icons.Filled.Close, "Ocisti")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (form.searchResults.isNotEmpty() && form.selectedListing == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                form.searchResults.forEach { listing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectListing(listing) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            listing.ticker,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${listing.listingType} · ${listing.price}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        form.selectedListing?.let { listing ->
            Text(
                "Izabrana hartija: ${listing.ticker} (${listing.listingType})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Direction: BUY / SELL toggle
        SegmentedToggle(
            options = RecurringDirection.entries.map { it.apiValue to it.labelSr },
            selected = form.direction.apiValue,
            onChange = { api ->
                viewModel.setDirection(RecurringDirection.entries.first { it.apiValue == api })
            },
        )

        // Mode: BYAMOUNT / BYQUANTITY toggle
        SegmentedToggle(
            options = RecurringMode.entries.map { it.apiValue to it.labelSr },
            selected = form.mode.apiValue,
            onChange = { api ->
                viewModel.setMode(RecurringMode.entries.first { it.apiValue == api })
            },
        )

        // Value input
        OutlinedTextField(
            value = form.valueText,
            onValueChange = viewModel::setValueText,
            label = {
                Text(if (form.mode == RecurringMode.BYAMOUNT) "Iznos (po valuti racuna)" else "Broj akcija")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        // Account dropdown
        AccountDropdown(
            accounts = state.accounts,
            selectedId = form.accountId,
            onSelected = viewModel::selectAccount,
        )

        // Cadence: DAILY / WEEKLY / MONTHLY
        SegmentedToggle(
            options = RecurringCadence.entries.map { it.apiValue to it.labelSr },
            selected = form.cadence.apiValue,
            onChange = { api ->
                viewModel.setCadence(RecurringCadence.entries.first { it.apiValue == api })
            },
        )

        if (state.formError != null) {
            Text(
                state.formError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.formSuccess != null) {
            Text(
                state.formSuccess!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        PrimaryButton(
            text = if (state.submittingForm) "Kreiranje..." else "Kreiraj nalog",
            onClick = viewModel::submitNewOrder,
            enabled = !state.submittingForm,
            loading = state.submittingForm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SegmentedToggle(
    options: List<Pair<String, String>>,
    selected: String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (api, label) ->
            val isSelected = api == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onChange(api) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    accounts: List<AccountDto>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedId }
    val displayText = selected?.let {
        "${AccountFormatter.formatAccountNumber(it.accountNumber)} (${it.currency ?: ""})"
    } ?: "Odaberi racun"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Racun za izvrsavanje") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                AccountFormatter.formatAccountNumber(acc.accountNumber),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${acc.currency ?: ""} · ${MoneyFormatter.format(acc.availableBalance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelected(acc.id)
                        expanded = false
                    },
                )
            }
        }
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
    selected: RecurringOrderLabels.FilterTab,
    onChange: (RecurringOrderLabels.FilterTab) -> Unit,
    state: RecurringOrdersState,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RecurringOrderLabels.FilterTab.entries.toList()) { tab ->
            val count = when (tab) {
                RecurringOrderLabels.FilterTab.ACTIVE -> state.activeCount
                RecurringOrderLabels.FilterTab.PAUSED -> state.pausedCount
                RecurringOrderLabels.FilterTab.ALL -> state.totalCount
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
private fun OrderCard(
    order: RecurringOrderDto,
    submitting: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        order.listingTicker ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusBadge(active = order.active)
                }
                Spacer(Modifier.height(4.dp))
                val directionLabel = RecurringOrderLabels.directionLabel(order.direction)
                val modeLabel = RecurringOrderLabels.modeLabel(order.mode)
                val cadenceLabel = RecurringOrderLabels.cadenceLabel(order.cadence)
                Text(
                    "$directionLabel · $modeLabel · $cadenceLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Vrednost: ${order.value.setScale(4, RoundingMode.HALF_UP).toPlainString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                order.nextRun?.let {
                    Text(
                        "Sledece izvrsavanje: ${DateFormatter.formatDate(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onCancel, enabled = !submitting) {
                    Icon(
                        Icons.Filled.Cancel,
                        "Otkazi",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(
                    onClick = if (order.active) onPause else onResume,
                    enabled = !submitting,
                ) {
                    Icon(
                        if (order.active) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (order.active) "Pauziraj" else "Nastavi",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean) {
    val (label, bg, fg) = if (active) {
        Triple("Aktivan", MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), MaterialTheme.colorScheme.tertiary)
    } else {
        Triple("Pauziran", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
