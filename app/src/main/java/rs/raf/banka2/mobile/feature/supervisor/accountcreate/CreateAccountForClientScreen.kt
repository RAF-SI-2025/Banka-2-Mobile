package rs.raf.banka2.mobile.feature.supervisor.accountcreate

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
fun CreateAccountForClientScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateAccountForClientViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is CreateAccountForClientEvent.Created) onCreated(it.accountId)
        }
    }

    BankaScaffold(
        title = "Novi racun (klijent)",
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AppTextField(value = state.ownerEmail, onValueChange = viewModel::setOwnerEmail, label = "Email vlasnika *", keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(value = state.accountType, onValueChange = viewModel::setAccountType, label = "Tip (CHECKING/SAVINGS/BUSINESS) *", modifier = Modifier.weight(2f))
                    Spacer(Modifier.width(8.dp))
                    AppTextField(value = state.currency, onValueChange = viewModel::setCurrency, label = "Valuta *", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.accountSubtype, onValueChange = viewModel::setSubtype, label = "Podtip (standardni/studentski/DOO/...)", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.initialDeposit, onValueChange = viewModel::setInitialDeposit, label = "Pocetni depozit", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.setCreateCard(!state.createCard) }) {
                    Checkbox(checked = state.createCard, onCheckedChange = viewModel::setCreateCard)
                    Text("Otvori i debitnu karticu uz racun", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            ErrorBanner(state.error)
            PrimaryButton(text = "Kreiraj racun", onClick = viewModel::submit, loading = state.submitting, modifier = Modifier.fillMaxWidth())
        }
    }
}
