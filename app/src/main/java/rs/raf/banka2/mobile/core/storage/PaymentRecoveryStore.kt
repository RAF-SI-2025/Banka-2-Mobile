package rs.raf.banka2.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ME-08 fix: ekvivalent FE `sessionStorage` recovery-a za inter-bank 2PC placanja.
 * Cuva aktivni `transactionId` lokalno pa kad korisnik napusti screen / vrati se
 * (process death, app background-restore), ViewModel moze da resume-uje polling.
 *
 * NIJE encrypted — transactionId nije sensitive. Plain SharedPreferences je dovoljan.
 * Cleanup: po terminal status (COMMITTED/ABORTED/STUCK) ili rucno `clearActive2PC()`.
 *
 * Paritet sa FE NewPaymentPage `sessionStorage[INTERBANK_ACTIVE_TX_KEY]`.
 */
@Singleton
class PaymentRecoveryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Cuva aktivni 2PC transactionId za resume polling. */
    suspend fun saveActive2PC(transactionId: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_ACTIVE_TX, transactionId) }
    }

    /** Vraca cuvan transactionId ili null ako nema aktivnog placanja. */
    suspend fun getActive2PC(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACTIVE_TX, null)
    }

    /** Brise cuvani transactionId — zove se kad polling dosegne terminal status. */
    suspend fun clearActive2PC(): Unit = withContext(Dispatchers.IO) {
        prefs.edit { remove(KEY_ACTIVE_TX) }
    }

    private companion object {
        const val FILE_NAME = "banka2_payment_recovery"
        const val KEY_ACTIVE_TX = "active_2pc_tx"
    }
}
