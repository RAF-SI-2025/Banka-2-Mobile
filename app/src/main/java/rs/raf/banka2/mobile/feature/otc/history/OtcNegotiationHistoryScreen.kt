package rs.raf.banka2.mobile.feature.otc.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.otchistory.OtcNegotiationHistoryDto

/**
 * B10 / Spec C4 §13 — Istorija OTC pregovora (supervisor/admin only).
 *
 * Filter chip-ovi (Status: ACTIVE/ACCEPTED/DECLINED) + datum opseg.
 * Tap na red expanduje pun lanac kontraponuda (sve iteracije iz log-a).
 */
private val STATUS_OPTIONS = listOf("ALL", "ACTIVE", "ACCEPTED", "DECLINED")
private val STATUS_LABEL_SR: Map<String, String> = mapOf(
    "ALL" to "Svi",
    "ACTIVE" to "Aktivan",
    "ACCEPTED" to "Prihvacen",
    "DECLINED" to "Odbijen"
)

@Composable
fun OtcNegotiationHistoryScreen(
    onBack: () -> Unit,
    viewModel: OtcNegotiationHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Istorija OTC pregovora",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Osvezi", tint = MaterialTheme.colorScheme.onSurface)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterCard(
                state = state,
                onStatus = viewModel::setStatus,
                onDateFrom = viewModel::setDateFrom,
                onDateTo = viewModel::setDateTo,
                onApply = viewModel::applyFilters,
                onReset = viewModel::resetFilters
            )

            if (state.error != null) ErrorBanner(state.error)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().weight(1f, fill = true)
            ) {
                if (state.entries.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Archive,
                            title = if (state.forbidden) "Nemate pristup" else "Nema pregovora",
                            description = if (state.forbidden)
                                "Pregled je dostupan samo supervizorima i administratorima."
                            else
                                "Pokusaj drugacijim filterima."
                        )
                    }
                }
                items(state.entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        expanded = state.expandedNegotiation == entry.negotiationId,
                        chain = state.chain.takeIf { state.expandedNegotiation == entry.negotiationId },
                        chainLoading = state.chainLoading && state.expandedNegotiation == entry.negotiationId,
                        chainError = state.chainError.takeIf { state.expandedNegotiation == entry.negotiationId },
                        onToggle = { viewModel.toggleChain(entry.negotiationId) }
                    )
                }

                if (state.totalPages > 1) {
                    item {
                        PaginationRow(
                            page = state.page,
                            totalPages = state.totalPages,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterCard(
    state: OtcNegotiationHistoryState,
    onStatus: (String?) -> Unit,
    onDateFrom: (String) -> Unit,
    onDateTo: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Filteri", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(STATUS_OPTIONS) { status ->
                val isSelected = (status == "ALL" && state.status == null) || state.status == status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onStatus(if (status == "ALL") null else status) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        STATUS_LABEL_SR[status] ?: status,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = state.dateFrom,
                onValueChange = onDateFrom,
                label = "Od (YYYY-MM-DD)",
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = state.dateTo,
                onValueChange = onDateTo,
                label = "Do (YYYY-MM-DD)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "Resetuj", onClick = onReset, modifier = Modifier.weight(1f), height = 42.dp)
            SecondaryButton(text = "Primeni", onClick = onApply, modifier = Modifier.weight(1f), height = 42.dp)
        }
    }
}

@Composable
private fun EntryRow(
    entry: OtcNegotiationHistoryDto,
    expanded: Boolean,
    chain: List<OtcNegotiationHistoryDto>?,
    chainLoading: Boolean,
    chainError: String?,
    onToggle: () -> Unit
) {
    val accent = when (entry.status) {
        "ACTIVE" -> Color(0xFFF59E0B)
        "ACCEPTED" -> Color(0xFF10B981)
        "DECLINED" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            STATUS_LABEL_SR[entry.status] ?: entry.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        " · Pregovor #${entry.negotiationId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kolicina ${entry.quantity} · cena ${MoneyFormatter.format(entry.pricePerShare, 2)} · premija ${MoneyFormatter.format(entry.premium, 2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Izmenio: ${entry.modifiedByName ?: "—"} · ${entry.createdAt.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Sakrij" else "Prikazi lanac",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            when {
                chainLoading -> Text(
                    "Ucitavanje lanca kontraponuda...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                chainError != null -> Text(
                    chainError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                chain.isNullOrEmpty() -> Text(
                    "Nema dodatnih kontraponuda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> ChainTimeline(chain)
            }
        }
    }
}

@Composable
private fun ChainTimeline(chain: List<OtcNegotiationHistoryDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Lanac kontraponuda (${chain.size} iteracija)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        chain.forEachIndexed { idx, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    "${idx + 1}.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Kol. ${step.quantity} · Cena ${MoneyFormatter.format(step.pricePerShare, 2)} · Prem. ${MoneyFormatter.format(step.premium, 2)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Settlement: ${step.settlementDate ?: "—"} · ${step.modifiedByName ?: "—"} · ${step.createdAt.take(19).replace('T', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginationRow(
    page: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious, enabled = page > 0) { Text("← Prethodna") }
        Text(
            "Strana ${page + 1} / $totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onNext, enabled = page + 1 < totalPages) { Text("Sledeca →") }
    }
}
