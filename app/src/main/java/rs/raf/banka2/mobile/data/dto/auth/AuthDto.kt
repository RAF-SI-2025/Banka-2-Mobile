package rs.raf.banka2.mobile.data.dto.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String? = null,
    val expiresIn: Long? = null
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class PasswordResetRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class PasswordResetConfirmRequest(
    val token: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ActivateAccountRequest(
    val token: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    val message: String? = null
)
