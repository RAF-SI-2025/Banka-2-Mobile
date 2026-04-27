package rs.raf.banka2.mobile.feature.accounts

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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.components.VerificationModal

@Composable
fun AccountDetailsScreen(
    onBack: () -> Unit,
    viewModel: AccountDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showRename by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountDetailsEvent.Toast -> snackbar.showSnackbar(event.message)
                AccountDetailsEvent.LimitSaved -> {
                    showLimitDialog = false
                    snackbar.showSnackbar("Limiti su azurirani.")
                }
            }
        }
    }

    BankaScaffold(
        title = "Detalji racuna",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { showRename = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Promeni naziv", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { showLimitDialog = true }) {
                Icon(Icons.Filled.Tune, contentDescription = "Limiti", tint = MaterialTheme.colorScheme.onSurface)
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
            if (state.generalError != null) {
                item { ErrorBanner(state.generalError) }
            }
            state.account?.let { acc ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = acc.name?.takeIf { it.isNotBlank() } ?: acc.accountType ?: "Racun",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AccountFormatter.formatAccountNumber(acc.accountNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Stanje", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.formatWithCurrency(acc.balance, acc.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Raspolozivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dnevni limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    acc.dailyLimit?.let { MoneyFormatter.formatWithCurrency(it, acc.currency) } ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mesecni limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    acc.monthlyLimit?.let { MoneyFormatter.formatWithCurrency(it, acc.currency) } ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Transakcije na ovom racunu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (state.transactions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Tune,
                            title = "Nema transakcija",
                            description = "Tek kada izvrsis prvu uplatu/isplatu pojavice se ovde."
                        )
                    }
                } else {
                    items(state.transactions, key = { it.id }) { tx ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = tx.recipientName ?: tx.description ?: "Transakcija",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${tx.status ?: "—"} · ${DateFormatter.formatDateTime(tx.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = MoneyFormatter.formatWithCurrency(tx.amount, tx.currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        RenameDialog(
            initialName = state.account?.name.orEmpty(),
            isLoading = state.isRenaming,
            onDismiss = { showRename = false },
            onConfirm = {
                viewModel.renameAccount(it)
                showRename = false
            }
        )
    }

    if (showLimitDialog) {
        LimitDialog(
            currency = state.account?.currency,
            isLoading = state.isSavingLimit,
            error = state.limitError,
            initialDaily = state.account?.dailyLimit,
            initialMonthly = state.account?.monthlyLimit,
            onDismiss = { showLimitDialog = false },
            onSubmit = { daily, monthly, otpCode ->
                viewModel.submitLimitChange(daily, monthly, otpCode)
            }
        )
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Promeni naziv racuna") },
        text = {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Naziv",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank() && !isLoading) {
                Text(if (isLoading) "Cuvam..." else "Sacuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Otkazi") }
        }
    )
}

@Composable
private fun LimitDialog(
    currency: String?,
    isLoading: Boolean,
    error: String?,
    initialDaily: Double?,
    initialMonthly: Double?,
    onDismiss: () -> Unit,
    onSubmit: (daily: Double?, monthly: Double?, otpCode: String?) -> Unit
) {
    var daily by remember { mutableStateOf(initialDaily?.let { MoneyFormatter.format(it) }.orEmpty()) }
    var monthly by remember { mutableStateOf(initialMonthly?.let { MoneyFormatter.format(it) }.orEmpty()) }
    var showOtp by remember { mutableStateOf(false) }
    val parsedDaily = MoneyFormatter.parse(daily)
    val parsedMonthly = MoneyFormatter.parse(monthly)
    val canSubmit = (parsedDaily != null || parsedMonthly != null) && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Promena limita") },
        text = {
            Column {
                Text(
                    "Banka zahteva OTP kod pre nego sto sacuvas izmenu limita.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = daily,
                    onValueChange = { daily = it },
                    label = "Dnevni limit ${currency.orEmpty()}",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = monthly,
                    onValueChange = { monthly = it },
                    label = "Mesecni limit ${currency.orEmpty()}",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                ErrorBanner(error)
            }
        },
        confirmButton = {
            TextButton(onClick = { showOtp = true }, enabled = canSubmit) {
                Text(if (isLoading) "Cuvam..." else "Sacuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Otkazi") }
        }
    )

    VerificationModal(
        visible = showOtp,
        onDismiss = { showOtp = false },
        isVerifying = isLoading,
        externalError = error,
        onSubmit = { code ->
            onSubmit(parsedDaily, parsedMonthly, code)
            showOtp = false
        }
    )
}
