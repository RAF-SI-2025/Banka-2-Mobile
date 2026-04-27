package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.option.OptionChainDto
import rs.raf.banka2.mobile.data.dto.option.OptionDto

interface OptionApi {

    @GET("options")
    suspend fun getOptionChain(
        @Query("stockListingId") stockListingId: Long
    ): Response<List<OptionChainDto>>

    @GET("options/{id}")
    suspend fun getOption(@Path("id") id: Long): Response<OptionDto>

    @POST("options/{id}/exercise")
    suspend fun exerciseOption(@Path("id") id: Long): Response<Unit>
}
