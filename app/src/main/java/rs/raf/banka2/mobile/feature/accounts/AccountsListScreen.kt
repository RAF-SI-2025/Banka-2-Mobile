package rs.raf.banka2.mobile.feature.accounts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.ShimmerLine
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600

@Composable
fun AccountsListScreen(
    onBack: () -> Unit,
    onAccountClick: (Long, Boolean) -> Unit,
    onNewRequest: () -> Unit,
    viewModel: AccountsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Moji racuni",
        onBack = onBack,
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
            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }
            if (state.loading) {
                items(3) { _ ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.7f), height = 16.dp)
                        Spacer(Modifier.height(8.dp))
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.4f), height = 12.dp)
                    }
                }
            } else if (state.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Nema otvorenih racuna",
                        description = "Posalji zahtev za otvaranje novog racuna i banka ce ga pregledati."
                    )
                }
            } else {
                items(state.accounts, key = { it.id }) { acc ->
                    AccountCard(
                        account = acc,
                        onClick = { onAccountClick(acc.id, acc.isBusiness) }
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                rs.raf.banka2.mobile.core.ui.components.SecondaryButton(
                    text = "Posalji zahtev za novi racun",
                    onClick = onNewRequest,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: rs.raf.banka2.mobile.data.dto.account.AccountDto,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Indigo500, Violet600))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name?.takeIf { it.isNotBlank() } ?: account.accountType ?: "Racun",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AccountFormatter.formatAccountNumber(account.accountNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.isBusiness) {
                    Text(
                        text = "Poslovni",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.formatWithCurrency(account.availableBalance, account.currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Stanje: ${MoneyFormatter.formatWithCurrency(account.balance, account.currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
