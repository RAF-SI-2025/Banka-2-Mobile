package rs.raf.banka2.mobile.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R1-584: `safeApiCall` za 200 sa praznim telom vraca `Success(Unit as T)`.
 * Ako tipiziran endpoint vrati prazno telo (BE prekrsi kontrakt), `data` je
 * zapravo `Unit` pa `transform(Unit)` (npr `{ it.content }`) baca
 * `ClassCastException`. `map` to sada hvata i vraca Failure umesto da rusi app.
 */
class ApiResultMapTest {

    private data class Page(val content: List<String>)

    /**
     * Reprodukuje tacno ono sto `safeApiCall` radi: u generickoj funkciji gde je
     * `T` erased, `Unit as T` PROLAZI bez greske (erasure) — pa se Unit "prosvercuje"
     * kao Page. CCE puca tek u `transform(Unit)` ako ga `map` ne uhvati.
     */
    private fun <T> successWithErasedUnit(): ApiResult<T> {
        @Suppress("UNCHECKED_CAST")
        return ApiResult.Success(Unit as T)
    }

    @Test
    fun map_unitMasqueradingAsTypedBody_doesNotThrow_returnsFailure() {
        val result: ApiResult<Page> = successWithErasedUnit()

        // Bez fix-a: transform(Unit) → ClassCastException → crash.
        val mapped = result.map { it.content }

        assertTrue("ocekivan Failure", mapped is ApiResult.Failure)
        mapped as ApiResult.Failure
        assertEquals(ApiError.Kind.Server, mapped.error.kind)
    }

    @Test
    fun map_validBody_transformsNormally() {
        val result: ApiResult<Page> = ApiResult.Success(Page(listOf("a", "b")))
        val mapped = result.map { it.content }
        assertTrue(mapped is ApiResult.Success)
        assertEquals(listOf("a", "b"), (mapped as ApiResult.Success).data)
    }

    @Test
    fun map_propagatesFailure() {
        val failure: ApiResult<Page> = ApiResult.Failure(
            ApiError(httpCode = 404, message = "nema", kind = ApiError.Kind.NotFound)
        )
        val mapped = failure.map { it.content }
        assertTrue(mapped is ApiResult.Failure)
    }
}
