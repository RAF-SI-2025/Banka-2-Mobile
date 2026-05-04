package rs.raf.banka2.mobile.feature.securities.details

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.option.OptionChainDto

@Composable
fun SecuritiesDetailsScreen(
    onBack: () -> Unit,
    onOrder: (Long, String) -> Unit,
    viewModel: SecuritiesDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listing = state.listing

    BankaScaffold(
        title = listing?.ticker ?: "Detalji",
        onBack = onBack,
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
            if (listing != null) {
                item { HeaderCard(listing) }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        PeriodTabRow(state.period, viewModel::setPeriod)
                        Spacer(Modifier.height(8.dp))
                        if (state.history.isNotEmpty()) {
                            PriceChart(points = state.history, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(
                                "Nema istorijskih podataka.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (listing.isTestMode == true) "SIMULIRANI PODACI"
                            else "LIVE",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (listing.isTestMode == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                item { StatGrid(listing) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "Kupi",
                            onClick = { onOrder(listing.id, "BUY") },
                            modifier = Modifier.weight(1f)
                        )
                        DangerButton(
                            text = "Prodaj",
                            onClick = { onOrder(listing.id, "SELL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (listing.listingType.equals("STOCK", true) && state.optionChains.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Opcije",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StrikeRowsStepper(
                                value = state.strikeRowsAroundPrice,
                                onChange = viewModel::setStrikeRowFilter
                            )
                        }
                    }
                    items(state.optionChains, key = { it.settlementDate ?: it.hashCode().toString() }) { chain ->
                        OptionChainCard(
                            chain = chain,
                            currentPrice = listing.price,
                            rowsAroundPrice = state.strikeRowsAroundPrice,
                            onExercise = { viewModel.exerciseOption(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(listing: ListingDto) {
    val percent = listing.changePercent ?: 0.0
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(listing.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "${listing.ticker} · ${listing.listingType}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = MoneyFormatter.formatWithCurrency(listing.price, listing.currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${if (percent >= 0) "+" else ""}${MoneyFormatter.format(percent, 2)} %",
            style = MaterialTheme.typography.labelLarge,
            color = if (percent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PeriodTabRow(selected: ChartPeriod, onSelect: (ChartPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartPeriod.entries.forEach { period ->
            val isSelected = selected == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    )
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatGrid(listing: ListingDto) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Detalji", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        StatRow("Bid", listing.bid?.let { MoneyFormatter.format(it) } ?: "—")
        StatRow("Ask", listing.ask?.let { MoneyFormatter.format(it) } ?: "—")
        StatRow("High", listing.high?.let { MoneyFormatter.format(it) } ?: "—")
        StatRow("Low", listing.low?.let { MoneyFormatter.format(it) } ?: "—")
        StatRow("Volume", listing.volume?.toString() ?: "—")
        listing.dividendYield?.let { StatRow("Dividenda", "${MoneyFormatter.format(it, 2)} %") }
        listing.marketCap?.let { StatRow("Trzisna kapitalizacija", MoneyFormatter.format(it.toDouble())) }
        listing.contractSize?.let { StatRow("Velicina ugovora", "$it ${listing.contractUnit.orEmpty()}") }
        listing.maintenanceMargin?.let { StatRow("Maintenance margin", MoneyFormatter.format(it)) }
        listing.settlementDate?.let { StatRow("Settlement", it) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StrikeRowsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = { onChange(value - 1) },
            enabled = value > STRIKE_FILTER_MIN
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Manje strike redova", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = "±$value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(
            onClick = { onChange(value + 1) },
            enabled = value < STRIKE_FILTER_MAX
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Vise strike redova", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun OptionChainCard(
    chain: OptionChainDto,
    currentPrice: Double,
    rowsAroundPrice: Int = DEFAULT_STRIKE_ROWS,
    onExercise: (Long) -> Unit = {}
) {
    // Spec Celina 3 §467: prikazuju se najblizi `rowsAroundPrice` redovi iznad i ispod
    // currentPrice (Shared Price), sortirano po strike-u rastuce. Ako ima manje od
    // 2*rowsAroundPrice ukupno, prikazuju se svi.
    val sortedEntries = remember(chain.entries) { chain.entries.sortedBy { it.strikePrice } }
    val visibleEntries = remember(sortedEntries, currentPrice, rowsAroundPrice) {
        pickVisibleStrikeEntries(sortedEntries, rowsAroundPrice, { it.strikePrice }, currentPrice)
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Settlement: ${chain.settlementDate ?: "—"}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Call premija", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Strike", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("Put premija", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        visibleEntries.forEach { entry ->
            val isAtPrice = kotlin.math.abs(entry.strikePrice - currentPrice) <= 0.5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.call?.premium?.let { MoneyFormatter.format(it, 2) } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.call?.itm == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAtPrice) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        MoneyFormatter.format(entry.strikePrice, 2),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    entry.put?.premium?.let { MoneyFormatter.format(it, 2) } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.put?.itm == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }
            // Exercise dugmad za ITM opcije — backend odbija ako korisnik nije aktuar/admin.
            val exercisable = listOfNotNull(
                entry.call?.takeIf { it.itm == true },
                entry.put?.takeIf { it.itm == true }
            )
            if (exercisable.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercisable.forEach { option ->
                        rs.raf.banka2.mobile.core.ui.components.SecondaryButton(
                            text = "Iskoristi ${option.type}",
                            onClick = { onExercise(option.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
