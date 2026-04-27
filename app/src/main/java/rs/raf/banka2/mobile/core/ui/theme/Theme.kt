package rs.raf.banka2.mobile.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Banka 2 ima dve teme: dark (default, slaze se sa web aplikacijom) i light.
 * Korisnik moze prebaciti rucno preko TopBar-a u HomeScreen actions; preferenca
 * se cuva kroz [ThemeManager] (SharedPreferences).
 */
@Composable
fun BankaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BankaTypography,
        content = content
    )
}

private val DarkColors = darkColorScheme(
    primary = Indigo500,
    onPrimary = TextWhite,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo300,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet700,
    onSecondaryContainer = Violet400,
    tertiary = Emerald500,
    onTertiary = TextWhite,
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
    surfaceContainerHighest = DarkCardElevated,
    outline = DarkCardBorder,
    outlineVariant = DarkBorder,
    error = Rose500,
    onError = TextWhite,
    errorContainer = DarkCard,
    onErrorContainer = Rose400,
    inversePrimary = Indigo300,
    scrim = DarkBg
)

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = TextWhite,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary = Violet600,
    onSecondary = TextWhite,
    secondaryContainer = Violet200,
    onSecondaryContainer = Violet700,
    tertiary = Emerald600,
    onTertiary = TextWhite,
    background = LightBg,
    onBackground = TextDarkPrimary,
    surface = LightSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = LightCardElevated,
    onSurfaceVariant = TextDarkMuted,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBg,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCardElevated,
    surfaceContainerHighest = LightCardElevated,
    outline = LightCardBorder,
    outlineVariant = LightBorder,
    error = Rose500,
    onError = TextWhite,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    inversePrimary = Indigo300,
    scrim = TextDarkPrimary
)
