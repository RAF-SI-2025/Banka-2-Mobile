package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.actuary.ActuaryDto
import rs.raf.banka2.mobile.data.dto.actuary.UpdateActuaryLimitDto

interface ActuaryApi {

    @GET("actuaries/agents")
    suspend fun listAgents(
        @Query("email") email: String? = null,
        @Query("firstName") firstName: String? = null,
        @Query("lastName") lastName: String? = null,
        @Query("position") position: String? = null
    ): Response<List<ActuaryDto>>

    @GET("actuaries/{employeeId}")
    suspend fun getActuary(@Path("employeeId") employeeId: Long): Response<ActuaryDto>

    @PATCH("actuaries/{employeeId}/limit")
    suspend fun updateLimit(
        @Path("employeeId") employeeId: Long,
        @Body body: UpdateActuaryLimitDto
    ): Response<ActuaryDto>

    @PATCH("actuaries/{employeeId}/reset-limit")
    suspend fun resetLimit(@Path("employeeId") employeeId: Long): Response<ActuaryDto>
}
