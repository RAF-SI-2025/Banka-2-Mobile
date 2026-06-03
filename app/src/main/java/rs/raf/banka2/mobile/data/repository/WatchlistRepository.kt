package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.WatchlistApi
import rs.raf.banka2.mobile.data.dto.watchlist.AddWatchlistItemRequest
import rs.raf.banka2.mobile.data.dto.watchlist.CreateWatchlistRequest
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistDto
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistFilterType
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistItemDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FE2 Mobile port] Wrapper oko [WatchlistApi] koji vraca [ApiResult] umesto
 * Retrofit `Response`. ViewModeli rade pattern-match na ApiResult i nigde u UI
 * sloju nema try/catch nad mreznim pozivima.
 */
@Singleton
class WatchlistRepository @Inject constructor(
    private val api: WatchlistApi,
) {
    suspend fun listMyWatchlists(): ApiResult<List<WatchlistDto>> =
        safeApiCall { api.listMy() }

    suspend fun create(name: String): ApiResult<WatchlistDto> =
        safeApiCall { api.create(CreateWatchlistRequest(name)) }

    suspend fun rename(id: Long, name: String): ApiResult<WatchlistDto> =
        safeApiCall { api.rename(id, CreateWatchlistRequest(name)) }

    suspend fun delete(id: Long): ApiResult<Unit> =
        safeApiCall { api.delete(id) }

    suspend fun listItems(
        watchlistId: Long,
        filter: WatchlistFilterType = WatchlistFilterType.ALL,
    ): ApiResult<List<WatchlistItemDto>> =
        safeApiCall { api.listItems(watchlistId, filter.apiValue) }

    suspend fun addItem(watchlistId: Long, listingId: Long): ApiResult<WatchlistItemDto> =
        safeApiCall { api.addItem(watchlistId, AddWatchlistItemRequest(listingId)) }

    suspend fun removeItem(watchlistId: Long, listingId: Long): ApiResult<Unit> =
        safeApiCall { api.removeItem(watchlistId, listingId) }
}
