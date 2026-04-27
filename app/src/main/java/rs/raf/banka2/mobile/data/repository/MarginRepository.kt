package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.MarginApi
import rs.raf.banka2.mobile.data.dto.margin.CreateMarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAmountRequestDto
import rs.raf.banka2.mobile.data.dto.margin.MarginTransactionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarginRepository @Inject constructor(
    private val api: MarginApi
) {
    suspend fun myAccounts(): ApiResult<List<MarginAccountDto>> = safeApiCall { api.myAccounts() }

    suspend fun create(
        initialMargin: Double,
        maintenanceMargin: Double,
        bankParticipation: Double,
        userId: Long? = null,
        companyId: Long? = null
    ): ApiResult<MarginAccountDto> = safeApiCall {
        api.create(CreateMarginAccountDto(initialMargin, maintenanceMargin, bankParticipation, userId, companyId))
    }

    suspend fun byId(id: Long): ApiResult<MarginAccountDto> = safeApiCall { api.byId(id) }

    suspend fun deposit(id: Long, amount: Double): ApiResult<MarginAccountDto> =
        safeApiCall { api.deposit(id, MarginAmountRequestDto(amount)) }

    suspend fun withdraw(id: Long, amount: Double): ApiResult<MarginAccountDto> =
        safeApiCall { api.withdraw(id, MarginAmountRequestDto(amount)) }

    suspend fun transactions(id: Long): ApiResult<List<MarginTransactionDto>> =
        safeApiCall { api.transactions(id) }
}
