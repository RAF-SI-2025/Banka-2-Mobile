package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.ClientDto
import rs.raf.banka2.mobile.data.dto.common.CreateClientRequestDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.common.UpdateClientRequestDto

interface ClientApi {

    @POST("clients")
    suspend fun create(@Body body: CreateClientRequestDto): Response<ClientDto>

    /**
     * ME-04: za login flow — `list` sa filter-om po email-u sluzi kao "searchByEmail".
     * BE prima `email` query param i vraca matching clients (max 1 expected za point lookup).
     */
    @GET("clients")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("firstName") firstName: String? = null,
        @Query("lastName") lastName: String? = null,
        @Query("email") email: String? = null,
        @Query("search") search: String? = null
    ): Response<PageResponse<ClientDto>>

    @GET("clients/{id}")
    suspend fun byId(@Path("id") id: Long): Response<ClientDto>

    @PUT("clients/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: UpdateClientRequestDto
    ): Response<ClientDto>
}
