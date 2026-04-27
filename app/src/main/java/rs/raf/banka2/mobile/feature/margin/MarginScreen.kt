package rs.raf.banka2.mobile.feature.margin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto

@Composable
fun MarginScreen(
    onBack: () -> Unit,
    viewModel: MarginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var depositTarget by remember { mutableStateOf<MarginAccountDto?>(null) }
    var withdrawTarget by remember { mutableStateOf<MarginAccountDto?>(null) }

    BankaScaffold(
        title = "Marzni racuni",
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
            if (state.error != null) item { ErrorBanner(state.error) }
            if (state.accounts.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Nemas marzni racun",
                        description = "Marzni racun ti omogucava da trgujes hartijama uz kredit od banke."
                    )
                }
            }
            items(state.accounts, key = { it.id }) { acc ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Marzni racun #${acc.id}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pocetna marza", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.formatWithCurrency(acc.initialMargin, acc.currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Odrzavanje", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.formatWithCurrency(acc.maintenanceMargin, acc.currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Dugovanje: ${MoneyFormatter.formatWithCurrency(acc.loanValue, acc.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (acc.active) "Aktivan" else "Blokiran (premali initial margin)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (acc.active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = "Uplati",
                            onClick = { depositTarget = acc },
                            leadingIcon = Icons.Filled.AddCircle,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Povuci",
                            onClick = { withdrawTarget = acc },
                            leadingIcon = Icons.Filled.RemoveCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    depositTarget?.let { target ->
        AmountDialog(
            title = "Uplata na marzni racun",
            currency = target.currency,
            onDismiss = { depositTarget = null },
            onConfirm = {
                viewModel.deposit(target.id, it)
                depositTarget = null
            }
        )
    }
    withdrawTarget?.let { target ->
        AmountDialog(
            title = "Povlacenje sa marznog racuna",
            currency = target.currency,
            onDismiss = { withdrawTarget = null },
            onConfirm = {
                viewModel.withdraw(target.id, it)
                withdrawTarget = null
            }
        )
    }
}

@Composable
private fun AmountDialog(
    title: String,
    currency: String?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val parsed = MoneyFormatter.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = "Iznos ${currency.orEmpty()}",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null && parsed > 0) {
                Text("Potvrdi")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}
