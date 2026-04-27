package rs.raf.banka2.mobile.feature.funds.discovery

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun FundsDiscoveryScreen(
    onBack: () -> Unit,
    onFundClick: (Long) -> Unit,
    onCreateFund: () -> Unit,
    onMyFunds: () -> Unit,
    viewModel: FundsDiscoveryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Investicioni fondovi",
        onBack = onBack,
        actions = {
            if (state.myPositions.isNotEmpty()) {
                IconButton(onClick = onMyFunds) {
                    Icon(Icons.Filled.WorkOutline, contentDescription = "Moji fondovi", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (state.canCreateFund) {
                IconButton(onClick = onCreateFund) {
                    Icon(Icons.Filled.Add, contentDescription = "Kreiraj fond", tint = MaterialTheme.colorScheme.primary)
                }
            }
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
            AppTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                label = "Pretraga po nazivu",
                leadingIcon = Icons.Outlined.Search,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SortRow(state.sortField, state.sortAscending, viewModel::setSort)
            Spacer(Modifier.height(8.dp))
            ErrorBanner(state.error)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.funds.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.WorkOutline,
                            title = "Nema fondova",
                            description = "Probaj druga pretragu ili sacekaj da supervizor kreira novi fond."
                        )
                    }
                }
                items(state.funds, key = { it.id }) { fund ->
                    GlassCard(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFundClick(fund.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fund.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                fund.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "Min ulog: ${MoneyFormatter.formatWithCurrency(fund.minimumContribution, fund.currency)} · Manager: ${fund.managerName ?: "—"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    MoneyFormatter.formatWithCurrency(fund.totalValue, fund.currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${if (fund.profit >= 0) "+" else ""}${MoneyFormatter.format(fund.profit, 2)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (fund.profit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(field: FundSortField, ascending: Boolean, onSelect: (FundSortField) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FundSortField.entries.toList()) { sort ->
            val isSelected = field == sort
            val arrow = if (isSelected) (if (ascending) " ↑" else " ↓") else ""
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(sort) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${sort.label}$arrow",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
