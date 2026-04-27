package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.CreateEmployeeRequestDto
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.common.UpdateEmployeePermissionsDto
import rs.raf.banka2.mobile.data.dto.common.UpdateEmployeeRequestDto

/**
 * Admin API za upravljanje zaposlenima. Razdvojeno od `EmployeeApi` koji se koristi
 * samo za login flow (search-by-email) — ovaj interfejs pokriva CRUD operacije.
 */
interface EmployeeAdminApi {

    @POST("employees")
    suspend fun create(@Body body: CreateEmployeeRequestDto): Response<EmployeeDto>

    @GET("employees")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("email") email: String? = null,
        @Query("firstName") firstName: String? = null,
        @Query("lastName") lastName: String? = null,
        @Query("position") position: String? = null
    ): Response<PageResponse<EmployeeDto>>

    @GET("employees/{id}")
    suspend fun byId(@Path("id") id: Long): Response<EmployeeDto>

    @PUT("employees/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: UpdateEmployeeRequestDto
    ): Response<EmployeeDto>

    @PATCH("employees/{id}/deactivate")
    suspend fun deactivate(@Path("id") id: Long): Response<EmployeeDto>

    @GET("employees/{id}/permissions")
    suspend fun getPermissions(@Path("id") id: Long): Response<List<String>>

    @PATCH("employees/{id}/permissions")
    suspend fun updatePermissions(
        @Path("id") id: Long,
        @Body body: UpdateEmployeePermissionsDto
    ): Response<EmployeeDto>
}
