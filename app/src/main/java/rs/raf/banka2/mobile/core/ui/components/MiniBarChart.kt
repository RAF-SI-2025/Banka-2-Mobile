package rs.raf.banka2.mobile.core.ui.components

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mini bar chart sa staggered fade-in + grow animacijom.
 * Stubice se redom uvecavaju iz dna sa ease-out efektom.
 */
@Composable
fun MiniBarChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentLast: Color = MaterialTheme.colorScheme.tertiary
) {
    if (values.isEmpty()) return
    val max = values.maxOrNull()?.coerceAtLeast(0.0001) ?: 1.0

    val animatables = remember(values.size) {
        List(values.size) { Animatable(0f) }
    }
    LaunchedEffect(values) {
        animatables.forEachIndexed { index, animatable ->
            launch {
                delay(index * 60L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val gap = 4f
        val barWidth = ((w - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
        values.forEachIndexed { index, value ->
            val targetH = ((value / max).toFloat()).coerceIn(0f, 1f) * h
            val animProgress = animatables.getOrNull(index)?.value ?: 1f
            val barH = targetH * animProgress
            val x = index * (barWidth + gap)
            val y = h - barH
            val isLast = index == values.lastIndex
            val brush = if (isLast) {
                Brush.verticalGradient(listOf(accentLast, accentLast.copy(alpha = 0.55f)), startY = y, endY = h)
            } else {
                val baseAlpha = 0.55f + 0.45f * (index.toFloat() / values.size)
                Brush.verticalGradient(
                    listOf(barColor.copy(alpha = baseAlpha), barColor.copy(alpha = baseAlpha * 0.55f)),
                    startY = y,
                    endY = h
                )
            }
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                alpha = 0.5f + 0.5f * animProgress
            )
        }
    }
}
