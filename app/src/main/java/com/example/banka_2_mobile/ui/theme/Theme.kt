package com.example.banka_2_mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo200,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet700,
    onSecondaryContainer = Violet200,
    tertiary = Emerald500,
    onTertiary = TextWhite,
    tertiaryContainer = Emerald500.copy(alpha = 0.2f),
    onTertiaryContainer = Emerald400,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCardElevated,
    surfaceContainerHighest = DarkBorder,
    outline = DarkCardBorder,
    outlineVariant = DarkBorder,
    error = Rose500,
    onError = TextWhite,
    errorContainer = Rose500.copy(alpha = 0.15f),
    onErrorContainer = Rose500,
    inverseSurface = LightSurface,
    inverseOnSurface = TextDark,
    inversePrimary = Indigo600,
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet200,
    onSecondaryContainer = Violet700,
    tertiary = Emerald500,
    onTertiary = TextWhite,
    tertiaryContainer = Emerald400.copy(alpha = 0.15f),
    onTertiaryContainer = Emerald500,
    background = LightBg,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = Indigo100.copy(alpha = 0.5f),
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = TextWhite,
    surfaceContainerLow = LightBg,
    surfaceContainer = Indigo100.copy(alpha = 0.3f),
    surfaceContainerHigh = Indigo100.copy(alpha = 0.5f),
    surfaceContainerHighest = Indigo200.copy(alpha = 0.4f),
    outline = Indigo200,
    outlineVariant = Indigo100,
    error = Rose500,
    onError = TextWhite,
    errorContainer = Rose500.copy(alpha = 0.1f),
    onErrorContainer = Rose500,
    inverseSurface = DarkSurface,
    inverseOnSurface = TextWhite,
    inversePrimary = Indigo300,
    scrim = Color(0x66000000)
)

@Composable
fun Banka2MobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
