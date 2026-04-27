package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.listing.ListingDailyPriceDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto

interface ListingApi {

    @GET("listings")
    suspend fun getListings(
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("exchangePrefix") exchangePrefix: String? = null,
        @Query("priceMin") priceMin: Double? = null,
        @Query("priceMax") priceMax: Double? = null,
        @Query("settlementDateFrom") settlementDateFrom: String? = null,
        @Query("settlementDateTo") settlementDateTo: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<PageResponse<ListingDto>>

    @GET("listings/{id}")
    suspend fun getListingById(@Path("id") id: Long): Response<ListingDto>

    @GET("listings/{id}/history")
    suspend fun getListingHistory(
        @Path("id") id: Long,
        @Query("period") period: String = "MONTH"
    ): Response<List<ListingDailyPriceDto>>

    @POST("listings/refresh")
    suspend fun refreshListings(): Response<Unit>
}
