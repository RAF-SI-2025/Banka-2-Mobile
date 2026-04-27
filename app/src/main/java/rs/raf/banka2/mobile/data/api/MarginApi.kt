package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.margin.CreateMarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAmountRequestDto
import rs.raf.banka2.mobile.data.dto.margin.MarginTransactionDto

interface MarginApi {

    @POST("margin-accounts")
    suspend fun create(@Body body: CreateMarginAccountDto): Response<MarginAccountDto>

    @GET("margin-accounts/my")
    suspend fun myAccounts(): Response<List<MarginAccountDto>>

    @GET("margin-accounts/{id}")
    suspend fun byId(@Path("id") id: Long): Response<MarginAccountDto>

    @POST("margin-accounts/{id}/deposit")
    suspend fun deposit(
        @Path("id") id: Long,
        @Body body: MarginAmountRequestDto
    ): Response<MarginAccountDto>

    @POST("margin-accounts/{id}/withdraw")
    suspend fun withdraw(
        @Path("id") id: Long,
        @Body body: MarginAmountRequestDto
    ): Response<MarginAccountDto>

    @GET("margin-accounts/{id}/transactions")
    suspend fun transactions(@Path("id") id: Long): Response<List<MarginTransactionDto>>
}
