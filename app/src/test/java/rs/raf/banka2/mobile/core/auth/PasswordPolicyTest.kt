package rs.raf.banka2.mobile.core.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R1-577: Mobile password validacija mora preslikati BE policy
 * (`PasswordResetDto` / `RegisterRequestDto`): 8–32 karaktera + bar 1 malo,
 * 1 veliko slovo, 2 cifre. Ranije je Mobile proveravao samo `length >= 8`,
 * pa je BE vracao 400 na npr. "password" ili predugacku lozinku.
 */
class PasswordPolicyTest {

    @Test
    fun validPassword_passes() {
        // 1 malo, 1 veliko, 2 cifre, 8-32 chars
        assertTrue(PasswordPolicy.isValid("Abcde12f"))
        assertNull(PasswordPolicy.validate("Abcde12f"))
    }

    @Test
    fun tooShort_rejected() {
        assertNotNull(PasswordPolicy.validate("Ab1c"))
        assertFalse(PasswordPolicy.isValid("Ab1c"))
    }

    @Test
    fun tooLong_rejected() {
        // 33 karaktera — preko BE max32
        val pw = "A1" + "a".repeat(31)
        assertNotNull(PasswordPolicy.validate(pw))
        assertFalse(PasswordPolicy.isValid(pw))
    }

    @Test
    fun noDigit_rejected() {
        // dovoljno dugacka, ima malo+veliko, ali nula cifara
        assertNotNull(PasswordPolicy.validate("Abcdefgh"))
    }

    @Test
    fun oneDigitOnly_rejected() {
        // BE trazi BAR DVE cifre
        assertNotNull(PasswordPolicy.validate("Abcdefg1"))
    }

    @Test
    fun noUppercase_rejected() {
        assertNotNull(PasswordPolicy.validate("abcdef12"))
    }

    @Test
    fun noLowercase_rejected() {
        assertNotNull(PasswordPolicy.validate("ABCDEF12"))
    }

    @Test
    fun maxLength32_passes() {
        // tacno 32 karaktera, validno
        val pw = "Ab12" + "c".repeat(28)
        assertEquals32(pw)
        assertNull(PasswordPolicy.validate(pw))
    }

    private fun assertEquals32(pw: String) {
        assertTrue("expected length 32 but was ${pw.length}", pw.length == 32)
    }
}
