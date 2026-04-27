package rs.raf.banka2.mobile.core.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import rs.raf.banka2.mobile.BuildConfig
import rs.raf.banka2.mobile.core.di.NetworkModule
import rs.raf.banka2.mobile.core.storage.AuthStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * OkHttp [Authenticator] se okida samo kad server vrati 401 za vec autentifikovani zahtev.
 *
 * Logika:
 *  1. Ako vec ima novi token (drugi paralelni zahtev je refreshovao), retry-uj sa njim
 *  2. Inace pozovi `/auth/refresh` sa refreshTokenom
 *  3. Sacuvaj novi par tokena, retry zahtev sa novim accessom
 *  4. Ako refresh padne — obrisi sesiju i pusti 401 da prodje (UI ce navigirati na login)
 *
 * Mutex stiti od race condition-a: vise paralelnih zahteva koji vrate 401
 * ce dobiti isti retry sa istim novim tokenom umesto da svaki pokusa refresh.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val authStore: AuthStore,
    /**
     * Refresh klijent je odvojen od regular klijenta (drugi @Named bind),
     * tako da nema ciklusa: regular client → authenticator → refresh client.
     */
    @Named(NetworkModule.REFRESH_CLIENT) private val refreshClient: OkHttpClient
) : Authenticator {

    private val refreshMutex = Mutex()
    private val moshi = Moshi.Builder().build()
    private val refreshAdapter = moshi.adapter(RefreshResponse::class.java)
    private val refreshRequestAdapter = moshi.adapter(RefreshRequest::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        // Ne ulancavaj refresh-evo ako je sam refresh vratio 401
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) {
            return null
        }
        if (response.priorResponseCount() >= MAX_RETRIES) return null

        return runBlocking {
            refreshMutex.withLock {
                val savedAccess = authStore.accessToken()
                val sentAccess = response.request.header("Authorization")?.removePrefix("Bearer ")

                // Drugi paralelni zahtev je vec refreshovao — koristi taj
                if (!savedAccess.isNullOrBlank() && savedAccess != sentAccess) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $savedAccess")
                        .build()
                }

                val refreshToken = authStore.refreshToken() ?: return@withLock null
                val newTokens = doRefresh(refreshToken) ?: run {
                    Timber.w("Refresh failed — clearing session")
                    authStore.clear()
                    return@withLock null
                }
                authStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            }
        }
    }

    private fun doRefresh(refreshToken: String): RefreshResponse? = runCatching {
        val body = refreshRequestAdapter.toJson(RefreshRequest(refreshToken))
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}auth/refresh")
            .post(body)
            .build()
        refreshClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            resp.body?.string()?.let { refreshAdapter.fromJson(it) }
        }
    }.onFailure { Timber.e(it, "Refresh call threw") }.getOrNull()

    @JsonClass(generateAdapter = true)
    internal data class RefreshRequest(val refreshToken: String)

    @JsonClass(generateAdapter = true)
    internal data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String
    )

    private companion object {
        const val MAX_RETRIES = 1
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

private fun Response.priorResponseCount(): Int {
    var count = 0
    var current: Response? = this.priorResponse
    while (current != null) {
        count++
        current = current.priorResponse
    }
    return count
}
