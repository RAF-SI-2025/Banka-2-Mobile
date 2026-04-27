package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import rs.raf.banka2.mobile.data.dto.interbank.InterbankTransactionDto

interface InterbankApi {

    @POST("interbank/payments")
    suspend fun initiate(@Body body: InitiateInterbankPaymentDto): Response<InterbankTransactionDto>

    @GET("interbank/payments/{transactionId}")
    suspend fun status(@Path("transactionId") transactionId: String): Response<InterbankTransactionDto>
}
