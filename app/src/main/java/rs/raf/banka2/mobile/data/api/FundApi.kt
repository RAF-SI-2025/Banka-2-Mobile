package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.fund.CreateFundDto
import rs.raf.banka2.mobile.data.dto.fund.FundDetailDto
import rs.raf.banka2.mobile.data.dto.fund.FundInvestDto
import rs.raf.banka2.mobile.data.dto.fund.FundPerformancePointDto
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.dto.fund.FundTransactionDto
import rs.raf.banka2.mobile.data.dto.fund.FundWithdrawDto
import rs.raf.banka2.mobile.data.dto.fund.ReassignFundManagerDto

interface FundApi {

    @GET("funds")
    suspend fun list(
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null
    ): Response<List<FundSummaryDto>>

    @GET("funds/{id}")
    suspend fun details(@Path("id") id: Long): Response<FundDetailDto>

    @GET("funds/{id}/performance")
    suspend fun performance(
        @Path("id") id: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<FundPerformancePointDto>>

    @GET("funds/{id}/transactions")
    suspend fun transactions(@Path("id") id: Long): Response<List<FundTransactionDto>>

    @POST("funds")
    suspend fun create(@Body body: CreateFundDto): Response<FundDetailDto>

    @POST("funds/{id}/invest")
    suspend fun invest(@Path("id") id: Long, @Body body: FundInvestDto): Response<FundPositionDto>

    @POST("funds/{id}/withdraw")
    suspend fun withdraw(@Path("id") id: Long, @Body body: FundWithdrawDto): Response<FundTransactionDto>

    @GET("funds/my-positions")
    suspend fun myPositions(): Response<List<FundPositionDto>>

    @GET("funds/bank-positions")
    suspend fun bankPositions(): Response<List<FundPositionDto>>

    /** P1.2: prebaci vlasnistvo fonda na drugog supervizora (admin/supervisor only). */
    @POST("funds/{id}/reassign-manager")
    suspend fun reassignManager(
        @Path("id") fundId: Long,
        @Body body: ReassignFundManagerDto
    ): Response<FundDetailDto>
}
