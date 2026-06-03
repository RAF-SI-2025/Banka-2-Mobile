package rs.raf.banka2.mobile.core.auth

/**
 * R1-577: ogledalo BE password policy-ja
 * (`PasswordResetDto` / `RegisterRequestDto`):
 *   - 8–32 karaktera (`@Size(min = 8, max = 32)`)
 *   - bar 1 malo slovo, bar 1 veliko slovo, bar 2 cifre
 *     (`^(?=.*[a-z])(?=.*[A-Z])(?=(?:.*[0-9]){2,}).*$`)
 *
 * Ranije je Mobile (Reset / Activate) proveravao samo `length < 8`, pa je
 * korisnik sa npr. "password" / "OVOJEPREDUGACKALOZINKABEZCIFARA..." video
 * generic BE 400 umesto trenutne klijentske poruke. Ovde validiramo isto
 * sto BE, sa srpskom porukom.
 */
object PasswordPolicy {

    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 32

    /**
     * Vraca `null` ako je lozinka validna, inace srpsku poruku o gresci.
     */
    fun validate(password: String): String? {
        if (password.length < MIN_LENGTH) {
            return "Lozinka mora imati bar $MIN_LENGTH karaktera."
        }
        if (password.length > MAX_LENGTH) {
            return "Lozinka sme imati najvise $MAX_LENGTH karaktera."
        }
        if (password.none { it in 'a'..'z' }) {
            return "Lozinka mora sadrzati bar jedno malo slovo."
        }
        if (password.none { it in 'A'..'Z' }) {
            return "Lozinka mora sadrzati bar jedno veliko slovo."
        }
        if (password.count { it.isDigit() } < 2) {
            return "Lozinka mora sadrzati bar dve cifre."
        }
        return null
    }

    fun isValid(password: String): Boolean = validate(password) == null
}
