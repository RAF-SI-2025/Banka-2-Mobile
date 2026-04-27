package rs.raf.banka2.mobile.feature.employees.edit

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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.DangerButton
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton

@Composable
fun EmployeeEditScreen(
    onBack: () -> Unit,
    viewModel: EmployeeEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is EmployeeEditEvent.Toast) snackbar.showSnackbar(it.message)
        }
    }

    var firstName by remember(state.employee) { mutableStateOf(state.employee?.firstName.orEmpty()) }
    var lastName by remember(state.employee) { mutableStateOf(state.employee?.lastName.orEmpty()) }
    var phone by remember(state.employee) { mutableStateOf(state.employee?.phoneNumber.orEmpty()) }
    var address by remember(state.employee) { mutableStateOf(state.employee?.address.orEmpty()) }
    var position by remember(state.employee) { mutableStateOf(state.employee?.position.orEmpty()) }
    var department by remember(state.employee) { mutableStateOf(state.employee?.department.orEmpty()) }
    var isAgent by remember(state.employee) { mutableStateOf(state.employee?.isAgent ?: false) }
    var isSupervisor by remember(state.employee) { mutableStateOf(state.employee?.isSupervisor ?: false) }
    var active by remember(state.employee) { mutableStateOf(state.employee?.active ?: true) }

    BankaScaffold(
        title = "Izmena zaposlenog",
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
            state.employee?.let { emp ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(emp.email, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        AppTextField(value = firstName, onValueChange = { firstName = it }, label = "Ime", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        AppTextField(value = lastName, onValueChange = { lastName = it }, label = "Prezime", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    AppTextField(value = phone, onValueChange = { phone = it }, label = "Telefon", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    AppTextField(value = address, onValueChange = { address = it }, label = "Adresa", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row {
                        AppTextField(value = position, onValueChange = { position = it }, label = "Pozicija", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        AppTextField(value = department, onValueChange = { department = it }, label = "Departman", modifier = Modifier.weight(1f))
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Permisije i status", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isAgent = !isAgent }) {
                        Checkbox(checked = isAgent, onCheckedChange = { isAgent = it })
                        Text("Agent", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isSupervisor = !isSupervisor }) {
                        Checkbox(checked = isSupervisor, onCheckedChange = { isSupervisor = it })
                        Text("Supervizor", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { active = !active }) {
                        Checkbox(checked = active, onCheckedChange = { active = it })
                        Text("Aktivan nalog", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                PrimaryButton(
                    text = "Sacuvaj izmene",
                    onClick = {
                        viewModel.update(firstName, lastName, phone, address, position, department, isAgent, isSupervisor, active)
                    },
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth()
                )
                if (emp.active != false) {
                    DangerButton(
                        text = "Deaktiviraj nalog",
                        onClick = viewModel::deactivate,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
