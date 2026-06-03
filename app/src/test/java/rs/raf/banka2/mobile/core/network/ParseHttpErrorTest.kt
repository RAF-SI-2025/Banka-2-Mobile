package rs.raf.banka2.mobile.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P1-error-contract-1: BE handleri (Employee/Transfer/Payment/EmployeeAuth)
 * vracaju telo `{"error":"Bad Request","message":"<prava poruka>"}` gde je
 * `error` samo generic Spring reason-phrase. Mobile je ranije citao `error`
 * PRVI → uvek "Bad Request"/"Conflict" umesto domenske poruke. Fix: `message`
 * ima prioritet; `error` se koristi samo ako nije generic reason.
 */
class ParseHttpErrorTest {

    @Test
    fun `prefers message over generic error reason phrase`() {
        val body = """{"error":"Bad Request","message":"Racun primaoca ne postoji."}"""

        val result = parseHttpError(400, body)

        assertEquals("Racun primaoca ne postoji.", result.message)
    }

    @Test
    fun `prefers message over generic Conflict reason`() {
        val body = """{"timestamp":"x","status":409,"error":"Conflict","message":"Email vec postoji."}"""

        val result = parseHttpError(409, body)

        assertEquals("Email vec postoji.", result.message)
    }

    @Test
    fun `falls back to error when message missing and error is meaningful`() {
        // Handleri tipa Card/Account vracaju {"error":"<prava poruka>"} bez message.
        val body = """{"error":"Kartica je blokirana."}"""

        val result = parseHttpError(403, body)

        assertEquals("Kartica je blokirana.", result.message)
    }

    @Test
    fun `ignores generic error reason and uses default when message missing`() {
        // error je samo generic reason, message fali → default poruka za kod.
        val body = """{"error":"Bad Request"}"""

        val result = parseHttpError(400, body)

        assertEquals("Neispravan zahtev.", result.message)
    }

    @Test
    fun `empty body uses default message for code`() {
        // 401 sa praznim telom (stari filter put) → default. Sad BE salje JSON,
        // ali fallback i dalje mora biti smislen.
        val result = parseHttpError(401, null)

        assertEquals("Neispravan email ili lozinka.", result.message)
    }

    @Test
    fun `parses standardized 401 message body from security filter`() {
        // P1-error-contract-1 BE strana: filteri sad salju {"message":...} za istek sesije.
        val body = """{"message":"Sesija je istekla ili nije validna. Prijavite se ponovo."}"""

        val result = parseHttpError(401, body)

        assertEquals("Sesija je istekla ili nije validna. Prijavite se ponovo.", result.message)
    }

    // ─── R4-1794: ne-JSON telo (gateway plain-text / HTML) ─────────────────

    @Test
    fun `non-json plain text body is surfaced instead of generic default`() {
        // Envoy/nginx ponekad vrati plain-text razlog (ne JSON). Ranije bi
        // korisnik video samo generic "Greska na serveru."
        val result = parseHttpError(502, "upstream connect error or disconnect")
        assertEquals("upstream connect error or disconnect", result.message)
    }

    @Test
    fun `html body falls back to generic default not markup`() {
        // HTML stranicu NE prikazujemo korisniku — padamo na default.
        val html = "<html><body><h1>502 Bad Gateway</h1></body></html>"
        val result = parseHttpError(502, html)
        assertEquals("Greska na serveru. Pokusaj ponovo kasnije.", result.message)
    }
}
