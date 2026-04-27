package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.otc.AcceptOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.ExerciseRequestDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto

interface OtcApi {

    // ─── Discovery ────────────────────────────────────────
    @GET("otc/listings")
    suspend fun discoverIntra(): Response<List<OtcListingDto>>

    @GET("interbank/otc/listings")
    suspend fun discoverInter(): Response<List<OtcListingDto>>

    // ─── Ponude ───────────────────────────────────────────
    @POST("otc/offers")
    suspend fun createIntraOffer(@Body body: CreateOtcOfferDto): Response<OtcOfferDto>

    @POST("interbank/otc/offers")
    suspend fun createInterOffer(@Body body: CreateOtcOfferDto): Response<OtcOfferDto>

    @GET("otc/offers/active")
    suspend fun listIntraOffers(): Response<List<OtcOfferDto>>

    @GET("interbank/otc/offers")
    suspend fun listInterOffers(): Response<List<OtcOfferDto>>

    @POST("otc/offers/{offerId}/counter")
    suspend fun counterIntra(
        @Path("offerId") offerId: Long,
        @Body body: CounterOtcOfferDto
    ): Response<OtcOfferDto>

    @POST("interbank/otc/offers/{offerId}/counter")
    suspend fun counterInter(
        @Path("offerId") offerId: Long,
        @Body body: CounterOtcOfferDto
    ): Response<OtcOfferDto>

    @POST("otc/offers/{offerId}/decline")
    suspend fun declineIntra(@Path("offerId") offerId: Long): Response<OtcOfferDto>

    @POST("interbank/otc/offers/{offerId}/decline")
    suspend fun declineInter(@Path("offerId") offerId: Long): Response<OtcOfferDto>

    @POST("otc/offers/{offerId}/accept")
    suspend fun acceptIntra(
        @Path("offerId") offerId: Long,
        @Body body: AcceptOtcOfferDto
    ): Response<OtcOfferDto>

    @POST("interbank/otc/offers/{offerId}/accept")
    suspend fun acceptInter(
        @Path("offerId") offerId: Long,
        @Body body: AcceptOtcOfferDto
    ): Response<OtcOfferDto>

    // ─── Ugovori ──────────────────────────────────────────
    @GET("otc/contracts")
    suspend fun listIntraContracts(@Query("status") status: String? = null): Response<List<OtcContractDto>>

    @GET("interbank/otc/contracts")
    suspend fun listInterContracts(@Query("status") status: String? = null): Response<List<OtcContractDto>>

    @POST("otc/contracts/{contractId}/exercise")
    suspend fun exerciseIntra(
        @Path("contractId") contractId: Long,
        @Body body: ExerciseRequestDto
    ): Response<OtcContractDto>

    @POST("interbank/otc/contracts/{contractId}/exercise")
    suspend fun exerciseInter(
        @Path("contractId") contractId: Long,
        @Body body: ExerciseRequestDto
    ): Response<OtcContractDto>

    @GET("interbank/otc/contracts/{contractId}/saga-status")
    suspend fun sagaStatus(@Path("contractId") contractId: Long): Response<SagaStatusDto>
}
