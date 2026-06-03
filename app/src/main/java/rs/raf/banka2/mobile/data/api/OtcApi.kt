package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.OtcExerciseResultDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankContractApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankListingApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankOfferApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import rs.raf.banka2.mobile.data.dto.otchistory.OtcNegotiationHistoryDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

// INTRA-BANK ONLY za debug isolation
interface OtcApi {
    @GET("otc/listings")
    suspend fun discoverIntra(): Response<List<OtcListingDto>>

    @POST("otc/offers")
    suspend fun createIntraOffer(@Body body: CreateOtcOfferDto): Response<OtcOfferDto>

    @GET("otc/offers/active")
    suspend fun listIntraOffers(): Response<List<OtcOfferDto>>

    @POST("otc/offers/{offerId}/counter")
    suspend fun counterIntra(
        @Path("offerId") offerId: Long,
        @Body body: CounterOtcOfferDto
    ): Response<OtcOfferDto>

    @POST("otc/offers/{offerId}/decline")
    suspend fun declineIntra(@Path("offerId") offerId: Long): Response<OtcOfferDto>

    /**
     * BE `acceptOffer` cita `buyerAccountId` kao `@RequestParam` (query), NE body.
     * Ranije je Mobile slao `AcceptOtcOfferDto` telo → BE ga ignorisao → buyerAccountId
     * uvek null → 400 (klijent mora navesti racun za podmirenje premije).
     */
    @POST("otc/offers/{offerId}/accept")
    suspend fun acceptIntra(
        @Path("offerId") offerId: Long,
        @Query("buyerAccountId") buyerAccountId: Long? = null
    ): Response<OtcOfferDto>

    @GET("otc/contracts")
    suspend fun listIntraContracts(@Query("status") status: String? = null): Response<List<OtcContractDto>>

    @POST("otc/contracts/{contractId}/exercise")
    suspend fun exerciseIntra(
        @Path("contractId") contractId: Long,
        @Query("buyerAccountId") buyerAccountId: Long? = null
    ): Response<OtcExerciseResultDto>

    @GET("otc/saga/{sagaId}")
    suspend fun sagaStatusIntra(@Path("sagaId") sagaId: String): Response<SagaStatusDto>

    /**
     * R1-479: rucno odustajanje od OTC ugovora (BE `POST /otc/contracts/{id}/abandon`).
     * Kupac NE dobija nazad placenu premiju (BE business rule). Vraca azuriran ugovor.
     */
    @POST("otc/contracts/{contractId}/abandon")
    suspend fun abandonIntra(@Path("contractId") contractId: Long): Response<OtcContractDto>

    @GET("interbank/otc/listings")
    suspend fun discoverInter(): Response<List<OtcInterbankListingApiDto>>

    @POST("interbank/otc/offers")
    suspend fun createInterOffer(
        @Body body: CreateOtcInterbankOfferRequest
    ): Response<OtcInterbankOfferApiDto>

    @GET("interbank/otc/offers/my")
    suspend fun listMyInterOffers(): Response<List<OtcInterbankOfferApiDto>>

    @retrofit2.http.PATCH("interbank/otc/offers/{offerId}/decline")
    suspend fun declineInter(@Path("offerId") offerId: String): Response<OtcInterbankOfferApiDto>

    @retrofit2.http.PATCH("interbank/otc/offers/{offerId}/counter")
    suspend fun counterInter(
        @Path("offerId") offerId: String,
        @Body body: CounterOtcInterbankOfferRequest
    ): Response<OtcInterbankOfferApiDto>

    @retrofit2.http.PATCH("interbank/otc/offers/{offerId}/accept")
    suspend fun acceptInter(
        @Path("offerId") offerId: String,
        @Query("accountId") accountId: Long? = null
    ): Response<OtcInterbankOfferApiDto>

    @DELETE("interbank/otc/offers/{offerId}")
    suspend fun deleteInterOffer(@Path("offerId") offerId: String): Response<OtcInterbankOfferApiDto>

    @GET("interbank/otc/contracts/my")
    suspend fun listMyInterContracts(
        @Query("status") status: String? = null
    ): Response<List<OtcInterbankContractApiDto>>

    @POST("interbank/otc/contracts/{contractId}/exercise")
    suspend fun exerciseInter(
        @Path("contractId") contractId: String,
        @Query("buyerAccountId") buyerAccountId: Long? = null
    ): Response<OtcInterbankContractApiDto>

    // ─── B10 / Spec C4 §13 — Istorija OTC pregovora (supervisor view) ─────

    /**
     * Paginiran pregled svih zapisa OTC pregovora sa filterima.
     * Dostupno samo ADMIN/SUPERVISOR rolama (BE vraca 403 inace).
     */
    @GET("otc/negotiation-history")
    suspend fun negotiationHistory(
        @Query("status") status: String? = null,
        @Query("modifiedById") modifiedById: Long? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<OtcNegotiationHistoryDto>>

    /** Hronoloski lanac kontraponuda jednog pregovora (sve iteracije). */
    @GET("otc/negotiation-history/{negotiationId}")
    suspend fun negotiationHistoryChain(
        @Path("negotiationId") negotiationId: Long
    ): Response<List<OtcNegotiationHistoryDto>>
}
