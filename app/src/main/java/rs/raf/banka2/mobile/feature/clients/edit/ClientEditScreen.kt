package rs.raf.banka2.mobile.feature.clients.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton

@Composable
fun ClientEditScreen(
    onBack: () -> Unit,
    viewModel: ClientEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is ClientEditEvent.Toast) snackbar.showSnackbar(it.message)
        }
    }

    var firstName by remember(state.client) { mutableStateOf(state.client?.firstName.orEmpty()) }
    var lastName by remember(state.client) { mutableStateOf(state.client?.lastName.orEmpty()) }
    var phone by remember(state.client) { mutableStateOf(state.client?.phoneNumber.orEmpty()) }
    var address by remember(state.client) { mutableStateOf(state.client?.address.orEmpty()) }

    BankaScaffold(
        title = "Izmena klijenta",
        onBack = onBack,
        snackbarHostState = snackbar,
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
            if (state.error != null) ErrorBanner(state.error)
            state.client?.let { client ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Email: ${client.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ID: ${client.id} · Status: ${client.status ?: "—"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        AppTextField(value = firstName, onValueChange = { firstName = it }, label = "Ime", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        AppTextField(value = lastName, onValueChange = { lastName = it }, label = "Prezime", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    AppTextField(value = phone, onValueChange = { phone = it }, label = "Telefon", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    AppTextField(value = address, onValueChange = { address = it }, label = "Adresa", singleLine = false, modifier = Modifier.fillMaxWidth())
                }
                PrimaryButton(
                    text = "Sacuvaj",
                    onClick = { viewModel.save(firstName, lastName, phone, address) },
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
