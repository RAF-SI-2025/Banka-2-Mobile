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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Brush
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
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600
import rs.raf.banka2.mobile.data.dto.card.CardDto
import java.math.BigDecimal

/**
 * ME-03: per-prepaid card UI gating + Top-Up/Withdraw dialog parity sa FE 14.05 vece-3.
 */
private data class CardMoveAction(
    val card: CardDto,
    val direction: Direction
) {
    enum class Direction { TOP_UP, WITHDRAW }
}

@Composable
fun CardsScreen(
    onBack: () -> Unit,
    onRequestNewCard: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editingLimit by remember { mutableStateOf<CardDto?>(null) }
    // ME-02 fix: block kartice trazi explicit confirm dialog (parity sa FE 14.05.2026 vece-1).
    var blockConfirmFor by remember { mutableStateOf<Long?>(null) }
    // ME-03: top-up/withdraw dialog za INTERNET_PREPAID kartice
    var moveAction by remember { mutableStateOf<CardMoveAction?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CardEvent.Toast) snackbar.showSnackbar(event.message)
        }
    }

    BankaScaffold(
        title = "Moje kartice",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = onRequestNewCard) {
                Icon(Icons.Filled.Add, contentDescription = "Nova kartica", tint = MaterialTheme.colorScheme.primary)
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
            if (state.error != null) item { ErrorBanner(state.error) }
            if (state.cards.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CreditCard,
                        title = "Nema aktivnih kartica",
                        description = "Posalji zahtev za novu karticu uz neki od racuna."
                    )
                }
            } else {
                items(state.cards, key = { it.id }) { card ->
                    CardVisual(card = card)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rs.raf.banka2.mobile.core.ui.components.SecondaryButton(
                            text = "Limit",
                            onClick = { editingLimit = card },
                            leadingIcon = Icons.Filled.Tune,
                            modifier = Modifier.weight(1f)
                        )
                        if (card.status.equals("BLOCKED", true)) {
                            // Unblock zahteva employee permisiju na backend-u.
                            // Klijenti dobijaju 403 — UI ipak prikazuje akciju radi konzistentnosti sa FE-om.
                            rs.raf.banka2.mobile.core.ui.components.SecondaryButton(
                                text = "Odblokiraj",
                                onClick = { viewModel.unblockCard(card.id) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            rs.raf.banka2.mobile.core.ui.components.DangerButton(
                                text = "Blokiraj",
                                onClick = { blockConfirmFor = card.id },
                                leadingIcon = Icons.Filled.Block,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // ME-03: za INTERNET_PREPAID kartice prikaz Dopuni + Povuci. Povuci je disabled
                    // kad prepaid balance nije pozitivan.
                    if (card.isPrepaid) {
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rs.raf.banka2.mobile.core.ui.components.PrimaryButton(
                                text = "Dopuni",
                                onClick = { moveAction = CardMoveAction(card, CardMoveAction.Direction.TOP_UP) },
                                modifier = Modifier.weight(1f)
                            )
                            val canWithdraw = (card.prepaidBalance ?: BigDecimal.ZERO) > BigDecimal.ZERO
                            rs.raf.banka2.mobile.core.ui.components.SecondaryButton(
                                text = "Povuci",
                                onClick = { moveAction = CardMoveAction(card, CardMoveAction.Direction.WITHDRAW) },
                                enabled = canWithdraw,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    editingLimit?.let { card ->
        EditLimitDialog(
            card = card,
            onDismiss = { editingLimit = null },
            onConfirm = { newLimit ->
                viewModel.updateLimit(card.id, BigDecimal.valueOf(newLimit))
                editingLimit = null
            }
        )
    }

    // ME-02 fix: confirm dialog za block (parity sa FE).
    blockConfirmFor?.let { cardId ->
        AlertDialog(
            onDismissRequest = { blockConfirmFor = null },
            title = { Text("Blokiraj karticu?") },
            text = {
                Text(
                    "Posle blokade kartica vise nece moci da se koristi za placanja. " +
                        "Odblokirati moze samo zaposleni u banci."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.blockCard(cardId)
                    blockConfirmFor = null
                }) {
                    Text("Blokiraj", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { blockConfirmFor = null }) { Text("Otkazi") }
            }
        )
    }

    // ME-03: Top-Up / Withdraw dialog za INTERNET_PREPAID karticu.
    moveAction?.let { action ->
        CardMoveDialog(
            action = action,
            accounts = state.accounts,
            onDismiss = { moveAction = null },
            onConfirm = { accountId, amount ->
                when (action.direction) {
                    CardMoveAction.Direction.TOP_UP -> viewModel.topUpCard(action.card.id, accountId, amount)
                    CardMoveAction.Direction.WITHDRAW -> viewModel.withdrawFromCard(action.card.id, accountId, amount)
                }
                moveAction = null
            }
        )
    }
}

/**
 * ME-03: dialog za Top-Up (Account -> Card.prepaidBalance) ili Withdraw (Card -> Account).
 * Direction-aware label-i, isti UI sa pre-validacijom amount > 0.
 */
@Composable
private fun CardMoveDialog(
    action: CardMoveAction,
    accounts: List<rs.raf.banka2.mobile.data.dto.account.AccountDto>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: BigDecimal) -> Unit
) {
    val isTopUp = action.direction == CardMoveAction.Direction.TOP_UP
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf("") }
    val parsedAmount = MoneyFormatter.parseBigDecimal(amountText)
    val canConfirm = selectedAccountId != null && parsedAmount != null && parsedAmount > BigDecimal.ZERO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isTopUp) "Dopuni karticu" else "Povuci sa kartice") },
        text = {
            Column {
                Text(
                    if (isTopUp)
                        "Prebacujes iznos sa racuna na prepaid karticu (${action.card.cardName ?: "kartica"})."
                    else
                        "Prebacujes iznos sa kartice na racun. Trenutni balance: " +
                            "${MoneyFormatter.format(action.card.prepaidBalance ?: BigDecimal.ZERO)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isTopUp) "Sa kog racuna" else "Na koji racun",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                accounts.forEach { acc ->
                    val selected = selectedAccountId == acc.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedAccountId = acc.id }
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selected,
                            onClick = { selectedAccountId = acc.id }
                        )
                        Text(
                            text = "${acc.accountNumber} · ${acc.currency ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                rs.raf.banka2.mobile.core.ui.components.AppTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Iznos",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val acc = selectedAccountId ?: return@TextButton
                    val amt = parsedAmount ?: return@TextButton
                    onConfirm(acc, amt)
                },
                enabled = canConfirm
            ) {
                Text(if (isTopUp) "Dopuni" else "Povuci")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Otkazi") }
        }
    )
}

@Composable
private fun CardVisual(card: CardDto) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 14.dp,
        borderBrush = Brush.linearGradient(listOf(Indigo500, Violet600))
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CreditCard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp).height(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    card.brand ?: card.cardType ?: "Kartica",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    card.status ?: "AKTIVNA",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (card.status.equals("BLOCKED", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                AccountFormatter.maskCardNumber(card.cardNumber),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Vlasnik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(card.ownerName ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Vazi do", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(card.expirationDate ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            card.cardLimit?.let { limit ->
                Spacer(Modifier.height(6.dp))
                Text("Limit: ${MoneyFormatter.format(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // ME-03: per-category info — prepaid balance / credit limit / outstanding balance
            if (card.isPrepaid && card.prepaidBalance != null) {
                Text(
                    "Prepaid balance: ${MoneyFormatter.format(card.prepaidBalance)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (card.isCredit) {
                card.creditLimit?.let {
                    Text(
                        "Kreditni limit: ${MoneyFormatter.format(it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                card.outstandingBalance?.let {
                    Text(
                        "Duguje: ${MoneyFormatter.format(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EditLimitDialog(
    card: CardDto,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    // ME-11: cardLimit je sad BigDecimal, koristi MoneyFormatter BigDecimal overload.
    var limitText by remember { mutableStateOf(card.cardLimit?.let { MoneyFormatter.format(it) }.orEmpty()) }
    val parsed = MoneyFormatter.parse(limitText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promena limita") },
        text = {
            AppTextField(
                value = limitText,
                onValueChange = { limitText = it },
                label = "Novi limit",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null && parsed > 0) {
                Text("Sacuvaj")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}
