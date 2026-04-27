package rs.raf.banka2.mobile.feature.accounts.business

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
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
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.feature.accounts.AccountDetailsViewModel

/**
 * Specijalizovani ekran za poslovni racun. Reuse-uje [AccountDetailsViewModel],
 * ali dodatno prikazuje firmu + sifru delatnosti + spisak ovlascenih lica.
 */
@Composable
fun BusinessAccountDetailsScreen(
    onBack: () -> Unit,
    viewModel: AccountDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Poslovni racun",
        onBack = onBack,
        backgroundDecoration = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.generalError != null) item { ErrorBanner(state.generalError) }
            state.account?.let { acc ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(acc.companyName ?: acc.name ?: "Firma", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(AccountFormatter.formatAccountNumber(acc.accountNumber), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(MoneyFormatter.formatWithCurrency(acc.balance, acc.currency), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Raspolozivo: ${MoneyFormatter.formatWithCurrency(acc.availableBalance, acc.currency)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Podaci o firmi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        KvRow("Naziv firme", acc.companyName ?: "—")
                        KvRow("Sifra delatnosti", acc.activityCode ?: "—")
                        KvRow("Maticni broj", acc.companyRegistrationNumber ?: "—")
                        KvRow("PIB", acc.taxNumber ?: "—")
                        KvRow("Tip racuna", "${acc.accountType ?: "—"} ${acc.accountSubtype.orEmpty()}")
                        KvRow("Valuta", acc.currency ?: "—")
                        KvRow("Status", acc.status ?: "—")
                    }
                }
                item {
                    Text("Ovlascena lica", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
                }
                if (acc.authorizedPersons.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.PersonOutline,
                            title = "Nema ovlascenih lica",
                            description = "Posalji zahtev banci za dodavanje ovlascenog lica."
                        )
                    }
                } else {
                    items(acc.authorizedPersons, key = { it.id ?: it.email.orEmpty().hashCode().toLong() }) { person ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PersonOutline, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Column(modifier = Modifier.padding(start = 8.dp).fillMaxWidth()) {
                                    Text(
                                        listOfNotNull(person.firstName, person.lastName).joinToString(" ").ifBlank { person.email.orEmpty() },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    person.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    person.phoneNumber?.let { Text("Tel: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    person.role?.let { Text("Uloga: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
