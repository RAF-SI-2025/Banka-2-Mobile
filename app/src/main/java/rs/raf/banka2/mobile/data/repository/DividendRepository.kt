package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.DividendApi
import rs.raf.banka2.mobile.data.dto.dividend.DividendPayoutDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dividend repository — B9 / Spec C3 §11.
 *
 * `getByPosition` daje istoriju dividendi za jednu STOCK poziciju u portfoliu
 * (klikabilna ekspanzija u PortfolioScreen).
 */
@Singleton
class DividendRepository @Inject constructor(
    private val api: DividendApi
) {
    suspend fun getMy(): ApiResult<List<DividendPayoutDto>> =
        safeApiCall { api.getMy() }

    suspend fun getByPosition(portfolioId: Long): ApiResult<List<DividendPayoutDto>> =
        safeApiCall { api.getByPosition(portfolioId) }
}
