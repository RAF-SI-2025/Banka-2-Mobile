package rs.raf.banka2.mobile.feature.transfers.create

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import rs.raf.banka2.mobile.core.ui.components.VerificationModal
import rs.raf.banka2.mobile.data.dto.account.AccountDto

@Composable
fun NewTransferScreen(
    onBack: () -> Unit,
    onSuccess: (Long) -> Unit,
    viewModel: NewTransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is NewTransferEvent.Success) onSuccess(event.transferId)
        }
    }

    BankaScaffold(
        title = "Novi prenos",
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
            AccountPickerRow(
                label = "Sa racuna",
                accounts = state.accounts,
                selected = state.fromAccount,
                onSelect = viewModel::setSource
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = viewModel::swap) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Zameni", tint = MaterialTheme.colorScheme.primary)
                }
            }
            AccountPickerRow(
                label = "Na racun",
                accounts = state.accounts.filter { it.id != state.fromAccount?.id },
                selected = state.toAccount,
                onSelect = viewModel::setDestination
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Iznos i napomena",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = "Iznos ${state.fromAccount?.currency.orEmpty()}",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    label = "Napomena (opciono)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isFx && state.estimatedConverted != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Procenjena vrednost u ${state.toAccount?.currency.orEmpty()}: " +
                            MoneyFormatter.formatWithCurrency(state.estimatedConverted ?: 0.0, state.toAccount?.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    state.exchangeRate?.let { rate ->
                        Text(
                            "Kurs: ${MoneyFormatter.format(rate, fractionDigits = 4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            ErrorBanner(state.error)
            PrimaryButton(
                text = "Potvrdi prenos",
                onClick = viewModel::openVerification,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    VerificationModal(
        visible = state.showVerification,
        onDismiss = viewModel::closeVerification,
        isVerifying = state.submitting,
        externalError = state.error,
        onSubmit = { viewModel.submitWithCode(it) }
    )
}

@Composable
private fun AccountPickerRow(
    label: String,
    accounts: List<AccountDto>,
    selected: AccountDto?,
    onSelect: (AccountDto) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        if (accounts.isEmpty()) {
            Text(
                "Nema dostupnih racuna.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@GlassCard
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.id }) { acc ->
                val isSelected = selected?.id == acc.id
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(acc) }
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
}
