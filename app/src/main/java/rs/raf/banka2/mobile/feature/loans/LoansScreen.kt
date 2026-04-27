package rs.raf.banka2.mobile.feature.loans

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
import androidx.compose.material.icons.filled.AccountBalance
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
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton

@Composable
fun LoansScreen(
    onBack: () -> Unit,
    onApply: () -> Unit,
    onLoanClick: (Long) -> Unit,
    viewModel: LoansViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Krediti",
        onBack = onBack,
        actions = {
            IconButton(onClick = onApply) {
                Icon(Icons.Filled.Add, contentDescription = "Novi zahtev", tint = MaterialTheme.colorScheme.primary)
            }
        },
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
            if (state.error != null) item { ErrorBanner(state.error) }
            if (state.loans.isEmpty() && state.applications.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Filled.AccountBalance,
                        title = "Nema kredita",
                        description = "Posalji zahtev za kredit i banka ce ga pregledati."
                    )
                }
            }
            if (state.loans.isNotEmpty()) {
                item {
                    Text("Aktivni krediti", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                items(state.loans, key = { it.id }) { loan ->
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onLoanClick(loan.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${loan.loanType ?: "Kredit"} · ${loan.status ?: "—"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Glavnica ${MoneyFormatter.format(loan.amount)} · Rata ${loan.monthlyInstallment?.let { MoneyFormatter.format(it) } ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Sledeca rata: ${DateFormatter.formatDate(loan.nextInstallmentDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Saldo:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    MoneyFormatter.format(loan.balance ?: 0.0),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(
                            text = "Prevremena otplata",
                            onClick = { viewModel.earlyRepay(loan.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (state.applications.isNotEmpty()) {
                item {
                    Text("Moji zahtevi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                items(state.applications, key = { it.id }) { app ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Zahtev #${app.id} · ${app.status ?: "—"}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Iznos: ${MoneyFormatter.format(app.amount ?: 0.0)} · ${app.durationMonths ?: "?"} meseci",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
