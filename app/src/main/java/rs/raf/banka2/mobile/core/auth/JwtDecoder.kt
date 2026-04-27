package rs.raf.banka2.mobile.core.auth

import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber

/**
 * Minimalni JWT parser — citamo samo `sub` (email) i `role`.
 * Backend ne stavlja permisije u token, te se one fetchuju zasebno.
 */
object JwtDecoder {

    @JsonClass(generateAdapter = true)
    data class Payload(
        val sub: String? = null,
        val role: String? = null,
        val active: Boolean? = null,
        val exp: Long? = null
    )

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter: JsonAdapter<Payload> = moshi.adapter(Payload::class.java)

    fun decode(token: String): Payload? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        return runCatching {
            val raw = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            adapter.fromJson(String(raw, Charsets.UTF_8))
        }.onFailure { Timber.w(it, "JWT decode failed") }.getOrNull()
    }

    fun isExpired(token: String, nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val exp = decode(token)?.exp ?: return false
        return exp <= nowSeconds
    }
}
