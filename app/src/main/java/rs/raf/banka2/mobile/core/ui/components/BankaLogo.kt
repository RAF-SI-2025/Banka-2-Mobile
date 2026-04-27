package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.R

/**
 * In-app brand logo — koristi favicon iz FE-a (`banka_logo.png`).
 * Default render uvija logo u krug sa indigo→violet halo gradijentom.
 */
@Composable
fun BankaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    showHalo: Boolean = true,
    pulse: Boolean = false
) {
    val scale = if (pulse) {
        val infinite = rememberInfiniteTransition(label = "logo-pulse")
        val s by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo-pulse-scale"
        )
        s
    } else 1f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showHalo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.55f),
                                Color(0xFF8B5CF6).copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .alpha(0.85f)
            )
        }
        Image(
            painter = painterResource(id = R.drawable.banka_logo),
            contentDescription = "Banka 2",
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showHalo) size * 0.10f else 0.dp)
                .scale(scale)
        )
    }
}

/**
 * Kompaktni inline logo za top-bar / header ikone.
 */
@Composable
fun BankaLogoCompact(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.banka_logo),
            contentDescription = "Banka 2",
            modifier = Modifier.fillMaxSize().padding(size * 0.08f)
        )
    }
}
