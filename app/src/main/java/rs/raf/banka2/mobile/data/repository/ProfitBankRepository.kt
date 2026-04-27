package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ProfitBankApi
import rs.raf.banka2.mobile.data.dto.profitbank.ActuaryProfitDto
import rs.raf.banka2.mobile.data.dto.profitbank.BankFundPositionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfitBankRepository @Inject constructor(
    private val api: ProfitBankApi
) {
    suspend fun actuaryProfits(): ApiResult<List<ActuaryProfitDto>> = safeApiCall { api.actuaryProfits() }

    suspend fun bankFundPositions(): ApiResult<List<BankFundPositionDto>> = safeApiCall { api.bankFundPositions() }
}
