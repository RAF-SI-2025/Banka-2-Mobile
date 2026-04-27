package rs.raf.banka2.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import rs.raf.banka2.mobile.core.ui.navigation.AppNavHost
import rs.raf.banka2.mobile.core.ui.theme.BankaTheme
import rs.raf.banka2.mobile.core.ui.theme.DarkBg
import rs.raf.banka2.mobile.core.ui.theme.LightBg
import rs.raf.banka2.mobile.core.ui.theme.ThemeManager
import rs.raf.banka2.mobile.core.ui.theme.ThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mode by themeManager.mode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (mode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val barColor = if (isDark) DarkBg.toArgb() else LightBg.toArgb()
            if (isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(barColor),
                    navigationBarStyle = SystemBarStyle.dark(barColor)
                )
            } else {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(barColor, barColor),
                    navigationBarStyle = SystemBarStyle.light(barColor, barColor)
                )
            }
            BankaTheme(darkTheme = isDark) {
                AppNavHost()
            }
        }
    }
}
