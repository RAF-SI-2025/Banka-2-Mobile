package rs.raf.banka2.mobile.feature.securities.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import rs.raf.banka2.mobile.core.ui.theme.Indigo400
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600
import rs.raf.banka2.mobile.data.dto.listing.ListingDailyPriceDto

/**
 * Custom canvas line chart sa gradient fill ispod krive i animacijom path
 * drawing-a (kriva se "iscrtava" levo→desno koristeci [PathMeasure]). Krug na
 * kraju pulsira da privuce paznju na zadnju vrednost.
 */
@Composable
fun PriceChart(
    points: List<ListingDailyPriceDto>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    if (points.size < 2) return
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val lineColor = MaterialTheme.colorScheme.primary

    val drawProgress = remember(points) { Animatable(0f) }
    val fillFade = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        drawProgress.snapTo(0f)
        fillFade.snapTo(0f)
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(points) {
        fillFade.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, delayMillis = 400)
        )
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-alpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val values = points.map { it.resolvedClose }
        val minV = values.minOrNull() ?: return@Canvas
        val maxV = (values.maxOrNull() ?: return@Canvas).coerceAtLeast(minV + 0.0001)
        val range = maxV - minV

        val w = size.width
        val h = size.height
        val stepX = if (points.size > 1) w / (points.size - 1) else w

        val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
        val gridLines = 4
        for (i in 1..gridLines) {
            val y = h * i / (gridLines + 1)
            drawLine(
                color = gridColor.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = gridDash
            )
        }

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { index, dp ->
            val x = stepX * index
            val normalized = ((dp.resolvedClose - minV) / range).toFloat()
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
                    Indigo500.copy(alpha = 0.45f),
                    Violet600.copy(alpha = 0.05f)
                )
            ),
            alpha = fillFade.value
        )

        val measure = PathMeasure().apply { setPath(linePath, false) }
        val totalLength = measure.length
        val animatedPath = Path()
        if (totalLength > 0f) {
            measure.getSegment(
                startDistance = 0f,
                stopDistance = totalLength * drawProgress.value,
                destination = animatedPath,
                startWithMoveTo = true
            )
        }
        drawPath(
            path = animatedPath,
            color = lineColor,
            style = Stroke(width = 3f)
        )

        if (drawProgress.value >= 1f) {
            val lastX = stepX * (points.size - 1)
            val lastY = h - ((values.last() - minV) / range).toFloat() * h
            drawCircle(
                color = Indigo400.copy(alpha = pulseAlpha),
                radius = 12f * pulseScale,
                center = Offset(lastX, lastY)
            )
            drawCircle(color = Indigo400, radius = 6f, center = Offset(lastX, lastY))
        }
    }
}
