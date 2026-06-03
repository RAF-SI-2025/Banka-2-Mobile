package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.MarginApi
import rs.raf.banka2.mobile.data.dto.margin.CreateMarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginAmountRequestDto
import rs.raf.banka2.mobile.data.dto.margin.MarginMessageDto
import rs.raf.banka2.mobile.data.dto.margin.MarginTransactionDto
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarginRepository @Inject constructor(
    private val api: MarginApi
) {
    suspend fun myAccounts(): ApiResult<List<MarginAccountDto>> = safeApiCall { api.myAccounts() }

    suspend fun create(
        accountId: Long,
        initialMargin: BigDecimal,
        maintenanceMargin: BigDecimal,
        bankParticipation: BigDecimal,
        userId: Long? = null,
        companyId: Long? = null
    ): ApiResult<MarginAccountDto> = safeApiCall {
        api.create(CreateMarginAccountDto(accountId, initialMargin, maintenanceMargin, bankParticipation, userId, companyId))
    }

    suspend fun byId(id: Long): ApiResult<MarginAccountDto> = safeApiCall { api.byId(id) }

    suspend fun deposit(id: Long, amount: BigDecimal): ApiResult<MarginMessageDto> =
        safeApiCall { api.deposit(id, MarginAmountRequestDto(amount)) }

    suspend fun withdraw(id: Long, amount: BigDecimal): ApiResult<MarginMessageDto> =
        safeApiCall { api.withdraw(id, MarginAmountRequestDto(amount)) }

    suspend fun transactions(id: Long): ApiResult<List<MarginTransactionDto>> =
        safeApiCall { api.transactions(id) }
}
