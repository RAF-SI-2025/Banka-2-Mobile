package rs.raf.banka2.mobile.core.ui.theme

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { System, Light, Dark }

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(value: ThemeMode) {
        _mode.value = value
        scope.launch { prefs.edit { putString(KEY_MODE, value.name) } }
    }

    fun toggleLightDark() {
        setMode(if (_mode.value == ThemeMode.Dark) ThemeMode.Light else ThemeMode.Dark)
    }

    private fun read(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.Dark.name) ?: ThemeMode.Dark.name)
    }.getOrDefault(ThemeMode.Dark)

    private companion object {
        const val FILE_NAME = "banka2_theme"
        const val KEY_MODE = "mode"
    }
}
