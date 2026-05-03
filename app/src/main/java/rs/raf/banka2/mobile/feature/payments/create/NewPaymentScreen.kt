package rs.raf.banka2.mobile.feature.payments.create

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto

@Composable
fun NewPaymentScreen(
    onBack: () -> Unit,
    onSuccess: (Long) -> Unit,
    viewModel: NewPaymentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is NewPaymentEvent.Success) onSuccess(event.paymentId)
        }
    }

    BankaScaffold(
        title = "Novo placanje",
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
            AccountSelector(
                accounts = state.accounts,
                selected = state.fromAccount,
                onSelect = viewModel::selectAccount
            )
            if (state.recipients.isNotEmpty()) {
                RecipientSelector(
                    recipients = state.recipients,
                    selected = state.selectedRecipient,
                    onSelect = viewModel::selectRecipient
                )
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Detalji primaoca",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.recipientName,
                    onValueChange = viewModel::setRecipientName,
                    label = "Ime primaoca",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.toAccountNumber,
                    onValueChange = viewModel::setToAccountNumber,
                    label = "Broj racuna",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Iznos i svrha",
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
                    value = state.paymentPurpose,
                    onValueChange = viewModel::setPurpose,
                    label = "Svrha placanja",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(
                        value = state.paymentCode,
                        onValueChange = viewModel::setPaymentCode,
                        label = "Sifra",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    AppTextField(
                        value = state.referenceNumber,
                        onValueChange = viewModel::setReferenceNumber,
                        label = "Poziv na broj",
                        modifier = Modifier.weight(2f)
                    )
                }
            }
            InterbankRoutingHint(state)
            ErrorBanner(state.error)
            PrimaryButton(
                text = "Potvrdi placanje",
                onClick = viewModel::openVerification,
                loading = state.verifying,
                leadingIcon = Icons.AutoMirrored.Filled.Send,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    VerificationModal(
        visible = state.showVerification,
        onDismiss = viewModel::closeVerification,
        isVerifying = state.verifying,
        externalError = state.error,
        onSubmit = { code -> viewModel.submitWithCode(code) }
    )

    state.interbankProgress?.let { progress ->
        InterbankProgressDialog(
            progress = progress,
            onDismiss = viewModel::closeInterbankProgress
        )
    }
}

@Composable
private fun InterbankRoutingHint(state: NewPaymentState) {
    val routing = AccountFormatter.routingPrefix(state.toAccountNumber)
    if (routing == null || state.toAccountNumber.length < 3) return
    val isInter = routing != "222"
    // Spec Celina 5 (Nova): kad je placanje inter-bank, korisnik mora odmah da
    // bude obavesten da transakcija ide preko 2-Phase Commit protokola, da
    // moze potrajati 1-2 min, i da ce videti progress posle Submit-a.
    val accent = if (isInter) Color(0xFFEAB308) else MaterialTheme.colorScheme.primary
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = accent
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isInter) "Medjubankarsko placanje (banka $routing)"
                    else "Intra-bank placanje (Banka 2)",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isInter) accent else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isInter)
                        "Pokrece se 2-Phase Commit transakcija ka drugoj banci. " +
                            "Moze potrajati 1-2 min — pratis status u realnom vremenu po Submit-u."
                    else "Trenutni saldo se direktno prebacuje primaocu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val INTERBANK_PHASES = listOf(
    "INITIATED" to "Inicijalizacija",
    "PREPARED" to "Prepare poslat partnerskoj banci",
    "READY" to "Partnerska banka spremna",
    "COMMITTED" to "Sredstva prebacena"
)

@Composable
private fun InterbankProgressDialog(
    progress: InterbankProgress,
    onDismiss: () -> Unit
) {
    // NOT_READY je 2PC respond — nije terminal; BE ga prevodi u ABORTED.
    val terminal = progress.status in setOf("COMMITTED", "ABORTED", "STUCK")
    val currentIndex = INTERBANK_PHASES.indexOfFirst { it.first == progress.status }.coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = { if (terminal) onDismiss() },
        title = { Text("Inter-bank placanje") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = {
                        if (progress.status == "COMMITTED") 1f
                        else (currentIndex + 1).toFloat() / INTERBANK_PHASES.size
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!terminal) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Status: ${progress.status}",
                        style = MaterialTheme.typography.titleSmall,
                        color = when (progress.status) {
                            "COMMITTED" -> MaterialTheme.colorScheme.tertiary
                            "ABORTED", "STUCK", "NOT_READY" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
                progress.transactionId?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Transakcija #$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (progress.convertedAmount != null && progress.convertedCurrency != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Konvertovano: ${MoneyFormatter.formatWithCurrency(progress.convertedAmount, progress.convertedCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                progress.rate?.let {
                    Text(
                        "Kurs: ${MoneyFormatter.format(it, 4)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                progress.fee?.let {
                    Text(
                        "Provizija: ${MoneyFormatter.format(it, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                progress.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = terminal) {
                Text(if (terminal) "Zatvori" else "Ceka se...")
            }
        }
    )
}

@Composable
private fun AccountSelector(
    accounts: List<AccountDto>,
    selected: AccountDto?,
    onSelect: (AccountDto) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Sa kog racuna",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        if (accounts.isEmpty()) {
            Text(
                "Ucitavam racune...",
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
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(acc) }
                        .padding(12.dp)
                        .width(220.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = acc.name?.takeIf { it.isNotBlank() } ?: acc.accountType ?: "Racun",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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

@Composable
private fun RecipientSelector(
    recipients: List<RecipientDto>,
    selected: RecipientDto?,
    onSelect: (RecipientDto?) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Sacuvani primaoci",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recipients, key = { it.id }) { recipient ->
                val isSelected = selected?.id == recipient.id
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(if (isSelected) null else recipient) }
                        .padding(10.dp)
                        .width(180.dp)
                ) {
                    Text(
                        recipient.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        AccountFormatter.formatAccountNumber(recipient.accountNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
