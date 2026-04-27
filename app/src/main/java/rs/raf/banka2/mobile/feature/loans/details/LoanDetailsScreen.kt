package rs.raf.banka2.mobile.feature.loans.details

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.loan.LoanDto

@Composable
fun LoanDetailsScreen(
    onBack: () -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is LoanDetailsEvent.Toast) snackbar.showSnackbar(it.message)
        }
    }

    BankaScaffold(
        title = "Detalji kredita",
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
            state.loan?.let { loan ->
                item { LoanHeader(loan) }
                item { LoanInfoCard(loan) }
                item {
                    SecondaryButton(
                        text = if (state.submitting) "Saljem..." else "Prevremena otplata",
                        onClick = viewModel::earlyRepay,
                        loading = state.submitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.installments.isNotEmpty()) {
                    item {
                        Text("Plan otplate", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    items(state.installments, key = { it.id }) { instalment ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(DateFormatter.formatDate(instalment.dueDate), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    instalment.paidDate?.let {
                                        Text("Placeno: ${DateFormatter.formatDate(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(MoneyFormatter.format(instalment.amount, 2), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        instalment.status ?: "—",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (instalment.status.equals("PAID", true)) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanHeader(loan: LoanDto) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("${loan.loanType ?: "Kredit"} #${loan.id}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(loan.status ?: "—", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(MoneyFormatter.format(loan.amount, 2), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Saldo: ${MoneyFormatter.format(loan.balance ?: 0.0, 2)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoanInfoCard(loan: LoanDto) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow("Broj kredita", "#${loan.id}")
        InfoRow("Vrsta", loan.loanType ?: "—")
        loan.rateType?.let { InfoRow("Tip kamate", it) }
        InfoRow("Valuta", loan.currency ?: "—")
        InfoRow("Racun za otplatu", AccountFormatter.formatAccountNumber(loan.accountNumber))
        InfoRow("Rata", MoneyFormatter.format(loan.monthlyInstallment ?: 0.0, 2))
        InfoRow("Sledeca rata", DateFormatter.formatDate(loan.nextInstallmentDate))
        loan.maturityDate?.let { InfoRow("Dospece kredita", DateFormatter.formatDate(it)) }
        loan.interestRate?.let { InfoRow("Nominalna kamata", "${MoneyFormatter.format(it, 2)} %") }
        loan.effectiveRate?.let { InfoRow("Efektivna kamata", "${MoneyFormatter.format(it, 2)} %") }
        loan.durationMonths?.let { InfoRow("Trajanje", "$it meseci") }
        InfoRow("Datum ugovaranja", DateFormatter.formatDate(loan.createdAt))
        loan.purpose?.let { InfoRow("Svrha", it) }
        loan.employer?.let { InfoRow("Poslodavac", it) }
        loan.employmentStatus?.let { InfoRow("Status zaposlenja", it) }
        loan.employmentMonths?.let { InfoRow("Period zaposlenja", "$it meseci") }
        loan.monthlyIncome?.let { InfoRow("Mesecna primanja", MoneyFormatter.format(it, 2)) }
        loan.phone?.let { InfoRow("Kontakt telefon", it) }
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
