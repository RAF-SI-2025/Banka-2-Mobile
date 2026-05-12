package rs.raf.banka2.mobile.feature.savings.details

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.core.ui.components.VerificationModal
import rs.raf.banka2.mobile.data.dto.savings.SavingsConstants
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsTransactionDto

@Composable
fun SavingsDetailsScreen(
    onBack: () -> Unit,
    viewModel: SavingsDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is SavingsDetailsEvent.Toast) snackbar.showSnackbar(event.message)
        }
    }

    BankaScaffold(
        title = "Detalji depozita",
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
            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }

            state.deposit?.let { deposit ->
                item { DepositHeaderCard(deposit) }
                item { DepositInfoCard(deposit) }

                // Auto-renew toggle
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Auto-obnova",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Depozit se automatski obnavlja po dospeci.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = deposit.autoRenew,
                                onCheckedChange = { viewModel.toggleAutoRenew(it) },
                                enabled = !state.autoRenewSaving && deposit.status == "ACTIVE"
                            )
                        }
                    }
                }

                // Early withdrawal button — only when active
                if (deposit.status == "ACTIVE") {
                    item {
                        SecondaryButton(
                            text = "Raskini depozit ranije",
                            onClick = viewModel::openWithdrawDialog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Transaction history
                if (state.transactions.isNotEmpty()) {
                    item {
                        Text(
                            "Istorija transakcija",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRow(tx)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Withdraw early confirmation dialog
    if (state.showWithdraw) {
        var otpVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = viewModel::dismissWithdrawDialog,
            title = { Text("Raskid pre roka") },
            text = {
                Text(
                    "Ova akcija ce raskinuti depozit pre dospeca. Primenjuje se penal od 1% na glavnicu. " +
                        "Sredstva ce biti vracena na vezani racun. Zelite li da nastavite?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { otpVisible = true }) {
                    Text("Nastavi", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWithdrawDialog) {
                    Text("Otkazi")
                }
            }
        )

        if (otpVisible) {
            VerificationModal(
                visible = true,
                onDismiss = {
                    otpVisible = false
                    viewModel.dismissWithdrawDialog()
                },
                onSubmit = { code ->
                    otpVisible = false
                    viewModel.confirmWithdraw(code)
                },
                isVerifying = state.withdrawing
            )
        }
    }
}

@Composable
private fun DepositHeaderCard(deposit: SavingsDepositDto) {
    val statusLabel = SavingsConstants.STATUS_LABEL_SR[deposit.status] ?: deposit.status
    val statusColor = when (deposit.status) {
        "ACTIVE" -> MaterialTheme.colorScheme.tertiary
        "WITHDRAWN_EARLY" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Depozit #${deposit.id}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${deposit.principalAmount.toPlainString()} ${deposit.currencyCode}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Kamata isplacena: ${deposit.totalInterestPaid.toPlainString()} ${deposit.currencyCode}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DepositInfoCard(deposit: SavingsDepositDto) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow("Rok", "${deposit.termMonths} meseci")
        InfoRow("G. kamatna stopa", "${deposit.annualInterestRate.toPlainString()} %")
        InfoRow("Datum pocetka", DateFormatter.formatDate(deposit.startDate))
        InfoRow("Datum dospeca", DateFormatter.formatDate(deposit.maturityDate))
        InfoRow(
            "Sledeca isplata kamate",
            DateFormatter.formatDate(deposit.nextInterestPaymentDate)
        )
        InfoRow(
            "Auto-obnova",
            if (deposit.autoRenew) "Ukljucena" else "Iskljucena"
        )
        deposit.linkedAccountNumber?.let {
            InfoRow("Vezani racun", it)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TransactionRow(tx: SavingsTransactionDto) {
    val typeLabel = SavingsConstants.TRANSACTION_TYPE_LABEL_SR[tx.type] ?: tx.type
    val amountColor = when {
        tx.type.contains("PENALTY", true) || tx.type.contains("EARLY_WITHDRAWAL", true) ->
            MaterialTheme.colorScheme.error
        tx.type.contains("INTEREST", true) || tx.type.contains("PRINCIPAL_RETURN", true) ->
            MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    DateFormatter.formatDate(tx.processedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                tx.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${tx.amount.toPlainString()} ${tx.currencyCode}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}
