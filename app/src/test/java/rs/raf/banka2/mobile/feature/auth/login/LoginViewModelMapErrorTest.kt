package rs.raf.banka2.mobile.feature.auth.login

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.data.repository.AuthRepository

/**
 * ME-AUTH-04 fix: mapLoginError je sad `internal` za testabilnost.
 *
 * Pokriva 4 input klase:
 *  - null -> null pass-through
 *  - novi SR prefix (BE 12.05.2026 vece) -> 1:1 prosledjivanje
 *  - stari EN prefix (backwards-compat) -> regex parsiranje sekundi i konverzija
 *    u minute zaokruzeno navise
 *  - ostali EN/SR error-i -> 1:1 prosledjivanje
 */
class LoginViewModelMapErrorTest {

    private val viewModel = LoginViewModel(mockk<AuthRepository>(relaxed = true))

    @Test
    fun mapLoginError_null_returnsNull() {
        assertNull(viewModel.mapLoginError(null))
    }

    @Test
    fun mapLoginError_srLockoutPrefix_passesThrough() {
        val sr = "Nalog je privremeno zakljucan. Pokusajte ponovo za 14 min."
        assertEquals(sr, viewModel.mapLoginError(sr))
    }

    @Test
    fun mapLoginError_srLockoutPrefixCaseInsensitive_passesThrough() {
        // ignoreCase prefix match — backend moze poslati i lowercased varijantu.
        val sr = "nalog je privremeno zakljucan na 15 minuta"
        assertEquals(sr, viewModel.mapLoginError(sr))
    }

    @Test
    fun mapLoginError_enLockoutWithSeconds_convertsToMinutesRoundedUp() {
        // 119s -> 2 min (zaokruzeno navise sa (n+59)/60).
        val result = viewModel.mapLoginError("Account temporarily locked. Try again in 119 seconds.")
        assertEquals("Nalog je privremeno zakljucan. Pokusajte ponovo za 2 min.", result)
    }

    @Test
    fun mapLoginError_enLockoutWith60Seconds_returnsOneMinute() {
        val result = viewModel.mapLoginError("Account temporarily locked. Try again in 60 seconds.")
        assertEquals("Nalog je privremeno zakljucan. Pokusajte ponovo za 1 min.", result)
    }

    @Test
    fun mapLoginError_enLockoutWith30Seconds_clampsToMinOneMinute() {
        // (30+59)/60 = 1 -> ali zbog coerceAtLeast(1), tacno 1 min.
        val result = viewModel.mapLoginError("Account temporarily locked. Try again in 30 seconds.")
        assertEquals("Nalog je privremeno zakljucan. Pokusajte ponovo za 1 min.", result)
    }

    @Test
    fun mapLoginError_enLockoutWithoutSeconds_returnsGenericFallback() {
        val result = viewModel.mapLoginError("Account temporarily locked.")
        assertEquals(
            "Nalog je privremeno zakljucan zbog vise neuspesnih pokusaja. Pokusajte kasnije.",
            result
        )
    }

    @Test
    fun mapLoginError_genericSrError_passesThrough() {
        val raw = "Neispravan email ili lozinka."
        assertEquals(raw, viewModel.mapLoginError(raw))
    }

    @Test
    fun mapLoginError_deactivatedAccountSr_passesThrough() {
        val raw = "Nalog je deaktiviran."
        assertEquals(raw, viewModel.mapLoginError(raw))
    }

    @Test
    fun mapLoginError_enLockoutWithSingularSecond_alsoMatches() {
        // Regex "(\\d+)\\s*seconds?" pokriva i "1 second" (rare BE response).
        val result = viewModel.mapLoginError("Account temporarily locked. Try again in 1 second.")
        // 1s -> (1+59)/60 = 1 min.
        assertEquals("Nalog je privremeno zakljucan. Pokusajte ponovo za 1 min.", result)
    }

    @Test
    fun mapLoginError_emptyString_passesThroughEmpty() {
        // Prazna BE poruka — nije lockout, vraca raw.
        val result = viewModel.mapLoginError("")
        assertEquals("", result)
    }

    @Test
    fun mapLoginError_enLockoutEmbeddedInSentence_stillParses() {
        // Spec garantuje prefix start, regex hvata bilo gde u stringu.
        val result = viewModel.mapLoginError("Account temporarily locked due to abuse — Try again in 240 seconds, please.")
        // 240s -> 4 min.
        assertTrue(result?.contains("4 min") == true)
    }
}
