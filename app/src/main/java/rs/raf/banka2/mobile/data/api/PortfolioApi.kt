package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioItemDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto
import rs.raf.banka2.mobile.data.dto.portfolio.PublicQuantityUpdateDto

interface PortfolioApi {

    @GET("portfolio/my")
    suspend fun getMyPortfolio(): Response<List<PortfolioItemDto>>

    @GET("portfolio/summary")
    suspend fun getSummary(): Response<PortfolioSummaryDto>

    @PATCH("portfolio/{id}/public")
    suspend fun setPublicQuantity(
        @Path("id") id: Long,
        @Body body: PublicQuantityUpdateDto
    ): Response<PortfolioItemDto>
}
