package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.FundApi
import rs.raf.banka2.mobile.data.dto.fund.CreateFundDto
import rs.raf.banka2.mobile.data.dto.fund.FundDetailDto
import rs.raf.banka2.mobile.data.dto.fund.FundInvestDto
import rs.raf.banka2.mobile.data.dto.fund.FundPerformancePointDto
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.dto.fund.FundTransactionDto
import rs.raf.banka2.mobile.data.dto.fund.FundWithdrawDto
import rs.raf.banka2.mobile.data.dto.fund.ReassignFundManagerDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FundRepository @Inject constructor(
    private val api: FundApi
) {
    suspend fun list(
        search: String? = null,
        sort: String? = null,
        direction: String? = null
    ): ApiResult<List<FundSummaryDto>> =
        safeApiCall { api.list(search = search, sort = sort, direction = direction) }

    suspend fun details(id: Long): ApiResult<FundDetailDto> = safeApiCall { api.details(id) }

    suspend fun performance(id: Long): ApiResult<List<FundPerformancePointDto>> =
        safeApiCall { api.performance(id) }

    suspend fun transactions(id: Long): ApiResult<List<FundTransactionDto>> =
        safeApiCall { api.transactions(id) }

    suspend fun create(name: String, description: String?, minimumContribution: Double): ApiResult<FundDetailDto> =
        safeApiCall { api.create(CreateFundDto(name, description, minimumContribution)) }

    suspend fun invest(fundId: Long, sourceAccountId: Long, amount: Double): ApiResult<FundPositionDto> =
        safeApiCall { api.invest(fundId, FundInvestDto(sourceAccountId, amount)) }

    suspend fun withdraw(
        fundId: Long,
        destinationAccountId: Long,
        amount: Double?,
        withdrawAll: Boolean
    ): ApiResult<FundTransactionDto> =
        safeApiCall { api.withdraw(fundId, FundWithdrawDto(destinationAccountId, amount, withdrawAll)) }

    suspend fun myPositions(): ApiResult<List<FundPositionDto>> = safeApiCall { api.myPositions() }

    suspend fun bankPositions(): ApiResult<List<FundPositionDto>> = safeApiCall { api.bankPositions() }

    suspend fun reassignManager(fundId: Long, newManagerEmployeeId: Long): ApiResult<FundDetailDto> =
        safeApiCall { api.reassignManager(fundId, ReassignFundManagerDto(newManagerEmployeeId)) }
}
