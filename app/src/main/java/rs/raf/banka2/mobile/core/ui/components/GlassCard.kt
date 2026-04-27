package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import rs.raf.banka2.mobile.core.ui.theme.GlassWhite
import rs.raf.banka2.mobile.core.ui.theme.Indigo500

/**
 * Reusable glassmorphism kartica koja se ponavlja na svim ekranima.
 * Koristi `surfaceContainer` boju iz teme — u dark modu izgleda kao staklo,
 * u light modu kao bela kartica sa blagom senkom.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    elevation: Dp = 8.dp,
    borderBrush: Brush? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val resolvedBorder = borderBrush
        ?: Brush.linearGradient(
            colors = listOf(GlassWhite, Color.Transparent)
        )

    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Indigo500.copy(alpha = 0.1f),
                spotColor = Indigo500.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(width = 1.dp, brush = resolvedBorder, shape = shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
