package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.otc.AcceptOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.ExerciseRequestDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankContractApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankListingApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankOfferApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto

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

    @POST("otc/offers/{offerId}/accept")
    suspend fun acceptIntra(
        @Path("offerId") offerId: Long,
        @Body body: AcceptOtcOfferDto
    ): Response<OtcOfferDto>

    @GET("otc/contracts")
    suspend fun listIntraContracts(@Query("status") status: String? = null): Response<List<OtcContractDto>>

    @POST("otc/contracts/{contractId}/exercise")
    suspend fun exerciseIntra(
        @Path("contractId") contractId: Long,
        @Body body: ExerciseRequestDto
    ): Response<OtcContractDto>

    @GET("otc/contracts/{contractId}/saga-status")
    suspend fun sagaStatusIntra(@Path("contractId") contractId: Long): Response<SagaStatusDto>

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
}
