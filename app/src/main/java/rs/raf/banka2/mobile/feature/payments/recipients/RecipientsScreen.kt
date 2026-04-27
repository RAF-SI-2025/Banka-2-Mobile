package rs.raf.banka2.mobile.feature.payments.recipients

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto

@Composable
fun RecipientsScreen(
    onBack: () -> Unit,
    viewModel: RecipientsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RecipientDto?>(null) }
    var creating by remember { mutableStateOf(false) }

    BankaScaffold(
        title = "Primaoci",
        onBack = onBack,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj", tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.error != null) item { ErrorBanner(state.error) }
            if (state.recipients.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.PersonAddAlt,
                        title = "Nema sacuvanih primalaca",
                        description = "Dodaj cest primaoca da brze inicirjes placanja sa Home ekrana."
                    )
                }
            } else {
                items(state.recipients, key = { it.id }) { recipient ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipient.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    AccountFormatter.formatAccountNumber(recipient.accountNumber),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!recipient.description.isNullOrBlank()) {
                                    Text(recipient.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { editing = recipient }) {
                                Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.delete(recipient.id) }) {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        RecipientDialog(
            initial = null,
            isLoading = state.submitting,
            onDismiss = { creating = false },
            onConfirm = { name, account, description ->
                viewModel.create(name, account, description)
                creating = false
            }
        )
    }
    editing?.let { current ->
        RecipientDialog(
            initial = current,
            isLoading = state.submitting,
            onDismiss = { editing = null },
            onConfirm = { name, account, description ->
                viewModel.update(current.id, name, account, description)
                editing = null
            }
        )
    }
}

@Composable
private fun RecipientDialog(
    initial: RecipientDto?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    val canSave = name.isNotBlank() && accountNumber.isNotBlank() && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(if (initial == null) "Dodaj primaoca" else "Izmeni primaoca") },
        text = {
            Column {
                AppTextField(value = name, onValueChange = { name = it }, label = "Ime", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = "Broj racuna",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Opis (opciono)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), accountNumber.trim(), description.takeIf { it.isNotBlank() }) },
                enabled = canSave
            ) {
                Text(if (isLoading) "Cuvam..." else "Sacuvaj")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Otkazi") } }
    )
}
