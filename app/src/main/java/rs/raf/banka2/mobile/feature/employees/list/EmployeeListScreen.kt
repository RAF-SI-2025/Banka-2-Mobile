package rs.raf.banka2.mobile.feature.employees.list

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard

@Composable
fun EmployeeListScreen(
    onBack: () -> Unit,
    onEmployeeClick: (Long) -> Unit,
    onCreateNew: () -> Unit,
    viewModel: EmployeeListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Zaposleni",
        onBack = onBack,
        actions = {
            IconButton(onClick = onCreateNew) {
                Icon(Icons.Filled.Add, contentDescription = "Novi zaposleni", tint = MaterialTheme.colorScheme.primary)
            }
        },
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
                .padding(horizontal = 16.dp)
        ) {
            AppTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                label = "Pretraga (email / ime / prezime)",
                leadingIcon = Icons.Outlined.Search,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ErrorBanner(state.error)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.employees.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.People,
                            title = "Nema zaposlenih",
                            description = "Probaj druga pretragu ili kreiraj novog zaposlenog."
                        )
                    }
                }
                items(state.employees, key = { it.id }) { employee ->
                    GlassCard(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEmployeeClick(employee.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${employee.firstName.orEmpty()} ${employee.lastName.orEmpty()}".trim().ifBlank { employee.email },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(employee.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${employee.position ?: "—"} · ${employee.department ?: "—"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val statusLabel = when {
                                employee.active == false -> "Neaktivan"
                                employee.isAdmin == true -> "Admin"
                                employee.isSupervisor == true -> "Supervizor"
                                employee.isAgent == true -> "Agent"
                                else -> "Zaposleni"
                            }
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (employee.active == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
