package rs.raf.banka2.mobile.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * R1 mob4: karakterizacija `safeApiCall` ponasanja za prazno 2xx telo.
 *
 * Ranije je `safeApiCall` za bilo koje null telo vracao `Success(Unit as T)` —
 * sto je za TIPIZOVAN poziv (Response<Dto>) "prosvercivalo" Unit i bacalo
 * `ClassCastException` daleko od izvora kad bi pozivalac koristio vrednost.
 * Sada se prazno telo na tipizovanom 200 tretira kao Failure(Server), a
 * `Response<Unit>` (Retrofit vraca Unit objekat, ne null) ostaje Success.
 */
class SafeApiCallTest {

    private data class Dto(val value: String)

    @Test
    fun typedEmptyBody200_returnsServerFailure_notUnitCast() = runTest {
        val response: Response<Dto> = Response.success(null)
        val result = safeApiCall { response }

        assertTrue("ocekivan Failure za prazno telo na tipizovanom 200", result is ApiResult.Failure)
        result as ApiResult.Failure
        assertEquals(ApiError.Kind.Server, result.error.kind)
    }

    @Test
    fun unitBody200_returnsSuccess() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        val result = safeApiCall { response }

        assertTrue(result is ApiResult.Success)
        assertEquals(Unit, (result as ApiResult.Success).data)
    }

    @Test
    fun typedNonEmptyBody200_returnsSuccess() = runTest {
        val response: Response<Dto> = Response.success(Dto("ok"))
        val result = safeApiCall { response }

        assertTrue(result is ApiResult.Success)
        assertEquals("ok", (result as ApiResult.Success).data.value)
    }

    @Test
    fun typed204NoContent_returnsSuccessUnit() = runTest {
        // 204 sa null telom = legitimno prazno (void akcija). Gradimo raw 204
        // odgovor da izbegnemo overload-ambiguity sa null body-jem.
        val raw = okhttp3.Response.Builder()
            .code(204)
            .message("No Content")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .request(okhttp3.Request.Builder().url("http://localhost/x").build())
            .build()
        val response: Response<Unit> = Response.success(null, raw)
        val result = safeApiCall { response }
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun errorBody_returnsFailure() = runTest {
        val body = """{"message":"nema"}""".toResponseBody("application/json".toMediaType())
        val response: Response<Dto> = Response.error(404, body)
        val result = safeApiCall { response }

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.NotFound, (result as ApiResult.Failure).error.kind)
        assertEquals(404, result.error.httpCode)
    }
}
