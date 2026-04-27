package rs.raf.banka2.mobile.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600

@Composable
fun LoginScreen(
    onLoggedIn: (UserRole) -> Unit,
    onForgotPassword: () -> Unit,
    onActivateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoggedIn -> onLoggedIn(event.role)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dobrodosao nazad",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Prijavi se da nastavis sa Banka 2 nalogom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                AppTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    placeholder = "ime.prezime@banka.rs",
                    leadingIcon = Icons.Outlined.Email,
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Lozinka",
                    placeholder = "Unesi lozinku",
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.passwordError,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Zaboravljena lozinka?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onForgotPassword)
                        .padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                ErrorBanner(message = state.generalError)

                Spacer(Modifier.height(16.dp))

                PrimaryButton(
                    text = "Prijavi se",
                    onClick = viewModel::submit,
                    loading = state.isSubmitting,
                    leadingIcon = Icons.AutoMirrored.Filled.Login,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Aktiviraj nalog (zaposleni)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onActivateAccount)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Footer()
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rs.raf.banka2.mobile.core.ui.components.BankaLogo(size = 96.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Banka 2",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Mobilno bankarstvo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Footer() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BANKA 2025 - TIM 2",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
