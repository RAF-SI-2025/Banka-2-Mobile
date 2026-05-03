package rs.raf.banka2.mobile.feature.orders.create

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import rs.raf.banka2.mobile.core.ui.components.VerificationModal
import rs.raf.banka2.mobile.data.dto.account.AccountDto

@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onSuccess: (Long) -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CreateOrderEvent.Success) onSuccess(event.orderId)
        }
    }

    BankaScaffold(
        title = "Novi nalog",
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
            state.listing?.let { listing ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(listing.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${listing.ticker} · ${listing.listingType} · ${listing.exchangeAcronym ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        MoneyFormatter.formatWithCurrency(listing.price, listing.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            DirectionRow(state.direction, viewModel::setDirection)
            OrderTypeRow(state.orderType, viewModel::setOrderType)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = state.quantity,
                    onValueChange = viewModel::setQuantity,
                    label = "Kolicina",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.orderType in listOf(OrderType.Limit, OrderType.StopLimit)) {
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.limitPrice,
                        onValueChange = viewModel::setLimitPrice,
                        label = "Limit cena",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.orderType in listOf(OrderType.Stop, OrderType.StopLimit)) {
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.stopPrice,
                        onValueChange = viewModel::setStopPrice,
                        label = "Stop cena",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAllOrNone(!state.allOrNone) }
                ) {
                    Checkbox(checked = state.allOrNone, onCheckedChange = viewModel::setAllOrNone)
                    Text(
                        "All-or-None (izvrsi celu kolicinu odjednom)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setUseMargin(!state.useMargin) }
                ) {
                    Checkbox(checked = state.useMargin, onCheckedChange = viewModel::setUseMargin)
                    Column {
                        Text("Marzni nalog (margin trading)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Banka pokriva deo vrednosti — Initial Margin Cost = MaintenanceMargin * 1.1.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (state.showAfterHoursWarning) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "⏰",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color(0xFFEAB308)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "After-hours rezim",
                                style = MaterialTheme.typography.titleSmall,
                                color = androidx.compose.ui.graphics.Color(0xFFEAB308),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Berza ${state.listing?.exchangeAcronym ?: ""} je trenutno zatvorena. Tvoj nalog ce ici u " +
                                    "after-hours rezim — svaki fill kasni dodatnih 30 minuta dok se berza ne otvori.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            state.exchangeNextOpen?.let { next ->
                                Text(
                                    "Sledece otvaranje: $next",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            if (state.canPickFund && state.funds.isNotEmpty()) {
                FundPicker(
                    funds = state.funds,
                    selected = state.selectedFund,
                    onSelect = viewModel::selectFund
                )
            }
            if (state.accounts.isNotEmpty()) {
                AccountPicker(state.accounts, state.selectedAccount, viewModel::selectAccount)
            }
            state.estimatedTotal?.let { total ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Procenjena vrednost", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        MoneyFormatter.formatWithCurrency(total, state.listing?.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!state.isEmployee) {
                        Text(
                            "Provizija ce biti obracunata po pravilima banke (Market 14% / Limit 24% u valuti listinga). " +
                                "Kada je valuta racuna razlicita, dodatno se primenjuje 1% FX marza.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            ErrorBanner(state.error)
            PrimaryButton(
                text = "Potvrdi nalog",
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
private fun DirectionRow(selected: OrderDirection, onSelect: (OrderDirection) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OrderDirection.entries.forEach { dir ->
            val isSelected = selected == dir
            val activeColor = if (dir == OrderDirection.Buy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) activeColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                    .clickable { onSelect(dir) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dir.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun OrderTypeRow(selected: OrderType, onSelect: (OrderType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OrderType.entries.forEach { type ->
            val isSelected = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(type.label, style = MaterialTheme.typography.labelMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AccountPicker(
    accounts: List<AccountDto>,
    selected: AccountDto?,
    onSelect: (AccountDto) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Racun za podmirenje", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
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
                        .padding(12.dp)
                        .width(220.dp)
                ) {
                    Text(acc.name?.takeIf { it.isNotBlank() } ?: acc.accountType ?: "Racun", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(AccountFormatter.formatAccountNumber(acc.accountNumber), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Spec Celina 4 (Nova) §3905-3925: supervizor pri kupovini hartije bira da li
 * kupuje za banku ili za jedan od fondova kojima upravlja. UI prikazuje
 * "Banka" chip + chip-ove za sve fondove sa imenom + likvidnoscu, da
 * supervizor vizuelno vidi koliko ima dostupnog novca u svakom fondu.
 */
@Composable
private fun FundPicker(
    funds: List<rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto>,
    selected: rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto?,
    onSelect: (rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto?) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Kupujem u ime", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Bira se da li hartija ide na bankin racun ili u fond kojim upravljas. Ako biras fond, BE proverava da li racun fonda ima dovoljno novca.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item("bank") {
                val isBank = selected == null
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isBank) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(null) }
                        .padding(12.dp)
                        .width(160.dp)
                ) {
                    Text(
                        "Banka",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Hartija ide na bankin racun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(funds, key = { it.id }) { fund ->
                val isSelected = selected?.id == fund.id
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(fund) }
                        .padding(12.dp)
                        .width(220.dp)
                ) {
                    Text(
                        fund.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Fond #${fund.id} · ${fund.currency ?: "RSD"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Vrednost: ${MoneyFormatter.formatWithCurrency(fund.totalValue, fund.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
