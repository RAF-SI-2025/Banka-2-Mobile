package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.PortfolioApi
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioItemDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto
import rs.raf.banka2.mobile.data.dto.portfolio.PublicQuantityUpdateDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepository @Inject constructor(
    private val api: PortfolioApi
) {
    suspend fun myPortfolio(): ApiResult<List<PortfolioItemDto>> =
        safeApiCall { api.getMyPortfolio() }

    suspend fun summary(): ApiResult<PortfolioSummaryDto> = safeApiCall { api.getSummary() }

    suspend fun setPublicQuantity(id: Long, quantity: Int): ApiResult<PortfolioItemDto> =
        safeApiCall { api.setPublicQuantity(id, PublicQuantityUpdateDto(quantity)) }
}
