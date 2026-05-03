package rs.raf.banka2.mobile.feature.otc.contracts

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.feature.otc.deviationStyle

@Composable
fun OtcOffersAndContractsScreen(
    onBack: () -> Unit,
    viewModel: OtcOffersAndContractsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var counterOffer by remember { mutableStateOf<OtcOfferDto?>(null) }
    var exerciseTarget by remember { mutableStateOf<OtcContractDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is OtcOffersAndContractsEvent.Toast) snackbar.showSnackbar(it.message) }
    }

    BankaScaffold(
        title = "OTC ponude i ugovori",
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            TabsRow(state.tab, state.unreadIntra, state.unreadInter, viewModel::setTab)
            Spacer(Modifier.height(8.dp))
            ErrorBanner(state.error)
            when (state.tab) {
                OtcTab.OffersDomestic, OtcTab.OffersForeign -> OffersList(
                    offers = state.offers,
                    loading = state.loading,
                    onAccept = { viewModel.acceptOffer(it, null) },
                    onDecline = { viewModel.declineOffer(it) },
                    onCounter = { counterOffer = it }
                )
                OtcTab.ContractsDomestic, OtcTab.ContractsForeign -> ContractsList(
                    contracts = state.contracts,
                    loading = state.loading,
                    onExercise = { exerciseTarget = it }
                )
            }
        }
    }

    counterOffer?.let { offer ->
        CounterOfferDialog(
            offer = offer,
            onDismiss = { counterOffer = null },
            onConfirm = { qty, price, premium, settlement ->
                viewModel.counterOffer(offer, qty, price, premium, settlement)
                counterOffer = null
            }
        )
    }

    exerciseTarget?.let { contract ->
        ExerciseDialog(
            contract = contract,
            onDismiss = { exerciseTarget = null },
            onConfirm = {
                viewModel.startExercise(contract, null)
                exerciseTarget = null
            }
        )
    }

    state.exerciseInProgress?.let { progress ->
        ExerciseSagaModal(
            progress = progress,
            onClose = viewModel::closeExercise
        )
    }
}

