package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.tax.TaxBreakdownItemDto
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

    /**
     * Spec Celina 3 §516-518: per-listing breakdown poreza za korisnika.
     * BE endpoint vraca listu hartija koje su doprinele profitu/gubitku.
     */
    @GET("tax/{userId}/{userType}/breakdown")
    suspend fun getBreakdown(
        @Path("userId") userId: Long,
        @Path("userType") userType: String
    ): Response<List<TaxBreakdownItemDto>>

    /** Per-listing breakdown za autentifikovanog korisnika. */
    @GET("tax/my/breakdown")
    suspend fun getMyBreakdown(): Response<List<TaxBreakdownItemDto>>
}
