package rs.raf.banka2.mobile.feature.auth.activate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
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
fun ActivateAccountScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ActivateAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ActivateEvent.Success) onSuccess()
        }
    }

    BankaScaffold(
        title = "Aktivacija naloga",
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Aktiviraj svoj nalog zaposlenog",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Unesi token koji je banka poslala na tvoj email i postavi inicijalnu lozinku.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                AppTextField(
                    value = state.token,
                    onValueChange = viewModel::onTokenChange,
                    label = "Aktivacioni token",
                    leadingIcon = Icons.Outlined.VpnKey,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Lozinka",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = state.confirm,
                    onValueChange = viewModel::onConfirmChange,
                    label = "Potvrdi lozinku",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                ErrorBanner(message = state.error)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "Aktiviraj nalog",
                    onClick = viewModel::submit,
                    loading = state.isSubmitting,
                    leadingIcon = Icons.Filled.PersonAdd,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
