package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600

/**
 * Pozadinski "orbs" — 3 obojene lopte koje plivaju na ekranu.
 * Daju aplikaciji isti banking glow kao web verzija.
 *
 * Stavi je kao prvu decu Box-a sa fillMaxSize i pusti da deca posle nje
 * sede iznad nje. Ne troši mrežu, samo Compose animaciju.
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Orb(
            color = Indigo500.copy(alpha = 0.30f),
            offsetX = -120f + phase1 * 80f,
            offsetY = -160f + phase2 * 60f,
            sizeDp = 320f
        )
        Orb(
            color = Violet600.copy(alpha = 0.28f),
            offsetX = 200f - phase2 * 50f,
            offsetY = 220f - phase1 * 90f,
            sizeDp = 380f
        )
        Orb(
            color = Color(0xFF06B6D4).copy(alpha = 0.18f),
            offsetX = -50f + phase1 * 30f,
            offsetY = 480f - phase2 * 70f,
            sizeDp = 260f
        )
    }
}

@Composable
private fun Orb(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    sizeDp: Float
) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(offsetX.toInt(), offsetY.toInt())
                }
            }
            .size(sizeDp.dp)
            .blur(80.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color, Color.Transparent)
                )
            )
    )
}
