package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.animateFloat

/**
 * Pulse shimmer kao "skeleton" placeholder. Boji se na osnovu trenutne teme
 * (svetla podloga u light, tamna u dark).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val brush = Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = alpha),
            highlight.copy(alpha = alpha),
            base.copy(alpha = alpha)
        )
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    ShimmerBox(modifier = modifier.fillMaxWidth().height(height), cornerRadius = height / 2)
}

@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(base.copy(alpha = 0.6f))
    )
}

internal val ShimmerNoOp: Color = Color.Transparent
