package rs.raf.banka2.mobile.feature.savings.list

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import rs.raf.banka2.mobile.data.dto.savings.SavingsConstants
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto

@Composable
fun SavingsListScreen(
    onBack: () -> Unit,
    onNewDeposit: () -> Unit,
    onDepositClick: (Long) -> Unit,
    viewModel: SavingsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Stednja",
        onBack = onBack,
        actions = {
            IconButton(onClick = onNewDeposit) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Novi oroceni depozit",
                    tint = MaterialTheme.colorScheme.primary
                )
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
            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }

            if (state.deposits.isNotEmpty()) {
                item {
                    SavingsSummaryCard(state)
                }
                item {
                    Text(
                        "Moji depoziti",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(state.deposits, key = { it.id }) { deposit ->
                    SavingsDepositCard(
                        deposit = deposit,
                        onClick = { onDepositClick(deposit.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            } else if (!state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Nema depozita",
                        description = "Orocite sredstva i zaradi kamatu. Pritisnite + za novi depozit."
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsSummaryCard(state: SavingsListState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Pregled stednje",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Aktivnih depozita",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.activeCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    "Kamata ukupno",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.totalInterestPaid.toPlainString()} RSD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SavingsDepositCard(deposit: SavingsDepositDto, onClick: () -> Unit) {
    val statusLabel = SavingsConstants.STATUS_LABEL_SR[deposit.status] ?: deposit.status
    val statusColor = when (deposit.status) {
        "ACTIVE" -> MaterialTheme.colorScheme.tertiary
        "WITHDRAWN_EARLY" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Depozit #${deposit.id}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${deposit.termMonths} mes · ${deposit.annualInterestRate.toPlainString()}% g.k.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Dospece: ${DateFormatter.formatDate(deposit.maturityDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${deposit.principalAmount.toPlainString()} ${deposit.currencyCode}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
        if (deposit.autoRenew) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Auto-obnova: uklj.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
