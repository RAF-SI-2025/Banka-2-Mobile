package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.TransferApi
import rs.raf.banka2.mobile.data.dto.transfer.TransferFxRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferInternalRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val api: TransferApi
) {
    suspend fun internal(request: TransferInternalRequestDto): ApiResult<TransferResponseDto> =
        safeApiCall { api.createInternalTransfer(request) }

    suspend fun fx(request: TransferFxRequestDto): ApiResult<TransferResponseDto> =
        safeApiCall { api.createFxTransfer(request) }

    suspend fun listMyTransfers(accountNumber: String? = null): ApiResult<List<TransferResponseDto>> =
        safeApiCall { api.listMyTransfers(accountNumber = accountNumber) }
}
