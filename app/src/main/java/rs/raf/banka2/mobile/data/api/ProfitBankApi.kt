package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import rs.raf.banka2.mobile.data.dto.profitbank.ActuaryProfitDto
import rs.raf.banka2.mobile.data.dto.profitbank.BankFundPositionDto

interface ProfitBankApi {

    @GET("profit-bank/actuary-performance")
    suspend fun actuaryProfits(): Response<List<ActuaryProfitDto>>

    @GET("profit-bank/fund-positions")
    suspend fun bankFundPositions(): Response<List<BankFundPositionDto>>
}
