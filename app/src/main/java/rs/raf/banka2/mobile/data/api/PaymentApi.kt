package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import rs.raf.banka2.mobile.data.dto.payment.OtpRequestStatusDto
import rs.raf.banka2.mobile.data.dto.payment.OtpResponseDto
import rs.raf.banka2.mobile.data.dto.payment.OtpVerifyRequest
import rs.raf.banka2.mobile.data.dto.payment.OtpVerifyResponse
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto

interface PaymentApi {

    // ─── Plaćanja ────────────────────────────────────────────
    @POST("payments")
    suspend fun createPayment(@Body body: CreatePaymentRequestDto): Response<PaymentResponseDto>

    @retrofit2.http.GET("payments/{id}/receipt")
    @retrofit2.http.Streaming
    suspend fun downloadReceipt(@Path("id") id: Long): Response<okhttp3.ResponseBody>

    @GET("payments")
    suspend fun getMyPayments(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("accountNumber") accountNumber: String? = null,
        @Query("minAmount") minAmount: Double? = null,
        @Query("maxAmount") maxAmount: Double? = null,
        @Query("status") status: String? = null
    ): Response<PageResponse<PaymentListItemDto>>

    @GET("payments/{id}")
    suspend fun getPaymentById(@Path("id") id: Long): Response<PaymentResponseDto>

    // ─── OTP ─────────────────────────────────────────────────
    @POST("payments/request-otp")
    suspend fun requestOtp(): Response<OtpRequestStatusDto>

    @POST("payments/request-otp-email")
    suspend fun requestOtpViaEmail(): Response<OtpRequestStatusDto>

    @GET("payments/my-otp")
    suspend fun getActiveOtp(): Response<OtpResponseDto>

    @POST("payments/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyRequest): Response<OtpVerifyResponse>
}
