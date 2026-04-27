package rs.raf.banka2.mobile.core.network

/**
 * Uniformni rezultat svakog API poziva. ViewModeli rade pattern-match
 * na ova tri stanja, tako da nigde u UI sloju ne postoji try/catch.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
}

/**
 * Strukturirana greska iz mreze. `httpCode == null` znaci da nikad nismo
 * dosli do servera (timeout, no network, parse fail).
 */
data class ApiError(
    val httpCode: Int?,
    val message: String,
    val kind: Kind,
    val cause: Throwable? = null
) {
    enum class Kind {
        Network,        // bez veze, timeout
        Unauthorized,   // 401 — refresh nije uspeo
        Forbidden,      // 403
        NotFound,       // 404
        Conflict,       // 409
        Validation,     // 400, 422
        Server,         // 5xx
        Unknown
    }
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

/**
 * Lift `ApiResult<T>` na `ApiResult<R>` cuvajuci Failure/Loading granu.
 * Koristi se kada repository zeli da skrati API odgovor (npr. PageResponse → List).
 */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
    ApiResult.Loading -> ApiResult.Loading
}
