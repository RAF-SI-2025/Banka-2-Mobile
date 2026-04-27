package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import rs.raf.banka2.mobile.data.dto.auth.ActivateAccountRequest
import rs.raf.banka2.mobile.data.dto.auth.LoginRequest
import rs.raf.banka2.mobile.data.dto.auth.LoginResponse
import rs.raf.banka2.mobile.data.dto.auth.MessageResponse
import rs.raf.banka2.mobile.data.dto.auth.PasswordResetConfirmRequest
import rs.raf.banka2.mobile.data.dto.auth.PasswordResetRequest
import rs.raf.banka2.mobile.data.dto.auth.RefreshRequest

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<LoginResponse>

    @POST("auth/password_reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): Response<MessageResponse>

    @POST("auth/password_reset/confirm")
    suspend fun confirmPasswordReset(@Body body: PasswordResetConfirmRequest): Response<MessageResponse>

    @POST("auth-employee/activate")
    suspend fun activateEmployee(@Body body: ActivateAccountRequest): Response<MessageResponse>
}
