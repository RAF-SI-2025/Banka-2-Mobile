package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.savings.OpenDepositRequest
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsRateDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsTransactionDto
import rs.raf.banka2.mobile.data.dto.savings.ToggleAutoRenewRequest
import rs.raf.banka2.mobile.data.dto.savings.WithdrawEarlyRequest

interface SavingsApi {
    @POST("savings/deposits")
    suspend fun openDeposit(@Body body: OpenDepositRequest): Response<SavingsDepositDto>

    @GET("savings/deposits/my")
    suspend fun listMy(): Response<List<SavingsDepositDto>>

    @GET("savings/deposits/{id}")
    suspend fun getDeposit(@Path("id") id: Long): Response<SavingsDepositDto>

    @GET("savings/deposits/{id}/transactions")
    suspend fun getTransactions(@Path("id") id: Long): Response<List<SavingsTransactionDto>>

    @PATCH("savings/deposits/{id}/auto-renew")
    suspend fun toggleAutoRenew(
        @Path("id") id: Long,
        @Body body: ToggleAutoRenewRequest,
    ): Response<SavingsDepositDto>

    @POST("savings/deposits/{id}/withdraw-early")
    suspend fun withdrawEarly(
        @Path("id") id: Long,
        @Body body: WithdrawEarlyRequest,
    ): Response<SavingsDepositDto>

    @GET("savings/rates")
    suspend fun getRates(@Query("currency") currency: String?): Response<List<SavingsRateDto>>
}
