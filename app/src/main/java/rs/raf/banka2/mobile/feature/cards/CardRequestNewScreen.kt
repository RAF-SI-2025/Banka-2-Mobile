package rs.raf.banka2.mobile.feature.cards

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun CardRequestNewScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: CardRequestNewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is CardRequestEvent.Submitted) onSubmitted() }
    }

    BankaScaffold(
        title = "Zahtev za karticu",
        onBack = onBack,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Uz koji racun?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.accounts, key = { it.id }) { acc ->
                        val isSelected = state.account?.id == acc.id
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { viewModel.setAccount(acc) }
                                .padding(12.dp)
                                .width(220.dp)
                        ) {
                            Text(
                                acc.name?.takeIf { it.isNotBlank() } ?: acc.accountType ?: "Racun",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                AccountFormatter.formatAccountNumber(acc.accountNumber),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Detalji kartice",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.cardType,
                    onValueChange = viewModel::setType,
                    label = "Brend (VISA/MASTERCARD/DINACARD/AMERICAN_EXPRESS)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // ME-03: kategorija placanja (ortogonalno na brend) — DEBIT/CREDIT/INTERNET_PREPAID
                Text(
                    "Kategorija placanja",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DEBIT" to "Debit", "CREDIT" to "Credit", "INTERNET_PREPAID" to "Prepaid").forEach { (value, label) ->
                        val selected = state.cardCategory == value
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { viewModel.setCategory(value) }
                                .padding(10.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.limit,
                    onValueChange = viewModel::setLimit,
                    label = "Mesecni limit potrosnje",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                // ME-03: za CREDIT karticu dodatno trazimo kreditni limit (banka unapred odobrava).
                if (state.cardCategory == "CREDIT") {
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.creditLimit,
                        onValueChange = viewModel::setCreditLimit,
                        label = "Kreditni limit (odobrava banka)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            ErrorBanner(state.error)
            if (state.awaitingConfirmation) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Banka je poslala 6-cifreni kod na tvoj email. Unesi ga da bi kartica bila izdata.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.confirmCode,
                        onValueChange = viewModel::setConfirmCode,
                        label = "Email kod (6 cifara)",
                        keyboardType = KeyboardType.NumberPassword,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PrimaryButton(
                    text = "Potvrdi i izdaj karticu",
                    onClick = viewModel::confirm,
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryButton(
                    text = "Posalji zahtev",
                    onClick = viewModel::submit,
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
