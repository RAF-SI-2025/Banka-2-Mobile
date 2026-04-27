package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.ui.theme.Indigo500
import rs.raf.banka2.mobile.core.ui.theme.Violet600

/**
 * Primarno dugme sa indigo→violet gradijentom. Loading prikazuje spinner umesto teksta.
 *
 * Za sekundarna dugmad koristi [SecondaryButton]; za destruktivna [DangerButton].
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    height: Dp = 52.dp
) {
    val brush = Brush.linearGradient(listOf(Indigo500, Violet600))
    GradientButton(
        modifier = modifier,
        text = text,
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        brush = brush,
        height = height
    )
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    height: Dp = 52.dp
) {
    val brush = Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFB91C1C)))
    GradientButton(
        modifier = modifier,
        text = text,
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        brush = brush,
        height = height
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    height: Dp = 52.dp
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .height(height)
            .let { mod ->
                if (enabled && !loading) mod.clickable(onClick = onClick) else mod
            },
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon, contentColor = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    leadingIcon: ImageVector?,
    brush: Brush,
    height: Dp
) {
    val shape = RoundedCornerShape(14.dp)
    val alpha = if (enabled && !loading) 1f else 0.55f
    Box(
        modifier = modifier
            .height(height)
            .alpha(alpha)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = Indigo500.copy(alpha = 0.4f),
                spotColor = Violet600.copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(brush)
            .let { mod ->
                if (enabled && !loading) mod.clickable(onClick = onClick) else mod
            },
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon, contentColor = Color.White)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    leadingIcon: ImageVector?,
    contentColor: Color
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
