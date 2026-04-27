package rs.raf.banka2.mobile.feature.exchange

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.CurrencyVisuals
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard

@Composable
fun ExchangeScreen(
    onBack: () -> Unit,
    viewModel: ExchangeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Menjacnica",
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
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kalkulator konverzije",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = state.amount,
                        onValueChange = viewModel::setAmount,
                        label = "Iznos",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = state.fromCurrency,
                            onValueChange = viewModel::setFrom,
                            label = "Iz valute",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = viewModel::swap) {
                            Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        AppTextField(
                            value = state.toCurrency,
                            onValueChange = viewModel::setTo,
                            label = "U valutu",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.calculation?.let { calc ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "${MoneyFormatter.formatWithCurrency(calc.amount, calc.fromCurrency)} ≈ ${MoneyFormatter.formatWithCurrency(calc.convertedAmount, calc.toCurrency)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        (calc.rate ?: calc.exchangeRate)?.let { rate ->
                            Text(
                                "Kurs: ${MoneyFormatter.format(rate, fractionDigits = 4)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ErrorBanner(state.error)
                }
            }
            item {
                Text(
                    "Kursna lista",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(state.rates, key = { (it.currency ?: it.fromCurrency).orEmpty() + (it.toCurrency.orEmpty()) }) { rate ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(CurrencyVisuals.flag(rate.currency ?: rate.fromCurrency), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rate.currency ?: rate.fromCurrency.orEmpty(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Srednji: ${MoneyFormatter.format(rate.middleRate ?: rate.rate ?: 0.0, 4)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Kupovni: ${MoneyFormatter.format(rate.buyRate ?: 0.0, 4)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Prodajni: ${MoneyFormatter.format(rate.sellRate ?: 0.0, 4)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
