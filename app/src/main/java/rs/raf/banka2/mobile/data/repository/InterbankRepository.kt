package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.InterbankApi
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import rs.raf.banka2.mobile.data.dto.interbank.InterbankTransactionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterbankRepository @Inject constructor(
    private val api: InterbankApi
) {
    suspend fun initiate(request: InitiateInterbankPaymentDto): ApiResult<InterbankTransactionDto> =
        safeApiCall { api.initiate(request) }

    suspend fun status(transactionId: String): ApiResult<InterbankTransactionDto> =
        safeApiCall { api.status(transactionId) }
}
