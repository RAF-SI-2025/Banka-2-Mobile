package rs.raf.banka2.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuva "Discord-style" lastEntranceTimestamp za OTC tabove. Spec Celina 4
 * (Nova) §2030-2090: pregovori izmenjeni od strane drugog korisnika nakon
 * korisnikove poslednje posete tabu se prikazuju kao "neprocitani".
 *
 * Kljucevi: `otc:lastEntrance:{userId}:{scope}` gde je scope "intra" ili "inter".
 * SharedPreferences (ne EncryptedSharedPreferences) je dovoljan jer ovde nema
 * tajnih podataka — samo timestamp.
 */
@Singleton
class OtcStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastEntrance(userId: Long, scope: String): Long =
        prefs.getLong(key(userId, scope), 0L)

    fun markEntrance(userId: Long, scope: String, timestampMs: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(key(userId, scope), timestampMs) }
    }

    private fun key(userId: Long, scope: String): String = "otc:lastEntrance:$userId:$scope"

    private companion object {
        const val PREFS_NAME = "banka2_otc_state"
    }
}
