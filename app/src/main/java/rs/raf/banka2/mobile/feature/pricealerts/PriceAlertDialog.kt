package rs.raf.banka2.mobile.feature.pricealerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertCondition

/**
 * [FE2 Mobile port — PriceAlertDialog] Forma za kreiranje cenovnog alarma.
 *
 * Pozivamo iz SecuritiesDetailsScreen ("Postavi cenovni alarm" dugme).
 * Dva radio kartice ABOVE/BELOW + threshold input + live procenat odstupanja
 * od trenutne cene + Zod-like FE validacija.
 */
@Composable
fun PriceAlertDialog(
    listingId: Long,
    ticker: String,
    currentPrice: Double?,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: PriceAlertDialogViewModel = hiltViewModel(),
) {
    LaunchedEffect(listingId) {
        viewModel.reset(listingId, ticker, currentPrice)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdAlert) {
        if (state.createdAlert != null) {
            kotlinx.coroutines.delay(800)
            onCreated()
        }
    }

    val percentDiff = PriceAlertDialogViewModel.percentDifference(state.threshold, state.currentPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cenovni alarm — $ticker") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentPrice != null) {
                    Text(
                        "Trenutna cena: ${"%.4f".format(currentPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ABOVE / BELOW radio kartice
                Text(
                    "Uslov:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                ConditionCard(
                    condition = PriceAlertCondition.ABOVE,
                    selected = state.condition == PriceAlertCondition.ABOVE,
                    icon = Icons.Filled.TrendingUp,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = "Iznad praga",
                    description = "Obavesti me kad cena dostigne ili predje prag.",
                    onSelect = { viewModel.setCondition(PriceAlertCondition.ABOVE) },
                )
                Spacer(Modifier.height(6.dp))
                ConditionCard(
                    condition = PriceAlertCondition.BELOW,
                    selected = state.condition == PriceAlertCondition.BELOW,
                    icon = Icons.Filled.TrendingDown,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = "Ispod praga",
                    description = "Obavesti me kad cena padne do ili ispod praga.",
                    onSelect = { viewModel.setCondition(PriceAlertCondition.BELOW) },
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.thresholdText,
                    onValueChange = viewModel::setThresholdText,
                    label = { Text("Cenovni prag") },
                    placeholder = { Text("npr. 150.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (percentDiff != null) {
                    Spacer(Modifier.height(4.dp))
                    val sign = if (percentDiff >= 0) "+" else ""
                    val color = if (percentDiff >= 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
                    Text(
                        "Odstupanje od trenutne cene: $sign${"%.2f".format(percentDiff)} %",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                }
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state.createdAlert != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Alarm kreiran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::submit,
                enabled = !state.submitting && state.threshold != null,
            ) {
                Text(if (state.submitting) "Slanje..." else "Postavi alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Otkazi") }
        },
    )
}

@Composable
private fun ConditionCard(
    condition: PriceAlertCondition,
    selected: Boolean,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onSelect: () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER") val unused = condition
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.size(8.dp))
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

