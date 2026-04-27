package rs.raf.banka2.mobile.feature.funds.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600
import rs.raf.banka2.mobile.data.dto.fund.FundPerformancePointDto

@Composable
fun FundPerformanceChart(
    points: List<FundPerformancePointDto>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    if (points.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val drawProgress = remember(points) { Animatable(0f) }
    val fillFade = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        drawProgress.snapTo(0f); fillFade.snapTo(0f)
        drawProgress.animateTo(1f, animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(points) {
        fillFade.animateTo(1f, animationSpec = tween(durationMillis = 700, delayMillis = 400))
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val values = points.map { it.value }
        val minV = values.minOrNull() ?: return@Canvas
        val maxV = (values.maxOrNull() ?: return@Canvas).coerceAtLeast(minV + 0.0001)
        val range = maxV - minV
        val w = size.width
        val h = size.height
        val stepX = if (points.size > 1) w / (points.size - 1) else w

        val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
        for (i in 1..3) {
            val y = h * i / 4f
            drawLine(
                color = gridColor.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = gridDash
            )
        }

        val line = Path()
        val fill = Path()
        points.forEachIndexed { index, point ->
            val x = stepX * index
            val y = h - (((point.value - minV) / range).toFloat() * h)
            if (index == 0) {
                line.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y)
            } else {
                line.lineTo(x, y); fill.lineTo(x, y)
            }
        }
        fill.lineTo(w, h); fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(Indigo500.copy(alpha = 0.45f), Violet600.copy(alpha = 0.05f))),
            alpha = fillFade.value
        )

        val measure = PathMeasure().apply { setPath(line, false) }
        val total = measure.length
        val animatedLine = Path()
        if (total > 0f) {
            measure.getSegment(0f, total * drawProgress.value, animatedLine, true)
        }
        drawPath(animatedLine, color = lineColor, style = Stroke(width = 3f))

        if (drawProgress.value >= 1f) {
            val lastX = stepX * (points.size - 1)
            val lastY = h - ((values.last() - minV) / range).toFloat() * h
            drawCircle(color = lineColor, radius = 6f, center = Offset(lastX, lastY))
        }
    }
}
