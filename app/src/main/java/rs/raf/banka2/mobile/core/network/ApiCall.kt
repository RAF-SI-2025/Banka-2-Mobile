package rs.raf.banka2.mobile.core.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Wrapper za Retrofit `suspend fun` pozive koji vracaju [Response] ili telo.
 * Hvata mreznu/parser gresku i pretvara je u [ApiError].
 */
suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> = try {
    val response = block()
    if (response.isSuccessful) {
        val body = response.body()
        when {
            body != null -> ApiResult.Success(body)
            // R1 mob4: prazno 2xx telo. Za `Response<Unit>` Retrofit-ov Unit
            // konverter vraca `Unit` (NE null), pa null-telo OVDE znaci da je
            // pozivalac ocekivao TIPIZOVANO telo (npr. DTO) koje nije stiglo.
            // Ranije smo radili `Unit as T` — to bi pri prvom koriscenju vrednosti
            // kao DTO bacilo `ClassCastException` daleko od mesta greske. Sada to
            // tretiramo kao gresku kontrakta (prazno telo gde se ocekuje objekat),
            // OSIM ako je status 204 No Content (legitimno prazno za void akcije).
            response.code() == 204 -> {
                @Suppress("UNCHECKED_CAST")
                ApiResult.Success(Unit as T)
            }
            else -> ApiResult.Failure(
                ApiError(
                    httpCode = response.code(),
                    message = "Server je vratio prazan odgovor.",
                    kind = ApiError.Kind.Server
                )
            )
        }
    } else {
        ApiResult.Failure(parseHttpError(response.code(), response.errorBody()?.string()))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Failure(parseHttpError(e.code(), e.response()?.errorBody()?.string(), e))
} catch (e: IOException) {
    Timber.w(e, "Network failure")
    ApiResult.Failure(
        ApiError(
            httpCode = null,
            message = "Nema veze sa serverom. Proveri internet i pokusaj ponovo.",
            kind = ApiError.Kind.Network,
            cause = e
        )
    )
} catch (e: Throwable) {
    Timber.e(e, "Unexpected API failure")
    ApiResult.Failure(
        ApiError(
            httpCode = null,
            message = e.message ?: "Nepoznata greska",
            kind = ApiError.Kind.Unknown,
            cause = e
        )
    )
}

@JsonClass(generateAdapter = true)
internal data class ServerErrorBody(
    val error: String? = null,
    val message: String? = null,
    val timestamp: String? = null,
    val status: Int? = null
)

private val errorBodyAdapter: JsonAdapter<ServerErrorBody> by lazy {
    Moshi.Builder().build().adapter(ServerErrorBody::class.java)
}

/**
 * Generic Spring status reason-phrase-ovi koje neki BE handleri (Employee/Transfer/
 * Payment/EmployeeAuth) stavljaju u `error` polje (`error=status.getReasonPhrase()`),
 * dok je prava poruka u `message`. Ako `error` sadrzi samo ovakav generic reason,
 * preskacemo ga u korist `message`-a (paritet sa FE `getErrorMessage`).
 */
private val GENERIC_SPRING_REASONS = setOf(
    "Bad Request", "Unauthorized", "Forbidden", "Not Found",
    "Conflict", "Internal Server Error", "Gone", "Locked",
    "Not Implemented", "Bad Gateway", "Unprocessable Entity"
)

internal fun parseHttpError(code: Int, rawBody: String?, cause: Throwable? = null): ApiError {
    val parsed = rawBody
        ?.takeIf { it.isNotBlank() }
        ?.let {
            runCatching { errorBodyAdapter.fromJson(it) }.getOrNull()
        }
    // P1-error-contract-1: `message` ima prioritet nad `error`. Vise BE handlera
    // popunjavaju OBA polja gde je `error=reasonPhrase` ("Bad Request"/"Conflict") a
    // `message`=stvarna poruka — ako bismo citali `error` prvi, korisnik bi uvek video
    // generic status reason umesto domenske poruke. `error` se koristi SAMO kad
    // `message` fali a `error` nije generic Spring reason.
    val messageField = parsed?.message?.takeIf { it.isNotBlank() }
    val errorField = parsed?.error?.takeIf { it.isNotBlank() && it !in GENERIC_SPRING_REASONS }
    // R4-1794: ako telo NIJE JSON (gateway HTML 502 / Envoy plain-text), `parsed`
    // je null pa bismo pali na generic `defaultMessageForCode`. Pre toga pokusaj
    // da izvuces citljiv plain-text iz sirovog tela (HTML se preskace — ne zelimo
    // da korisniku prikazujemo markup).
    val rawFallback = rawBody
        ?.takeIf { parsed == null }
        ?.let { plainTextSnippet(it) }
    val message = messageField
        ?: errorField
        ?: rawFallback
        ?: defaultMessageForCode(code)
    val kind = when (code) {
        in 400..400 -> ApiError.Kind.Validation
        401 -> ApiError.Kind.Unauthorized
        403 -> ApiError.Kind.Forbidden
        404 -> ApiError.Kind.NotFound
        409 -> ApiError.Kind.Conflict
        422 -> ApiError.Kind.Validation
        in 500..599 -> ApiError.Kind.Server
        else -> ApiError.Kind.Unknown
    }
    return ApiError(httpCode = code, message = message, kind = kind, cause = cause)
}

/**
 * R4-1794: izvlaci kratak citljiv plain-text iz ne-JSON tela. Vraca null za
 * HTML (markup ne prikazujemo) ili previse dugacak/prazan tekst.
 */
private fun plainTextSnippet(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    // HTML/XML body (npr. nginx/Envoy 502 stranica) — ne prikazuj markup.
    if (trimmed.startsWith("<")) return null
    if (trimmed.length > 200) return null
    return trimmed
}

private fun defaultMessageForCode(code: Int): String = when (code) {
    400 -> "Neispravan zahtev."
    // 12.05.2026 vece (Bug T1-001/T1-012): BE sad vraca 401 i za pogresne
    // kredencijale (ne samo expired session). Default je generic poruka koja
    // pokriva oba slucaja — login screen mapira BE message ako postoji
    // (parsed?.message u parseHttpError uvek pobedjuje default).
    401 -> "Neispravan email ili lozinka."
    403 -> "Nemate dozvolu za ovu akciju."
    404 -> "Trazeni resurs nije pronadjen."
    409 -> "Konflikt: stanje resursa je vec promenjeno."
    422 -> "Podaci nisu prosli validaciju."
    in 500..599 -> "Greska na serveru. Pokusaj ponovo kasnije."
    else -> "Greska ($code)."
}
