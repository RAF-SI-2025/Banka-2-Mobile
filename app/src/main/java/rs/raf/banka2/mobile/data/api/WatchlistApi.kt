package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.watchlist.AddWatchlistItemRequest
import rs.raf.banka2.mobile.data.dto.watchlist.CreateWatchlistRequest
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistDto
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistItemDto

/**
 * [FE2 Mobile port] Retrofit interfejs za Watchlist endpoint-e.
 *
 * BE: trading-service `WatchlistController` izlaze rute pod `/watchlists`.
 * Sve rute zahtevaju JWT (AuthInterceptor automatski dodaje Bearer header).
 */
interface WatchlistApi {

    @GET("watchlists")
    suspend fun listMy(): Response<List<WatchlistDto>>

    @POST("watchlists")
    suspend fun create(@Body body: CreateWatchlistRequest): Response<WatchlistDto>

    @PATCH("watchlists/{id}")
    suspend fun rename(
        @Path("id") id: Long,
        @Body body: CreateWatchlistRequest,
    ): Response<WatchlistDto>

    @DELETE("watchlists/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>

    @GET("watchlists/{id}/items")
    suspend fun listItems(
        @Path("id") id: Long,
        @Query("type") type: String? = null,
    ): Response<List<WatchlistItemDto>>

    /**
     * BE `addItem` cita `@RequestBody AddWatchlistItemRequest` (`{"listingId":N}`),
     * NE `@RequestParam`. Ranije query → 400.
     */
    @POST("watchlists/{id}/items")
    suspend fun addItem(
        @Path("id") id: Long,
        @Body body: AddWatchlistItemRequest,
    ): Response<WatchlistItemDto>

    /**
     * KRITICNO: BE brise po `listingId`, NE po `itemId`. Path je `/items/{listingId}`.
     */
    @DELETE("watchlists/{id}/items/{listingId}")
    suspend fun removeItem(
        @Path("id") id: Long,
        @Path("listingId") listingId: Long,
    ): Response<Unit>
}
