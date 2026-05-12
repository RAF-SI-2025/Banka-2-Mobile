package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.SavingsApi
import rs.raf.banka2.mobile.data.dto.savings.OpenDepositRequest
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsRateDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsTransactionDto
import rs.raf.banka2.mobile.data.dto.savings.ToggleAutoRenewRequest
import rs.raf.banka2.mobile.data.dto.savings.WithdrawEarlyRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsRepository @Inject constructor(private val api: SavingsApi) {
    suspend fun openDeposit(req: OpenDepositRequest): ApiResult<SavingsDepositDto> =
        safeApiCall { api.openDeposit(req) }

    suspend fun listMy(): ApiResult<List<SavingsDepositDto>> =
        safeApiCall { api.listMy() }

    suspend fun getDeposit(id: Long): ApiResult<SavingsDepositDto> =
        safeApiCall { api.getDeposit(id) }

    suspend fun getTransactions(id: Long): ApiResult<List<SavingsTransactionDto>> =
        safeApiCall { api.getTransactions(id) }

    suspend fun toggleAutoRenew(id: Long, autoRenew: Boolean): ApiResult<SavingsDepositDto> =
        safeApiCall { api.toggleAutoRenew(id, ToggleAutoRenewRequest(autoRenew)) }

    suspend fun withdrawEarly(id: Long, otpCode: String): ApiResult<SavingsDepositDto> =
        safeApiCall { api.withdrawEarly(id, WithdrawEarlyRequest(otpCode)) }

    suspend fun getRates(currency: String? = null): ApiResult<List<SavingsRateDto>> =
        safeApiCall { api.getRates(currency) }
}
