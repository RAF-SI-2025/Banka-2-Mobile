package rs.raf.banka2.mobile.feature.profitbank

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.account.AccountDto

@Composable
fun ProfitBankScreen(
    onBack: () -> Unit,
    viewModel: ProfitBankViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is ProfitBankEvent.Toast) snackbar.showSnackbar(it.message) }
    }

    BankaScaffold(
        title = "Profit Banke",
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.error != null) item { ErrorBanner(state.error) }
            item {
                Text("Performans aktuara (RSD)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            items(state.actuaries, key = { it.employeeId }) { actuary ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(actuary.displayName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(actuary.position ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            MoneyFormatter.format(actuary.realizedProfitRsd, 2),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (actuary.realizedProfitRsd >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Pozicije banke u fondovima", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            items(state.fundPositions, key = { it.fundId }) { position ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(position.fundName ?: "Fond #${position.fundId}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Manager: ${position.managerName ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            position.sharePercent?.let {
                                Text("Udeo: ${MoneyFormatter.format(it, 2)} %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            position.shareAmountRsd?.let {
                                Text(MoneyFormatter.format(it, 2), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            position.profitRsd?.let {
                                Text(
                                    "${if (it >= 0) "+" else ""}${MoneyFormatter.format(it, 2)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (it >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "Uplata",
                            onClick = { viewModel.openInvestDialog(position) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Povlacenje",
                            onClick = { viewModel.openWithdrawDialog(position) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    state.investTarget?.let { target ->
        InvestDialog(
            fundName = target.fundName ?: "Fond #${target.fundId}",
            accounts = state.accounts,
            isSubmitting = state.submitting,
            onDismiss = viewModel::closeDialogs,
            onConfirm = { account, amount -> viewModel.invest(target.fundId, account.id, amount) }
        )
    }
    state.withdrawTarget?.let { target ->
        WithdrawDialog(
            fundName = target.fundName ?: "Fond #${target.fundId}",
            accounts = state.accounts,
            currentShareRsd = target.shareAmountRsd ?: 0.0,
            isSubmitting = state.submitting,
            onDismiss = viewModel::closeDialogs,
            onConfirm = { account, amount, all ->
                viewModel.withdraw(target.fundId, account.id, amount, all)
            }
        )
    }
}

@Composable
private fun InvestDialog(
    fundName: String,
    accounts: List<AccountDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AccountDto, Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    val parsedAmount = MoneyFormatter.parse(amountText)
    val canSubmit = !isSubmitting && selected != null && parsedAmount != null && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uplata u $fundName") },
        text = {
            Column {
                Text(
                    "Bira se bankin racun sa kog se sredstva prebacuju na racun fonda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AccountChips(accounts = accounts, selected = selected, onSelect = { selected = it })
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Iznos",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val acc = selected ?: return@TextButton
                    val amt = parsedAmount ?: return@TextButton
                    onConfirm(acc, amt)
                },
                enabled = canSubmit
            ) { Text("Potvrdi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

@Composable
private fun WithdrawDialog(
    fundName: String,
    accounts: List<AccountDto>,
    currentShareRsd: Double,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AccountDto, Double?, Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var withdrawAll by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    val parsedAmount = MoneyFormatter.parse(amountText)
    val canSubmit = !isSubmitting && selected != null &&
        (withdrawAll || (parsedAmount != null && parsedAmount > 0.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Povlacenje iz $fundName") },
        text = {
            Column {
                Text(
                    "Bira se bankin racun na koji ce sredstva biti uplacena. Trenutni udeo banke: ${MoneyFormatter.format(currentShareRsd, 2)} RSD.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AccountChips(accounts = accounts, selected = selected, onSelect = { selected = it })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withdrawAll, onCheckedChange = { withdrawAll = it })
                    Spacer(Modifier.height(4.dp))
                    Text("Povuci celu poziciju", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                if (!withdrawAll) {
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "Iznos (RSD)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val acc = selected ?: return@TextButton
                    onConfirm(acc, parsedAmount, withdrawAll)
                },
                enabled = canSubmit
            ) { Text("Potvrdi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

@Composable
private fun AccountChips(
    accounts: List<AccountDto>,
    selected: AccountDto?,
    onSelect: (AccountDto) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(accounts, key = { it.id }) { acc ->
            val isSelected = selected?.id == acc.id
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(acc) }
                    .padding(10.dp)
            ) {
                Text(
                    AccountFormatter.formatAccountNumber(acc.accountNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
