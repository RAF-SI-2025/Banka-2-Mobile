package rs.raf.banka2.mobile.feature.funds.details

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.data.dto.account.AccountDto

@Composable
fun FundDetailsScreen(
    onBack: () -> Unit,
    viewModel: FundDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showInvest by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is FundDetailsEvent.Toast) snackbar.showSnackbar(it.message) }
    }

    BankaScaffold(
        title = state.fund?.name ?: "Detalji fonda",
        onBack = onBack,
        snackbarHostState = snackbar,
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
            state.fund?.let { fund ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(fund.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        fund.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            MoneyFormatter.formatWithCurrency(fund.totalValue, fund.currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${if (fund.profit >= 0) "+" else ""}${MoneyFormatter.format(fund.profit, 2)} (${fund.profitPercent?.let { MoneyFormatter.format(it, 2) } ?: "—"} %)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (fund.profit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Likvidna sredstva: ${MoneyFormatter.formatWithCurrency(fund.liquidFunds, fund.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Min ulog: ${MoneyFormatter.formatWithCurrency(fund.minimumContribution, fund.currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        fund.accountNumber?.let {
                            Text("Racun fonda: ${AccountFormatter.formatAccountNumber(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (state.performance.isNotEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Performans", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            FundPerformanceChart(points = state.performance, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                state.myPosition?.let { position ->
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Moja pozicija", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Ukupno ulozeno: ${MoneyFormatter.formatWithCurrency(position.totalInvested, position.currency)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            position.currentValue?.let {
                                Text("Trenutna vrednost: ${MoneyFormatter.formatWithCurrency(it, position.currency)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            position.profit?.let {
                                Text(
                                    "Profit: ${MoneyFormatter.format(it, 2)} (${position.profitPercent?.let { p -> MoneyFormatter.format(p, 2) } ?: "—"} %)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (it >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                if (fund.holdings.isNotEmpty()) {
                    item {
                        Text("Hartije fonda", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    items(fund.holdings, key = { it.listingId ?: it.ticker.orEmpty().hashCode().toLong() }) { holding ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(holding.ticker ?: "—", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(holding.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${holding.quantity} kom", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    holding.totalValue?.let {
                                        Text(MoneyFormatter.format(it, 2), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(text = "Ulozi", onClick = { showInvest = true }, modifier = Modifier.weight(1f))
                        if (state.myPosition != null) {
                            DangerButton(text = "Povuci", onClick = { showWithdraw = true }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showInvest) {
        InvestDialog(
            accounts = state.accounts,
            minContribution = state.fund?.minimumContribution ?: 0.0,
            isLoading = state.submitting,
            onDismiss = { showInvest = false },
            onConfirm = { account, amount ->
                viewModel.invest(account.id, amount)
                showInvest = false
            }
        )
    }
    if (showWithdraw) {
        WithdrawDialog(
            accounts = state.accounts,
            currentInvested = state.myPosition?.currentValue ?: 0.0,
            isLoading = state.submitting,
            onDismiss = { showWithdraw = false },
            onConfirm = { account, amount, all ->
                viewModel.withdraw(account.id, amount, all)
                showWithdraw = false
            }
        )
    }
}

@Composable
private fun InvestDialog(
    accounts: List<AccountDto>,
    minContribution: Double,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AccountDto, Double) -> Unit
) {
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    val parsed = MoneyFormatter.parse(amount)
    val canSubmit = selected != null && parsed != null && parsed >= minContribution && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Ulaganje u fond") },
        text = {
            Column {
                Text(
                    "Min ulog: ${MoneyFormatter.format(minContribution, 2)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AccountPicker(accounts, selected) { selected = it }
                Spacer(Modifier.height(8.dp))
                AppTextField(value = amount, onValueChange = { amount = it }, label = "Iznos", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val src = selected; val a = parsed
                if (src != null && a != null) onConfirm(src, a)
            }, enabled = canSubmit) { Text("Ulozi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

@Composable
private fun WithdrawDialog(
    accounts: List<AccountDto>,
    currentInvested: Double,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AccountDto, Double?, Boolean) -> Unit
) {
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var all by remember { mutableStateOf(false) }
    val parsed = MoneyFormatter.parse(amount)
    val canSubmit = selected != null && (all || (parsed != null && parsed > 0 && parsed <= currentInvested)) && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Povlacenje iz fonda") },
        text = {
            Column {
                Text(
                    "Trenutna vrednost: ${MoneyFormatter.format(currentInvested, 2)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AccountPicker(accounts, selected) { selected = it }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { all = !all }) {
                    Checkbox(checked = all, onCheckedChange = { all = it })
                    Text("Povuci celu poziciju", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                if (!all) {
                    Spacer(Modifier.height(8.dp))
                    AppTextField(value = amount, onValueChange = { amount = it }, label = "Iznos", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dst = selected; val a = if (all) null else parsed
                if (dst != null) onConfirm(dst, a, all)
            }, enabled = canSubmit) { Text("Povuci") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

@Composable
private fun AccountPicker(
    accounts: List<AccountDto>,
    selected: AccountDto?,
    onSelect: (AccountDto) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts, key = { it.id }) { acc ->
            val isSelected = selected?.id == acc.id
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(acc) }
                    .padding(10.dp)
                    .width(180.dp)
            ) {
                Text(acc.name?.takeIf { it.isNotBlank() } ?: acc.accountType ?: "Racun", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(AccountFormatter.formatAccountNumber(acc.accountNumber), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
