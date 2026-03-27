package com.example.banka_2_mobile.data.api

import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.model.CalculateExchangeResponse
import com.example.banka_2_mobile.data.model.CardResponse
import com.example.banka_2_mobile.data.model.CreateOrderRequest
import com.example.banka_2_mobile.data.model.ExchangeRate
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.model.ListingDailyPrice
import com.example.banka_2_mobile.data.model.LoginRequest
import com.example.banka_2_mobile.data.model.LoginResponse
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
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ─── Auth ──────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<LoginResponse>

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

    // TODO: Confirm base path with backend. Spec says /listings but backend
    //       may use /api/listings or /stock-service/listings depending on
    //       gateway routing. Check application.yml route config.

    // FIXME: All Celina 3 endpoints are placeholder URLs — update once
    //        backend listing-service is deployed and API contract is finalized.

    @GET("listings")
    suspend fun getListings(
        @Query("type") type: String = "STOCK",   // "STOCK", "FUTURES", "FOREX"
        @Query("search") search: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PaginatedListingResponse>

    @GET("listings/{id}")
    suspend fun getListingById(
        @Path("id") id: Long
    ): Response<Listing>

    // TODO: Backend may use query params differently for history period.
    //       Spec mentions: "1D", "1W", "1M", "3M", "6M", "1Y", "ALL"
    //       Confirm param name ("period" vs "interval" vs "timeframe").
    @GET("listings/{id}/history")
    suspend fun getListingHistory(
        @Path("id") id: Long,
        @Query("period") period: String = "MONTH"  // "DAY", "WEEK", "MONTH", "YEAR"
    ): Response<List<ListingDailyPrice>>

    // ─── Portfolio ───────────────────────────────────────

    // TODO: Backend endpoint may be /portfolio or /portfolios/my.
    //       Check user-service or portfolio-service routes.
    @GET("portfolio/my")
    suspend fun getMyPortfolio(): Response<List<PortfolioItem>>

    @GET("portfolio/summary")
    suspend fun getPortfolioSummary(): Response<PortfolioSummary>

    // ─── Orders ──────────────────────────────────────────

    // TODO: Confirm POST /orders vs POST /order. Backend may require
    //       additional headers (e.g., X-Idempotency-Key) for order creation.
    @POST("orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    // TODO: Backend may support filters: status, direction, listingId, date range.
    //       Add @Query params once API contract is confirmed.
    @GET("orders/my")
    suspend fun getMyOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<OrderResponse>>
}