@Composable
private fun TabsRow(
    selected: OtcTab,
    unreadIntra: Int,
    unreadInter: Int,
    onSelect: (OtcTab) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(OtcTab.entries.toList()) { tab ->
            val isSelected = selected == tab
            // Discord-style unread badge — Spec Celina 4 (Nova) §2030-2090.
            // Badge se prikazuje samo na "Ponude" tabovima (ne na "Ugovori") i samo
            // kad je brojac > 0, slicno notifikacionom pristupu.
            val badge = when (tab) {
                OtcTab.OffersDomestic -> unreadIntra
                OtcTab.OffersForeign -> unreadInter
                else -> 0
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (badge > 0) {
                    Spacer(Modifier.size(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badge > 9) "9+" else badge.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OffersList(
    offers: List<OtcOfferDto>,
    loading: Boolean,
    onAccept: (OtcOfferDto) -> Unit,
    onDecline: (OtcOfferDto) -> Unit,
    onCounter: (OtcOfferDto) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (offers.isEmpty() && !loading) {
            item {
                EmptyState(
                    icon = Icons.Filled.Description,
                    title = "Nema aktivnih ponuda",
                    description = "Kreiraj ponudu sa OTC ekrana ili sacekaj kontraponudu."
                )
            }
        }
        items(offers, key = { it.id }) { offer ->
            OfferCard(offer = offer, onAccept = { onAccept(offer) }, onDecline = { onDecline(offer) }, onCounter = { onCounter(offer) })
        }
    }
}

@Composable
private fun OfferCard(
    offer: OtcOfferDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounter: () -> Unit
) {
    val deviation = deviationStyle(offer.pricePerStock, offer.currentPrice)
    val tint = deviation?.tint ?: MaterialTheme.colorScheme.primary
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(tint.copy(alpha = 0.6f), androidx.compose.ui.graphics.Color.Transparent)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${offer.listingTicker ?: offer.listingId} · ${offer.quantity} kom",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Cena ${MoneyFormatter.format(offer.pricePerStock, 2)} · premija ${MoneyFormatter.format(offer.premium, 2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Settlement: ${DateFormatter.formatDate(offer.settlementDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Status: ${offer.status} · ${offer.modifiedBy?.let { "modifikovao $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                deviation?.let {
                    Text(
                        "${if (it.percent >= 0) "+" else ""}${MoneyFormatter.format(it.percent, 2)} %",
                        style = MaterialTheme.typography.titleSmall,
                        color = it.tint,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(it.label, style = MaterialTheme.typography.labelSmall, color = it.tint)
                }
                offer.currentPrice?.let {
                    Text(
                        "Trzisno: ${MoneyFormatter.format(it, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (offer.status.equals("ACTIVE", true)) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PrimaryButton(text = "Prihvati", onClick = onAccept, modifier = Modifier.weight(1f))
                SecondaryButton(text = "Kontra", onClick = onCounter, modifier = Modifier.weight(1f))
                DangerButton(text = "Odustani", onClick = onDecline, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContractsList(
    contracts: List<OtcContractDto>,
    loading: Boolean,
    onExercise: (OtcContractDto) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (contracts.isEmpty() && !loading) {
            item {
                EmptyState(
                    icon = Icons.Filled.Description,
                    title = "Nema sklopljenih ugovora",
                    description = "Kada prihvatis OTC ponudu, ugovor ce se pojaviti ovde."
                )
            }
        }
        items(contracts, key = { it.id }) { contract ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${contract.listingTicker ?: contract.listingId} · ${contract.quantity} kom",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Strike ${MoneyFormatter.format(contract.strikePrice, 2)} · premija ${MoneyFormatter.format(contract.premium, 2)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Settlement: ${DateFormatter.formatDate(contract.settlementDate)} · ${contract.myRole ?: "—"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            contract.status,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (contract.status) {
                                "ACTIVE" -> MaterialTheme.colorScheme.primary
                                "EXERCISED" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        contract.profitEstimate?.let {
                            Text(
                                "Profit ${MoneyFormatter.format(it, 2)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (contract.status.equals("ACTIVE", true) && contract.myRole.equals("BUYER", true)) {
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(text = "Iskoristi", onClick = { onExercise(contract) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CounterOfferDialog(
    offer: OtcOfferDto,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, Double, String) -> Unit
) {
    var qty by remember { mutableStateOf(offer.quantity.toString()) }
    var price by remember { mutableStateOf(MoneyFormatter.format(offer.pricePerStock, 2)) }
    var premium by remember { mutableStateOf(MoneyFormatter.format(offer.premium, 2)) }
    var settlement by remember { mutableStateOf(offer.settlementDate.orEmpty()) }
    val parsedQty = qty.toIntOrNull()
    val parsedPrice = MoneyFormatter.parse(price)
    val parsedPremium = MoneyFormatter.parse(premium)
    val canSubmit = parsedQty != null && parsedQty > 0 && parsedPrice != null && parsedPremium != null && settlement.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kontraponuda") },
        text = {
            Column {
                AppTextField(value = qty, onValueChange = { qty = it.filter { ch -> ch.isDigit() } }, label = "Kolicina", keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = price, onValueChange = { price = it }, label = "Cena po komadu", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = premium, onValueChange = { premium = it }, label = "Premija", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = settlement, onValueChange = { settlement = it }, label = "Settlement (YYYY-MM-DD)", modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsedQty ?: 0, parsedPrice ?: 0.0, parsedPremium ?: 0.0, settlement.trim()) }, enabled = canSubmit) {
                Text("Posalji kontraponudu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

@Composable
private fun ExerciseDialog(
    contract: OtcContractDto,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iskoristi opciju") },
        text = {
            Column {
                Text(
                    "Iskoristices ugovor #${contract.id}: kupujes ${contract.quantity} kom ${contract.listingTicker ?: ""} po strike-u ${MoneyFormatter.format(contract.strikePrice, 2)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (contract.foreign) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Inostrana banka — pokrenuce se SAGA transakcija (5 koraka). Pratis status u realnom vremenu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Pokreni") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}

private val SAGA_PHASES = listOf(
    "INITIATED" to "Inicijalizacija",
    "RESERVE_FUNDS" to "Rezervacija sredstava (kupac)",
    "RESERVE_SHARES" to "Rezervacija hartija (prodavac)",
    "TRANSFER_FUNDS" to "Prebacivanje novca",
    "TRANSFER_OWNERSHIP" to "Prebacivanje vlasnistva",
    "COMMITTED" to "Finalizacija"
)

@Composable
private fun ExerciseSagaModal(
    progress: ExerciseProgress,
    onClose: () -> Unit
) {
    val isTerminal = progress.phase in setOf("COMMITTED", "ABORTED", "STUCK")
    val currentIndex = SAGA_PHASES.indexOfFirst { it.first == progress.phase }.coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = { if (isTerminal) onClose() },
        title = {
            Text(
                if (progress.foreign) "Inter-bank exercise (SAGA)" else "Exercise"
            )
        },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = {
                        if (isTerminal && progress.phase != "COMMITTED") 1f
                        else (currentIndex + 1).toFloat() / SAGA_PHASES.size
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
                Spacer(Modifier.height(12.dp))
                if (progress.foreign) {
                    SAGA_PHASES.forEachIndexed { index, (key, label) ->
                        val isActive = key == progress.phase
                        val isPast = index < currentIndex || (progress.phase == "COMMITTED" && index <= currentIndex)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        when {
                                            isPast -> MaterialTheme.colorScheme.tertiary
                                            isActive -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else if (isPast) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isTerminal) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Status: ${progress.phase}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(12.dp))
                progress.message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress.phase == "ABORTED" || progress.phase == "STUCK")
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose, enabled = isTerminal) {
                Text(if (isTerminal) "Zatvori" else "Ceka se...")
            }
        }
    )
}
