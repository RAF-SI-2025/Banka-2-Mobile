package rs.raf.banka2.mobile.feature.funds.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun CreateFundScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateFundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is CreateFundEvent.Created) onCreated(it.fundId) }
    }

    BankaScaffold(
        title = "Kreiraj fond",
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
                Text(
                    "Fond je dinarski. Dinarski racun fonda otvara banka automatski.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(value = state.name, onValueChange = viewModel::setName, label = "Naziv fonda", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.description, onValueChange = viewModel::setDescription, label = "Opis (strategija fonda)", singleLine = false, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.minContribution, onValueChange = viewModel::setMinContribution, label = "Minimalna uplata RSD", keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
            }
            ErrorBanner(state.error)
            PrimaryButton(text = "Kreiraj fond", onClick = viewModel::submit, loading = state.submitting, modifier = Modifier.fillMaxWidth())
        }
    }
}
