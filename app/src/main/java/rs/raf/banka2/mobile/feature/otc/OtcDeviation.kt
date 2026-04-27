package rs.raf.banka2.mobile.feature.otc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Spec Celine 4: aktivne OTC ponude moraju biti vizuelno razlikovane prema
 * odstupanju ponudjene cene od trzisne:
 *   ≤ ±5 %  → zeleno (povoljna)
 *   ±5..±20 % → zuto (neutralna)
 *   > ±20 % → crveno (rizicna)
 */
data class DeviationStyle(
    val percent: Double,
    val tint: Color,
    val label: String
)

@Composable
fun deviationStyle(offered: Double, current: Double?): DeviationStyle? {
    if (current == null || current == 0.0) return null
    val percent = ((offered - current) / current) * 100.0
    val abs = abs(percent)
    val (tint, label) = when {
        abs <= 5.0 -> MaterialTheme.colorScheme.tertiary to "Povoljno"
        abs <= 20.0 -> Color(0xFFEAB308) to "Neutralno"
        else -> MaterialTheme.colorScheme.error to "Rizicno"
    }
    return DeviationStyle(percent = percent, tint = tint, label = label)
}
