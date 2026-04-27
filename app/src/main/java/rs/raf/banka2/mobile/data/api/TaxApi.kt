package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.tax.TaxRecordDto

interface TaxApi {

    @GET("tax")
    suspend fun listAll(
        @Query("userType") userType: String? = null,
        @Query("name") name: String? = null
    ): Response<List<TaxRecordDto>>

    @GET("tax/my")
    suspend fun myRecord(): Response<TaxRecordDto>

    @POST("tax/calculate")
    suspend fun calculate(): Response<Unit>
}
