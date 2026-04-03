package com.example.banka_2_mobile.data.api

import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.model.CalculateExchangeResponse
import com.example.banka_2_mobile.data.model.CardResponse
import com.example.banka_2_mobile.data.model.CreateOrderRequest
import com.example.banka_2_mobile.data.model.CreatePaymentRequest
import com.example.banka_2_mobile.data.model.CreateTransferRequest
import com.example.banka_2_mobile.data.model.ExchangeRate
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.model.ListingDailyPrice
import com.example.banka_2_mobile.data.model.LoginRequest
import com.example.banka_2_mobile.data.model.LoginResponse
import com.example.banka_2_mobile.data.model.PasswordResetRequest
import com.example.banka_2_mobile.data.model.OrderResponse
import com.example.banka_2_mobile.data.model.OtpResponse
import com.example.banka_2_mobile.data.model.PaginatedListingResponse
import com.example.banka_2_mobile.data.model.PaginatedResponse
import com.example.banka_2_mobile.data.model.Payment
import com.example.banka_2_mobile.data.model.PortfolioItem
import com.example.banka_2_mobile.data.model.PortfolioSummary
import com.example.banka_2_mobile.data.model.RefreshRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ─── Auth ──────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<LoginResponse>

    @POST("auth/password_reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<Any>

    // ─── OTP ───────────────────────────────────────────

    @GET("payments/my-otp")
    suspend fun getActiveOtp(): Response<OtpResponse>

    // ─── Accounts ──────────────────────────────────────

    @GET("accounts/my")
    suspend fun getMyAccounts(): Response<List<Account>>

    // ─── Payments / Transactions ───────────────────────

    @GET("payments")
    suspend fun getPayments(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PaginatedResponse<Payment>>

    @POST("payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<Any>

    // ─── Transfers ────────────────────────────────────────

    @POST("transfers/internal")
    suspend fun createTransfer(@Body request: CreateTransferRequest): Response<Any>

    // ─── Cards ─────────────────────────────────────────

    @GET("cards")
    suspend fun getMyCards(): Response<List<CardResponse>>

    // ─── Exchange ──────────────────────────────────────

    @GET("exchange-rates")
    suspend fun getExchangeRates(): Response<List<ExchangeRate>>

    @GET("exchange/calculate")
    suspend fun calculateExchange(
        @Query("amount") amount: Double,
        @Query("fromCurrency") fromCurrency: String,
        @Query("toCurrency") toCurrency: String
    ): Response<CalculateExchangeResponse>

    // ─── Celina 3: Berza / Securities ────────────────────

    @GET("listings")
    suspend fun getListings(
        @Query("type") type: String = "STOCK",
        @Query("search") search: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PaginatedListingResponse>

    @GET("listings/{id}")
    suspend fun getListingById(
        @Path("id") id: Long
    ): Response<Listing>

    @GET("listings/{id}/history")
    suspend fun getListingHistory(
        @Path("id") id: Long,
        @Query("period") period: String = "MONTH"
    ): Response<List<ListingDailyPrice>>

    @POST("listings/refresh")
    suspend fun refreshListings(): Response<Any>

    @GET("options")
    suspend fun getOptions(
        @Query("stockListingId") stockListingId: Long
    ): Response<List<Any>>

    // ─── Portfolio ───────────────────────────────────────

    @GET("portfolio/my")
    suspend fun getMyPortfolio(): Response<List<PortfolioItem>>

    @GET("portfolio/summary")
    suspend fun getPortfolioSummary(): Response<PortfolioSummary>

    @PATCH("portfolio/{id}/public-quantity")
    suspend fun updatePublicQuantity(
        @Path("id") id: Long,
        @Body request: Map<String, Int>
    ): Response<Any>

    // ─── Orders ──────────────────────────────────────────

    @POST("orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @GET("orders/my")
    suspend fun getMyOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<OrderResponse>>

    // ─── Actuaries ───────────────────────────────────────

    @GET("actuaries/agents")
    suspend fun getAgents(): Response<List<Any>>

    // ─── Tax ─────────────────────────────────────────────

    @GET("tax")
    suspend fun getTax(): Response<Any>

    @POST("tax/calculate")
    suspend fun calculateTax(): Response<Any>

    // ─── Exchanges ───────────────────────────────────────

    @GET("exchanges")
    suspend fun getExchanges(): Response<List<Any>>
}
