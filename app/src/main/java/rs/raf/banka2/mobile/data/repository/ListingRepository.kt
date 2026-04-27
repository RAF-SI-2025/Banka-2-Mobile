package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ListingApi
import rs.raf.banka2.mobile.data.dto.listing.ListingDailyPriceDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingRepository @Inject constructor(
    private val api: ListingApi
) {
    suspend fun list(
        type: String? = null,
        search: String? = null,
        exchangePrefix: String? = null,
        priceMin: Double? = null,
        priceMax: Double? = null,
        settlementFrom: String? = null,
        settlementTo: String? = null,
        page: Int = 0,
        size: Int = 30
    ): ApiResult<List<ListingDto>> = safeApiCall {
        api.getListings(
            type = type,
            search = search,
            exchangePrefix = exchangePrefix,
            priceMin = priceMin,
            priceMax = priceMax,
            settlementDateFrom = settlementFrom,
            settlementDateTo = settlementTo,
            page = page,
            size = size
        )
    }.map { it.content }

    suspend fun byId(id: Long): ApiResult<ListingDto> = safeApiCall { api.getListingById(id) }

    suspend fun history(id: Long, period: String = "MONTH"): ApiResult<List<ListingDailyPriceDto>> =
        safeApiCall { api.getListingHistory(id, period) }

    suspend fun refresh(): ApiResult<Unit> = safeApiCall { api.refreshListings() }.map { }
}
