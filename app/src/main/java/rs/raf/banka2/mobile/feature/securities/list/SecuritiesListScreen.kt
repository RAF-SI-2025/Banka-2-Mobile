package rs.raf.banka2.mobile.feature.securities.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.listing.ListingDto

@Composable
fun SecuritiesListScreen(
    onBack: () -> Unit,
    onListingClick: (Long) -> Unit,
    viewModel: SecuritiesListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    BankaScaffold(
        title = "Berza",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showFilters = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filteri", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { viewModel.refresh() }) {
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
            HeaderBanner(state = state)
            Spacer(Modifier.height(12.dp))
            TypeTabRow(state, viewModel::setType)
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                label = "Pretraga (ticker / naziv)",
                leadingIcon = Icons.Outlined.Search,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            ErrorBanner(state.error)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.listings.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = "Nema rezultata",
                            description = "Probaj druga filtre ili obrise pretrage."
                        )
                    }
                }
                itemsIndexed(state.listings, key = { _, l -> l.id }) { index, listing ->
                    ListingRow(listing = listing, index = index, onClick = { onListingClick(listing.id) })
                }
            }
        }
    }

    if (showFilters) {
        FiltersDialog(
            initialExchange = state.exchangePrefix,
            initialMin = state.priceMin,
            initialMax = state.priceMax,
            onDismiss = { showFilters = false },
            onApply = { exchange, min, max ->
                viewModel.setExchangePrefix(exchange)
                viewModel.setPriceRange(min, max)
                showFilters = false
            }
        )
    }
}

@Composable
private fun HeaderBanner(state: SecuritiesListState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(testMode = state.anyTestMode)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (state.anyTestMode) "SIMULIRANI PODACI" else "LIVE TRZISTE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${state.listings.size} hartija",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            val gainers = state.listings.count { (it.changePercent ?: 0.0) > 0 }
            val losers = state.listings.count { (it.changePercent ?: 0.0) < 0 }
            Text(
                text = "Rast: $gainers · Pad: $losers · Bez promene: ${state.listings.size - gainers - losers}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun LiveBadge(testMode: Boolean) {
    val infinite = rememberInfiniteTransition(label = "live-badge")
    val alpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "live-alpha"
    )
    val color = if (testMode) Color(0xFFF59E0B) else Color(0xFF10B981)
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun TypeTabRow(state: SecuritiesListState, onSelect: (ListingTypeFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        state.availableTypes.forEach { type ->
            val isSelected = state.type == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected)
                            Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF7C3AED)))
                        else
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ListingRow(listing: ListingDto, index: Int, onClick: () -> Unit) {
    var visible by remember(listing.id) { mutableStateOf(false) }
    LaunchedEffect(listing.id) { visible = true }

    val change = listing.changePercent ?: 0.0
    val accent = when {
        change > 0 -> Color(0xFF10B981)
        change < 0 -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = (index * 35).coerceAtMost(420))) +
            slideInVertically(animationSpec = tween(320, delayMillis = (index * 35).coerceAtMost(420))) { it / 4 }
    ) {
        GlassCard(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF7C3AED)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = listing.ticker.take(3),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(listing.ticker, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(listing.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                listing.exchangeAcronym ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            listing.listingType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        MoneyFormatter.formatWithCurrency(listing.price, listing.currency),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (listing.changePercent != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (change >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (change >= 0) "+" else ""}${MoneyFormatter.format(change, 2)} %",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersDialog(
    initialExchange: String,
    initialMin: String,
    initialMax: String,
    onDismiss: () -> Unit,
    onApply: (String, String, String) -> Unit
) {
    var exchange by remember { mutableStateOf(initialExchange) }
    var min by remember { mutableStateOf(initialMin) }
    var max by remember { mutableStateOf(initialMax) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filteri") },
        text = {
            Column {
                AppTextField(
                    value = exchange,
                    onValueChange = { exchange = it },
                    label = "Berza (NYSE / NASDAQ / ...)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(
                        value = min,
                        onValueChange = { min = it },
                        label = "Cena od",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    AppTextField(
                        value = max,
                        onValueChange = { max = it },
                        label = "Cena do",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(exchange.trim(), min, max) }) {
                Text("Primeni")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}
