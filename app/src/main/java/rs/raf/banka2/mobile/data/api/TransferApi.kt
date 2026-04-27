package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.transfer.TransferFxRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferInternalRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferResponseDto

interface TransferApi {

    @POST("transfers/internal")
    suspend fun createInternalTransfer(@Body body: TransferInternalRequestDto): Response<TransferResponseDto>

    @POST("transfers/fx")
    suspend fun createFxTransfer(@Body body: TransferFxRequestDto): Response<TransferResponseDto>

    @GET("transfers")
    suspend fun listMyTransfers(
        @Query("accountNumber") accountNumber: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<List<TransferResponseDto>>
}
