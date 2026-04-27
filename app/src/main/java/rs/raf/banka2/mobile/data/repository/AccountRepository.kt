package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.AccountApi
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.account.AccountLimitsUpdateDto
import rs.raf.banka2.mobile.data.dto.account.AccountNameUpdateDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestRejectDto
import rs.raf.banka2.mobile.data.dto.account.AccountRequestResponseDto
import rs.raf.banka2.mobile.data.dto.account.AccountStatusUpdateDto
import rs.raf.banka2.mobile.data.dto.account.CreateAccountDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val api: AccountApi
) {
    suspend fun getMyAccounts(): ApiResult<List<AccountDto>> =
        safeApiCall { api.getMyAccounts() }

    suspend fun getAccountById(id: Long): ApiResult<AccountDto> =
        safeApiCall { api.getAccountById(id) }

    suspend fun renameAccount(id: Long, name: String): ApiResult<AccountDto> =
        safeApiCall { api.updateAccountName(id, AccountNameUpdateDto(name)) }

    suspend fun updateLimits(
        id: Long,
        dailyLimit: Double?,
        monthlyLimit: Double?,
        otpCode: String?
    ): ApiResult<AccountDto> =
        safeApiCall { api.updateAccountLimits(id, AccountLimitsUpdateDto(dailyLimit, monthlyLimit, otpCode)) }

    suspend fun submitAccountRequest(request: AccountRequestDto): ApiResult<AccountRequestResponseDto> =
        safeApiCall { api.createAccountRequest(request) }

    suspend fun getMyAccountRequests(): ApiResult<List<AccountRequestResponseDto>> =
        safeApiCall { api.getMyAccountRequests() }.map { it.content }

    suspend fun listAllAccounts(): ApiResult<List<AccountDto>> =
        safeApiCall { api.getAllAccounts() }.map { it.content }

    suspend fun createAccountForClient(request: CreateAccountDto): ApiResult<AccountDto> =
        safeApiCall { api.createAccountForClient(request) }

    suspend fun listAllRequests(status: String? = null): ApiResult<List<AccountRequestResponseDto>> =
        safeApiCall { api.listAllRequests(status = status) }.map { it.content }

    suspend fun approveAccountRequest(id: Long): ApiResult<AccountRequestResponseDto> =
        safeApiCall { api.approveAccountRequest(id) }

    suspend fun rejectAccountRequest(id: Long, reason: String): ApiResult<AccountRequestResponseDto> =
        safeApiCall { api.rejectAccountRequest(id, AccountRequestRejectDto(reason)) }

    suspend fun updateAccountStatus(id: Long, status: String): ApiResult<AccountDto> =
        safeApiCall { api.updateAccountStatus(id, AccountStatusUpdateDto(status)) }
}
