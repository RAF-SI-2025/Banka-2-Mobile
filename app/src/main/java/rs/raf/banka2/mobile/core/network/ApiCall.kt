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
        if (body == null) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(Unit as T)
        } else {
            ApiResult.Success(body)
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

internal fun parseHttpError(code: Int, rawBody: String?, cause: Throwable? = null): ApiError {
    val parsed = rawBody
        ?.takeIf { it.isNotBlank() }
        ?.let {
            runCatching { errorBodyAdapter.fromJson(it) }.getOrNull()
        }
    val message = parsed?.error
        ?: parsed?.message
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

private fun defaultMessageForCode(code: Int): String = when (code) {
    400 -> "Neispravan zahtev."
    401 -> "Sesija je istekla, prijavi se ponovo."
    403 -> "Nemate dozvolu za ovu akciju."
    404 -> "Trazeni resurs nije pronadjen."
    409 -> "Konflikt: stanje resursa je vec promenjeno."
    422 -> "Podaci nisu prosli validaciju."
    in 500..599 -> "Greska na serveru. Pokusaj ponovo kasnije."
    else -> "Greska ($code)."
}
