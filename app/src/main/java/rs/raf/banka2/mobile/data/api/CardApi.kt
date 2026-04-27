package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.dto.card.CardLimitUpdateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestCreateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestRejectDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestResponseDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

interface CardApi {

    @GET("cards")
    suspend fun getMyCards(): Response<List<CardDto>>

    @GET("cards/account/{accountId}")
    suspend fun getCardsForAccount(@Path("accountId") accountId: Long): Response<List<CardDto>>

    @PATCH("cards/{id}/block")
    suspend fun blockCard(@Path("id") id: Long): Response<CardDto>

    @PATCH("cards/{id}/unblock")
    suspend fun unblockCard(@Path("id") id: Long): Response<CardDto>

    @PATCH("cards/{id}/deactivate")
    suspend fun deactivateCard(@Path("id") id: Long): Response<CardDto>

    @PATCH("cards/{id}/limit")
    suspend fun updateLimit(
        @Path("id") id: Long,
        @Body body: CardLimitUpdateDto
    ): Response<CardDto>

    @POST("cards/requests")
    suspend fun submitCardRequest(@Body body: CardRequestCreateDto): Response<CardRequestResponseDto>

    @retrofit2.http.POST("cards/requests/{id}/confirm")
    suspend fun confirmCardRequest(
        @Path("id") id: Long,
        @Body body: rs.raf.banka2.mobile.data.dto.payment.OtpVerifyRequest
    ): Response<CardRequestResponseDto>

    @GET("cards/requests/my")
    suspend fun getMyCardRequests(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<PageResponse<CardRequestResponseDto>>

    @GET("cards/requests")
    suspend fun listAllCardRequests(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<PageResponse<CardRequestResponseDto>>

    @PATCH("cards/requests/{id}/approve")
    suspend fun approveCardRequest(@Path("id") id: Long): Response<CardRequestResponseDto>

    @PATCH("cards/requests/{id}/reject")
    suspend fun rejectCardRequest(
        @Path("id") id: Long,
        @Body body: CardRequestRejectDto
    ): Response<CardRequestResponseDto>
}
