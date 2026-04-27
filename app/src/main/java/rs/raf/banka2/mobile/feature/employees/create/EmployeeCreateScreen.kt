package rs.raf.banka2.mobile.feature.employees.create

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
fun EmployeeCreateScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: EmployeeCreateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is EmployeeCreateEvent.Created) onCreated(it.employeeId)
        }
    }

    BankaScaffold(
        title = "Novi zaposleni",
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
                    "Banka generise privremenu lozinku i salje email sa linkom za aktivaciju.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(value = state.email, onValueChange = viewModel::setEmail, label = "Email *", keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(value = state.firstName, onValueChange = viewModel::setFirstName, label = "Ime *", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    AppTextField(value = state.lastName, onValueChange = viewModel::setLastName, label = "Prezime *", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.phoneNumber, onValueChange = viewModel::setPhone, label = "Telefon", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.address, onValueChange = viewModel::setAddress, label = "Adresa", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    AppTextField(value = state.gender, onValueChange = viewModel::setGender, label = "Pol (M/F)", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    AppTextField(value = state.position, onValueChange = viewModel::setPosition, label = "Pozicija", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(value = state.department, onValueChange = viewModel::setDepartment, label = "Departman", modifier = Modifier.fillMaxWidth())
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Permisije", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.setIsAgent(!state.isAgent) }) {
                    Checkbox(checked = state.isAgent, onCheckedChange = viewModel::setIsAgent)
                    Text("Agent (trgovina hartijama)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.setIsSupervisor(!state.isSupervisor) }) {
                    Checkbox(checked = state.isSupervisor, onCheckedChange = viewModel::setIsSupervisor)
                    Text("Supervizor (orderi/aktuari/porez)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            ErrorBanner(state.error)
            PrimaryButton(text = "Kreiraj zaposlenog", onClick = viewModel::submit, loading = state.submitting, modifier = Modifier.fillMaxWidth())
        }
    }
}
