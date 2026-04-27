package rs.raf.banka2.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.theme.ThemeMode

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigate: (HomeAction) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = themeMode != ThemeMode.Light

    BankaScaffold(
        backgroundDecoration = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        },
        actions = {
            IconButton(onClick = viewModel::toggleTheme) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = if (isDark) "Light tema" else "Dark tema",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Osvezi", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {
                viewModel.logout()
                onLogout()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Odjavi se",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { padding ->
        val role = state.profile?.role ?: UserRole.Unknown
        if (role.isEmployee) {
            EmployeeHomeContent(
                state = state,
                role = role,
                onNavigate = onNavigate,
                contentPadding = padding
            )
        } else {
            ClientHomeContent(
                state = state,
                onNavigate = onNavigate,
                contentPadding = padding
            )
        }
    }
}
