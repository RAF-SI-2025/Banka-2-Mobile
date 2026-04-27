package rs.raf.banka2.mobile.feature.actuaries

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.actuary.ActuaryDto

@Composable
fun ActuariesScreen(
    onBack: () -> Unit,
    viewModel: ActuariesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<ActuaryDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is ActuariesEvent.Toast) snackbar.showSnackbar(it.message) }
    }

    BankaScaffold(
        title = "Aktuari",
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.error != null) item { ErrorBanner(state.error) }
            items(state.agents, key = { it.employeeId }) { agent ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(agent.displayName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(agent.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Limit: ${agent.dailyLimit?.let { MoneyFormatter.format(it) } ?: "—"} · iskorisceno ${agent.usedLimit?.let { MoneyFormatter.format(it) } ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (agent.needApproval == true) {
                                Text("Zahteva odobrenje supervizora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(text = "Limit", onClick = { editing = agent }, modifier = Modifier.weight(1f))
                        SecondaryButton(text = "Reset usedLimit", onClick = { viewModel.resetLimit(agent.employeeId) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    editing?.let { agent ->
        EditLimitDialog(
            agent = agent,
            onDismiss = { editing = null },
            onConfirm = { limit, needApproval ->
                viewModel.updateLimit(agent.employeeId, limit, needApproval)
                editing = null
            }
        )
    }
}

@Composable
private fun EditLimitDialog(
    agent: ActuaryDto,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var limitText by remember { mutableStateOf(agent.dailyLimit?.let { MoneyFormatter.format(it) }.orEmpty()) }
    var needApproval by remember { mutableStateOf(agent.needApproval ?: false) }
    val parsed = MoneyFormatter.parse(limitText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limit aktuara") },
        text = {
            Column {
                AppTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = "Dnevni limit",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { needApproval = !needApproval }
                ) {
                    Checkbox(checked = needApproval, onCheckedChange = { needApproval = it })
                    Text("Zahteva odobrenje supervizora", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onConfirm(it, needApproval) } }, enabled = parsed != null) {
                Text("Sacuvaj")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkazi") } }
    )
}
