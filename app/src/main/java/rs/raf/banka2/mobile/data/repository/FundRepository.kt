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
import rs.raf.banka2.mobile.data.dto.fundstatistics.FundStatisticsDto
import java.math.BigDecimal
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

    suspend fun create(name: String, description: String?, minimumContribution: BigDecimal): ApiResult<FundDetailDto> =
        safeApiCall { api.create(CreateFundDto(name, description, minimumContribution)) }

    suspend fun invest(
        fundId: Long,
        sourceAccountId: Long,
        amount: BigDecimal,
        currency: String = "RSD"
    ): ApiResult<FundPositionDto> =
        safeApiCall { api.invest(fundId, FundInvestDto(sourceAccountId, amount, currency)) }

    suspend fun withdraw(
        fundId: Long,
        destinationAccountId: Long,
        amount: BigDecimal?,
        withdrawAll: Boolean
    ): ApiResult<FundTransactionDto> {
        // R1 1054: BE cita `amount == null` kao "povuci celu poziciju" — checkbox
        // "Povuci celu poziciju" mapiramo na null iznos umesto da saljemo mrtvo
        // `withdrawAll` polje koje BE ignorise.
        val wireAmount = if (withdrawAll) null else amount
        return safeApiCall { api.withdraw(fundId, FundWithdrawDto(destinationAccountId, wireAmount)) }
    }

    suspend fun myPositions(): ApiResult<List<FundPositionDto>> = safeApiCall { api.myPositions() }

    suspend fun reassignManager(fundId: Long, newManagerEmployeeId: Long): ApiResult<FundDetailDto> =
        safeApiCall { api.reassignManager(fundId, ReassignFundManagerDto(newManagerEmployeeId)) }

    /** B12 / Spec C4 §15: statisticke metrike performansi fonda. */
    suspend fun statistics(fundId: Long): ApiResult<FundStatisticsDto> =
        safeApiCall { api.statistics(fundId) }
}
