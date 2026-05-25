package rs.raf.banka2.mobile.core.auth

import androidx.compose.ui.graphics.Color

/**
 * TODO_final C2 #3 — TOTP support.
 *
 * Pure helper-i za RFC 6238 TOTP prozor (30s). Paritet sa FE
 * `Banka-2-Frontend/src/components/shared/VerificationModal.tsx`
 * helper-ima `getTotpSecondsLeft`/`getTotpProgressPercent`/
 * `getTotpIndicatorColorClass`.
 *
 * BE `TotpService` verifikuje kod protiv trenutnog 30s prozora,
 * pa FE/Mobile UI pokazuje koliko sekundi ostaje pre nego sto se
 * kod automatski promeni u authenticator app-u.
 *
 * Pure i bez Android API zavisnosti (osim [Color]) — testovi
 * pozivaju [getTotpSecondsLeft] sa fiksnim epoch-om.
 */
object TotpHelpers {

    const val TOTP_PERIOD_SECONDS: Int = 30

    /**
     * Vraca koliko sekundi (1..30) ostaje u trenutnom TOTP prozoru.
     * @param nowMillis epoch ms — testovi prosledjuju fiksne vrednosti.
     */
    fun getTotpSecondsLeft(nowMillis: Long = System.currentTimeMillis()): Int {
        val epochSeconds = nowMillis / 1000L
        val elapsed = (epochSeconds % TOTP_PERIOD_SECONDS).toInt()
        return TOTP_PERIOD_SECONDS - elapsed
    }

    /**
     * 30s → 1.0f, 1s → ~0.033f. Ide nadole kako vreme tece.
     */
    fun getTotpProgressFraction(secondsLeft: Int): Float =
        (secondsLeft.coerceIn(0, TOTP_PERIOD_SECONDS).toFloat()) / TOTP_PERIOD_SECONDS

    /**
     * Boja indikatora po threshold-u:
     *  - secondsLeft <= 3  → crvena (~ FE bg-red-500)
     *  - secondsLeft <= 10 → amber  (~ FE bg-amber-500)
     *  - inace             → emerald (~ FE bg-emerald-500)
     */
    fun getTotpIndicatorColor(secondsLeft: Int): Color = when {
        secondsLeft <= 3 -> Color(0xFFEF4444)
        secondsLeft <= 10 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
}
