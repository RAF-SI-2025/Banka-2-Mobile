package rs.raf.banka2.mobile.feature.otc.discovery

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
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
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.feature.otc.OtcScope
import java.time.LocalDate

@Composable
fun OtcDiscoveryScreen(
    onBack: () -> Unit,
    onOpenOffersAndContracts: () -> Unit,
    viewModel: OtcDiscoveryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickedListing by remember { mutableStateOf<OtcListingDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OtcDiscoveryEvent.OfferSent) {
                snackbar.showSnackbar("Ponuda poslata.")
                pickedListing = null
            }
        }
    }

    BankaScaffold(
        title = "OTC trgovina",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            TextButton(onClick = onOpenOffersAndContracts) {
                Text("Ponude/Ugovori")
            }
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ScopeTabRow(state.scope, viewModel::setScope)
            Spacer(Modifier.height(8.dp))
            ErrorBanner(state.error)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.listings.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Storefront,
                            title = "Nema dostupnih hartija",
                            description = "Drugi korisnici jos uvek nisu izlozili hartije za OTC trgovinu."
                        )
                    }
                }
                items(state.listings, key = { "${it.listingId}-${it.sellerUserId ?: 0}-${it.bankRoutingNumber.orEmpty()}" }) { listing ->
                    GlassCard(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickedListing = listing }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(listing.ticker, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text(listing.name ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Prodaje: ${listing.sellerName ?: "—"} ${if (listing.foreign) " · banka ${listing.bankRoutingNumber ?: "?"}" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${listing.publicQuantity} kom",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                listing.currentPrice?.let {
                                    Text(
                                        MoneyFormatter.formatWithCurrency(it, listing.currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pickedListing?.let { listing ->
        CreateOfferDialog(
            listing = listing,
            isLoading = state.submitting,
            onDismiss = { pickedListing = null },
            onConfirm = { qty, price, premium, settlement ->
                viewModel.submitOffer(listing, qty, price, premium, settlement)
            }
        )
    }
}

@Composable
private fun ScopeTabRow(selected: OtcScope, onSelect: (OtcScope) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OtcScope.entries.forEach { scope ->
            val isSelected = selected == scope
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    )
                    .clickable { onSelect(scope) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    scope.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CreateOfferDialog(
    listing: OtcListingDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, Double, String) -> Unit
) {
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf(listing.currentPrice?.let { MoneyFormatter.format(it, 2) }.orEmpty()) }
    var premium by remember { mutableStateOf("") }
    var settlement by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }

    val parsedQty = qty.toIntOrNull()
    val parsedPrice = MoneyFormatter.parse(price)
    val parsedPremium = MoneyFormatter.parse(premium)
    val canSubmit = !isLoading && parsedQty != null && parsedQty in 1..listing.publicQuantity &&
        parsedPrice != null && parsedPrice > 0 && parsedPremium != null && parsedPremium >= 0 &&
        settlement.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Nova ponuda za ${listing.ticker}") },
        text = {
            Column {
                Text(
                    "Dostupno ${listing.publicQuantity} kom · trzisna cena ${MoneyFormatter.formatWithCurrency(listing.currentPrice ?: 0.0, listing.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = qty,
                    onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                    label = "Kolicina (max ${listing.publicQuantity})",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Cena po komadu",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = premium,
                    onValueChange = { premium = it },
                    label = "Premija (cena ugovora)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = settlement,
                    onValueChange = { settlement = it },
                    label = "Settlement (YYYY-MM-DD)",
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Settlement mora biti u buducnosti (${DateFormatter.nowIsoLocalDate()} ili kasnije).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(parsedQty ?: 0, parsedPrice ?: 0.0, parsedPremium ?: 0.0, settlement.trim())
                },
                enabled = canSubmit
            ) {
                Text(if (isLoading) "Saljem..." else "Posalji ponudu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Otkazi") }
        }
    )
}
