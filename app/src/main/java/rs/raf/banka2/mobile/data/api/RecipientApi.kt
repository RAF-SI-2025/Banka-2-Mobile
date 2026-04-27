package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.recipient.CreateRecipientDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.dto.recipient.UpdateRecipientDto

interface RecipientApi {

    @GET("payment-recipients")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<PageResponse<RecipientDto>>

    @POST("payment-recipients")
    suspend fun create(@Body body: CreateRecipientDto): Response<RecipientDto>

    @PUT("payment-recipients/{id}")
    suspend fun update(@Path("id") id: Long, @Body body: UpdateRecipientDto): Response<RecipientDto>

    @DELETE("payment-recipients/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
