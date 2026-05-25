package rs.raf.banka2.mobile.feature.payments.quickapprove

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import java.time.Duration
import java.time.Instant

/**
 * TODO_final Mobile bonus #7 — Quick Approve placeholder.
 *
 * UI prikazuje detalje placanja koje ceka odobrenje + 5min countdown
 * iz notifikacije. Klik na "Odobri" trenutno pokazuje napomenu da
 * BE endpoint jos nije implementiran — u punoj integraciji bi pokrenuo
 * OTP modal + POST /payments/{id}/approve.
 */
@Composable
fun QuickApproveScreen(
    onBack: () -> Unit,
    viewModel: QuickApproveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickApproveEvent.NavigateBack -> onBack()
                is QuickApproveEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BankaScaffold(
        title = "Brzo odobrenje",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        backgroundDecoration = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.error != null) {
                ErrorBanner(state.error)
            }

            CountdownCard(notificationCreatedAt = state.notificationCreatedAt, expired = state.expired)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Detalji placanja",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                DetailRow("Iznos", "${state.amount ?: "—"} ${state.currencyCode ?: ""}")
                DetailRow("Primalac", state.recipientName ?: "—")
                DetailRow("Racun", state.recipientAccount ?: "—")
                DetailRow("Svrha", state.purpose ?: "—")
                DetailRow("Status", state.status ?: "—")
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::cancel,
                    modifier = Modifier.weight(1f),
                ) { Text("Otkazi") }
                Button(
                    onClick = viewModel::onApproveRequested,
                    enabled = !state.expired && !state.loading && state.error == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Odobri")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CountdownCard(notificationCreatedAt: String?, expired: Boolean) {
    if (notificationCreatedAt.isNullOrBlank()) return

    val initialSeconds = remember(notificationCreatedAt) {
        secondsRemainingFrom(notificationCreatedAt)
    }
    var secondsLeft by remember(notificationCreatedAt) {
        mutableIntStateOf(initialSeconds)
    }

    LaunchedEffect(notificationCreatedAt) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft = secondsRemainingFrom(notificationCreatedAt)
        }
    }

    val isExpired = expired || secondsLeft <= 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isExpired) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpired) Icons.Filled.ErrorOutline else Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = if (isExpired) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isExpired) "Link je istekao"
                    else "Preostalo vreme: ${formatMmSs(secondsLeft)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpired) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isExpired)
                        "Brzo odobrenje vazi 5 minuta od trenutka kreiranja notifikacije."
                    else "Brzo odobrenje vazi jos 5 minuta od kreiranja notifikacije.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun secondsRemainingFrom(timestamp: String): Int {
    return runCatching {
        val created = Instant.parse(timestamp)
        val deadline = created.plus(Duration.ofMinutes(5))
        val now = Instant.now()
        val remaining = Duration.between(now, deadline).seconds
        remaining.toInt().coerceAtLeast(0)
    }.getOrDefault(0)
}

private fun formatMmSs(totalSeconds: Int): String {
    val mm = totalSeconds / 60
    val ss = totalSeconds % 60
    return "%02d:%02d".format(mm, ss)
}
