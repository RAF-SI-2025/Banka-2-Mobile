package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.account.AccountLimitsUpdateDto
import rs.raf.banka2.mobile.data.dto.account.AccountNameUpdateDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestRejectDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestResponseDto
import rs.raf.banka2.mobile.data.dto.account.AccountStatusUpdateDto
import rs.raf.banka2.mobile.data.dto.account.CreateAccountDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

interface AccountApi {

    @GET("accounts/my")
    suspend fun getMyAccounts(): Response<List<AccountDto>>

    @GET("accounts/{id}")
    suspend fun getAccountById(@Path("id") id: Long): Response<AccountDto>

    @GET("accounts/all")
    suspend fun getAllAccounts(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("ownerName") ownerName: String? = null
    ): Response<PageResponse<AccountDto>>

    @PATCH("accounts/{id}/name")
    suspend fun updateAccountName(
        @Path("id") id: Long,
        @Body body: AccountNameUpdateDto
    ): Response<AccountDto>

    @PATCH("accounts/{id}/limits")
    suspend fun updateAccountLimits(
        @Path("id") id: Long,
        @Body body: AccountLimitsUpdateDto
    ): Response<AccountDto>

    @POST("accounts/requests")
    suspend fun createAccountRequest(@Body body: AccountRequestDto): Response<AccountRequestResponseDto>

    @GET("accounts/requests/my")
    suspend fun getMyAccountRequests(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<PageResponse<AccountRequestResponseDto>>

    @POST("accounts")
    suspend fun createAccountForClient(@Body body: CreateAccountDto): Response<AccountDto>

    @GET("accounts/requests")
    suspend fun listAllRequests(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<PageResponse<AccountRequestResponseDto>>

    @PATCH("accounts/requests/{id}/approve")
    suspend fun approveAccountRequest(@Path("id") id: Long): Response<AccountRequestResponseDto>

    @PATCH("accounts/requests/{id}/reject")
    suspend fun rejectAccountRequest(
        @Path("id") id: Long,
        @Body body: AccountRequestRejectDto
    ): Response<AccountRequestResponseDto>

    @PATCH("accounts/{id}/status")
    suspend fun updateAccountStatus(
        @Path("id") id: Long,
        @Body body: AccountStatusUpdateDto
    ): Response<AccountDto>
}
