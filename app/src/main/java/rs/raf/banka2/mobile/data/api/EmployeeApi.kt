package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

/**
 * Minimalni Employees endpoint za login flow — vracamo prvih nekoliko
 * rezultata kako bi otkrili permisije korisnika po email-u.
 *
 * Backend endpoint: `/employees?email=...&page=0&limit=1`.
 */
interface EmployeeApi {

    @GET("employees")
    suspend fun searchByEmail(
        @Query("email") email: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 1
    ): Response<PageResponse<EmployeeDto>>
}
