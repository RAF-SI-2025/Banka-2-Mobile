package rs.raf.banka2.mobile.feature.portfolio

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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.WorkOutline
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
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioItemDto

@Composable
fun PortfolioScreen(
    onBack: () -> Unit,
    onSell: (Long) -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var publicTarget by remember { mutableStateOf<PortfolioItemDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is PortfolioEvent.Toast) snackbar.showSnackbar(event.message)
        }
    }

    BankaScaffold(
        title = "Portfolio",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) item { ErrorBanner(state.error) }
            state.summary?.let { summary ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Ukupna vrednost", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            MoneyFormatter.formatWithCurrency(summary.totalValue, summary.currency ?: "USD"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Profit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.format(summary.totalProfit, 2),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (summary.totalProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                            summary.taxOwed?.let { tax ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Procenjeni porez (15%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(tax, 2), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
            if (state.positions.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.WorkOutline,
                        title = "Portfolio je prazan",
                        description = "Posle prvog izvrsenog naloga, hartije ce se pojaviti ovde."
                    )
                }
            }
            items(state.positions, key = { it.id }) { item ->
                PortfolioRow(
                    item = item,
                    onSell = { item.listingId?.let(onSell) },
                    onPublic = { publicTarget = item },
                    onExercise = { item.optionId?.let { id -> viewModel.exerciseOption(id) } }
                )
            }
        }
    }

    publicTarget?.let { target ->
        PublicQuantityDialog(
            item = target,
            onDismiss = { publicTarget = null },
            onConfirm = { qty ->
                viewModel.setPublicQuantity(target, qty)
                publicTarget = null
            }
        )
    }
}

@Composable
private fun PortfolioRow(
    item: PortfolioItemDto,
    onSell: () -> Unit,
    onPublic: () -> Unit,
    onExercise: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.listingTicker ?: "—", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(item.listingName ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.isOption) {
                    Text(
                        "${item.optionType ?: "OPT"} · strike ${item.strikePrice?.let { MoneyFormatter.format(it, 2) } ?: "—"} · settlement ${item.settlementDate ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Kolicina ${item.quantity} · prosek ${MoneyFormatter.format(item.averageBuyPrice, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if ((item.publicQuantity ?: 0) > 0) {
                    Text(
                        "Javno: ${item.publicQuantity} kom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                item.currentPrice?.let { price ->
                    Text(MoneyFormatter.format(price, 2), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                item.profitPercent?.let { p ->
                    Text(
                        "${if (p >= 0) "+" else ""}${MoneyFormatter.format(p, 2)} %",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (p >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
                if (item.itm == true) {
                    Text("ITM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (item.isOption) {
            // Spec linija: "Za opcije: mogucnost iskoriscavanja ako settlement nije prosao i ako je ITM"
            val canExercise = item.itm == true && !item.settlementDate.isNullOrBlank()
            SecondaryButton(
                text = if (canExercise) "Iskoristi opciju" else "Iskoristi (uslovi nisu ispunjeni)",
                onClick = onExercise,
                enabled = canExercise,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Javna ponuda", onClick = onPublic, leadingIcon = Icons.Filled.Public, modifier = Modifier.weight(1f))
                DangerButton(text = "Prodaj", onClick = onSell, leadingIcon = Icons.Filled.Sell, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PublicQuantityDialog(
    item: PortfolioItemDto,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf((item.publicQuantity ?: 0).toString()) }
    val parsed = text.toIntOrNull()
    val maxAvailable = item.quantity - (item.reservedQuantity ?: 0) - (item.inOrderQuantity ?: 0)
    val isValid = parsed != null && parsed in 0..maxAvailable

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Javna kolicina za OTC") },
        text = {
            Column {
                Text(
                    "Postavi koliko kom hartija je dostupno drugim korisnicima na OTC trzistu (max $maxAvailable).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() } },
                    label = "Javna kolicina",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = isValid) {
                Text("Sacuvaj")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}
