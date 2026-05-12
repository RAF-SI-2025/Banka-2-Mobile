package rs.raf.banka2.mobile.feature.savings.newdeposit

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.VerificationModal
import rs.raf.banka2.mobile.data.dto.savings.SavingsConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsNewDepositScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SavingsNewDepositViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) onSuccess()
    }

    BankaScaffold(
        title = "Novi depozit",
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
            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }

            // Source account picker
            item {
                AccountDropdown(
                    label = "Izvorni racun (za uplatu)",
                    accounts = state.accounts,
                    selectedId = state.sourceAccountId,
                    onSelected = viewModel::setSource
                )
            }

            // Linked account picker
            item {
                AccountDropdown(
                    label = "Vezani racun (za isplatu kamate)",
                    accounts = state.accounts,
                    selectedId = state.linkedAccountId,
                    onSelected = viewModel::setLinked
                )
            }

            // Principal amount
            item {
                OutlinedTextField(
                    value = if (state.principalAmount == java.math.BigDecimal.ZERO) "" else state.principalAmount.toPlainString(),
                    onValueChange = viewModel::setPrincipalText,
                    label = { Text("Iznos (${state.currencyCode})") },
                    placeholder = { Text("Minimalno: ${state.minAmount.toPlainString()}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }

            // Term selection
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Rok orocavanja",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    SavingsConstants.TERM_OPTIONS.forEach { months ->
                        val rate = state.rates.firstOrNull { it.termMonths == months }
                        val rateLabel = rate?.let { " · ${it.annualRate.toPlainString()}% g.k." } ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.termMonths == months,
                                    onClick = { viewModel.setTerm(months) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.termMonths == months,
                                onClick = { viewModel.setTerm(months) }
                            )
                            Text(
                                "$months meseci$rateLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

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
                                "Depozit se automatski obnavlja po isteku roka.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoRenew,
                            onCheckedChange = viewModel::setAutoRenew
                        )
                    }
                }
            }

            // Interest calculator
            if (state.principalAmount > java.math.BigDecimal.ZERO && state.annualRate > java.math.BigDecimal.ZERO) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Kalkulator kamate",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        CalcRow("Godisnja kamatna stopa", "${state.annualRate.toPlainString()} %")
                        CalcRow(
                            "Mesecna kamata",
                            "${state.monthlyInterest.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()} ${state.currencyCode}"
                        )
                        CalcRow(
                            "Ukupna kamata (${state.termMonths} mes.)",
                            "${state.totalInterest.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()} ${state.currencyCode}"
                        )
                        CalcRow(
                            "Penal ranijeg raskida (1%)",
                            "${state.penalty.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()} ${state.currencyCode}"
                        )
                    }
                }
            }

            // Submit button
            item {
                PrimaryButton(
                    text = if (state.submitting) "Slanje..." else "Oroci sredstva",
                    onClick = viewModel::openOtpDialog,
                    enabled = !state.submitting,
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    VerificationModal(
        visible = state.showOtp,
        onDismiss = viewModel::dismissOtp,
        onSubmit = viewModel::submit,
        isVerifying = state.submitting
    )
}

@Composable
private fun CalcRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<rs.raf.banka2.mobile.data.dto.account.AccountDto>,
    selectedId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedId }
    val displayText = selected?.let {
        "${AccountFormatter.formatAccountNumber(it.accountNumber)} (${it.currency ?: ""})"
    } ?: "Odaberi racun"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                AccountFormatter.formatAccountNumber(acc.accountNumber),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${acc.currency ?: ""} · saldo: ${acc.availableBalance}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelected(acc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
