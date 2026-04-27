package rs.raf.banka2.mobile.feature.supervisor.margincreate

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun CreateMarginScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateMarginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is CreateMarginEvent.Created) onCreated(it.accountId) }
    }

    BankaScaffold(
        title = "Novi marzni racun",
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
                Text("Vlasnik (unesi userId ILI companyId)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(value = state.userId, onValueChange = viewModel::setUserId, label = "User ID", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    AppTextField(value = state.companyId, onValueChange = viewModel::setCompanyId, label = "Company ID", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Iznosi (RSD)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.initialMargin, onValueChange = viewModel::setInitialMargin, label = "Initial margin", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.maintenanceMargin, onValueChange = viewModel::setMaintenanceMargin, label = "Maintenance margin", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.bankParticipation, onValueChange = viewModel::setBankParticipation, label = "Bank participation (0..1, npr 0.5 = 50%)", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
            }
            ErrorBanner(state.error)
            PrimaryButton(text = "Kreiraj marzni racun", onClick = viewModel::submit, loading = state.submitting, modifier = Modifier.fillMaxWidth())
        }
    }
}
