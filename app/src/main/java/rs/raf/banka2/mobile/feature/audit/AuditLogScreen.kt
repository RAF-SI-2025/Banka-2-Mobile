package rs.raf.banka2.mobile.feature.audit

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
import androidx.compose.material.icons.filled.Description
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
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.AppTextField
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.EmptyState
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.SecondaryButton
import rs.raf.banka2.mobile.data.dto.audit.AuditActionTypes
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto

/**
 * B7 / Spec C3 §69 — Audit log portal (supervisor/admin only).
 *
 * Sastoji se od:
 *  - 4 filter polja: tip akcije (chip toggle), email aktera, datum od/do
 *  - Lista zapisa sa Badge varijantom po tipu akcije i actor/target info
 *  - Paginacija (prethodna/naredna)
 */
@Composable
fun AuditLogScreen(
    onBack: () -> Unit,
    viewModel: AuditLogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BankaScaffold(
        title = "Audit log",
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
                onActionType = viewModel::setActionType,
                onActorEmail = viewModel::setActorEmail,
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
                if (state.logs.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Description,
                            title = if (state.forbidden) "Nemate pristup" else "Nema zapisa",
                            description = if (state.forbidden)
                                "Audit log je dostupan samo supervizorima i administratorima."
                            else
                                "Pokusaj drugacijim filterima."
                        )
                    }
                }
                items(state.logs, key = { it.id }) { log -> LogRow(log) }

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
    state: AuditLogState,
    onActionType: (String?) -> Unit,
    onActorEmail: (String) -> Unit,
    onDateFrom: (String) -> Unit,
    onDateTo: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Filteri", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        // Tip akcije — chip toggle (paritet sa FE Audit Log filterom)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                ActionChip(
                    label = "Svi",
                    isSelected = state.actionType == null,
                    onClick = { onActionType(null) }
                )
            }
            items(AuditActionTypes.ALL) { actionType ->
                ActionChip(
                    label = AuditActionTypes.label(actionType),
                    isSelected = state.actionType == actionType,
                    onClick = { onActionType(actionType) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.actorEmail,
            onValueChange = onActorEmail,
            label = "Akter (email)",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
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
            SecondaryButton(
                text = "Resetuj",
                onClick = onReset,
                modifier = Modifier.weight(1f),
                height = 42.dp
            )
            SecondaryButton(
                text = "Primeni",
                onClick = onApply,
                modifier = Modifier.weight(1f),
                height = 42.dp
            )
        }
    }
}

@Composable
private fun ActionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun LogRow(log: AuditLogDto) {
    val accent = when (log.actionType) {
        AuditActionTypes.ORDER_APPROVED, AuditActionTypes.TAX_RUN_TRIGGERED -> Color(0xFF10B981)
        AuditActionTypes.ORDER_DECLINED -> Color(0xFFEF4444)
        AuditActionTypes.LIMIT_CHANGED, AuditActionTypes.USED_LIMIT_RESET -> Color(0xFFF59E0B)
        AuditActionTypes.PERMISSIONS_CHANGED -> Color(0xFF8B5CF6)
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    AuditActionTypes.label(log.actionType),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                log.createdAt.take(19).replace('T', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            log.actorName ?: log.actorEmail ?: "Akter ID: ${log.actorId ?: "—"}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!log.targetType.isNullOrBlank()) {
            Text(
                "Cilj: ${log.targetType}${log.targetId?.let { " #$it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!log.oldValue.isNullOrBlank() || !log.newValue.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Pre: ${log.oldValue ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Posle: ${log.newValue ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
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
