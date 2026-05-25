package rs.raf.banka2.mobile.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.ui.theme.Indigo400
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeHistoryPointDto

/**
 * Mobile-bonus #5: kompaktan sparkline chart za 1-mesec istoriju deviznog
 * kursa. Identicna konstrukcija kao [PriceChart] ali bez animacije
 * (sparkline je read-only mini prikaz). Min/max markeri za citljivost.
 */
@Composable
fun ExchangeRateHistoryChart(
    points: List<ExchangeHistoryPointDto>,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp
) {
    if (points.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val values = points.map { it.rate }
        val minV = values.minOrNull() ?: return@Canvas
        val maxV = (values.maxOrNull() ?: return@Canvas).coerceAtLeast(minV + 0.0001)
        val range = maxV - minV

        val w = size.width
        val h = size.height
        val stepX = if (points.size > 1) w / (points.size - 1) else w

        // jedna srednja horizontalna grid linija
        drawLine(
            color = gridColor.copy(alpha = 0.35f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1f
        )

        val linePath = Path()
        val fillPath = Path()
        var minIndex = 0
        var maxIndex = 0
        values.forEachIndexed { idx, v ->
            if (v < values[minIndex]) minIndex = idx
            if (v > values[maxIndex]) maxIndex = idx
        }
        points.forEachIndexed { index, p ->
            val x = stepX * index
            val normalized = ((p.rate - minV) / range).toFloat()
            val y = h - (normalized * h)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Indigo500.copy(alpha = 0.35f),
                    Violet600.copy(alpha = 0.04f)
                )
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5f)
        )

        // min/max markeri
        val minX = stepX * minIndex
        val minY = h - ((values[minIndex] - minV) / range).toFloat() * h
        val maxX = stepX * maxIndex
        val maxY = h - ((values[maxIndex] - minV) / range).toFloat() * h
        drawCircle(color = Indigo400, radius = 4f, center = Offset(minX, minY))
        drawCircle(color = Indigo400, radius = 4f, center = Offset(maxX, maxY))
    }
}
