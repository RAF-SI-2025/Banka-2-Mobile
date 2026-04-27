package rs.raf.banka2.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistuje access + refresh token u EncryptedSharedPreferences-u (AES256_GCM).
 * Drzimo i poslednji email da splash ne mora da decode-uje JWT za inicijalni
 * fetch permisija.
 *
 * Sve operacije idu kroz IO dispatcher — pozivamo iz suspend fun-a.
 */
@Singleton
class AuthStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = createPrefs(context)
    private val accessFlow = MutableStateFlow(prefs.getString(KEY_ACCESS, null))
    private val refreshFlow = MutableStateFlow(prefs.getString(KEY_REFRESH, null))
    private val emailFlow = MutableStateFlow(prefs.getString(KEY_EMAIL, null))

    val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(prefs.getString(KEY_ACCESS, null) != null).asStateFlow()
    val email: StateFlow<String?> = emailFlow.asStateFlow()

    suspend fun accessToken(): String? = withContext(Dispatchers.IO) { accessFlow.value }

    suspend fun refreshToken(): String? = withContext(Dispatchers.IO) { refreshFlow.value }

    suspend fun savedEmail(): String? = withContext(Dispatchers.IO) { emailFlow.value }

    suspend fun saveTokens(access: String, refresh: String) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(KEY_ACCESS, access)
            putString(KEY_REFRESH, refresh)
        }
        accessFlow.value = access
        refreshFlow.value = refresh
    }

    suspend fun saveEmail(value: String) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_EMAIL, value) }
        emailFlow.value = value
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
        accessFlow.value = null
        refreshFlow.value = null
        emailFlow.value = null
    }

    private fun createPrefs(context: Context): SharedPreferences {
        // Ako Keystore nije dostupan (npr. neki rooted emulator-i), pad-uj
        // na obicne SharedPreferences-e umesto crash-a. Token na uredjaju
        // bez Keystore-a je svakako kompromitovan.
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val FILE_NAME = "banka2_secure_auth"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EMAIL = "email"
    }
}
