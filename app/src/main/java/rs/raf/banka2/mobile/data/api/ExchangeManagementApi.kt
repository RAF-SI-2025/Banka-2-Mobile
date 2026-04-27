package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.listing.ExchangeManagementDto
import rs.raf.banka2.mobile.data.dto.listing.ToggleTestModeDto

interface ExchangeManagementApi {

    @GET("exchanges")
    suspend fun listExchanges(): Response<List<ExchangeManagementDto>>

    @GET("exchanges/{acronym}")
    suspend fun getExchange(@Path("acronym") acronym: String): Response<ExchangeManagementDto>

    @PATCH("exchanges/{acronym}/test-mode")
    suspend fun toggleTestMode(
        @Path("acronym") acronym: String,
        @Body body: ToggleTestModeDto
    ): Response<ExchangeManagementDto>
}
