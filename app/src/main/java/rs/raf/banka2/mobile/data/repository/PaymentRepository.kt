package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.PaymentApi
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import rs.raf.banka2.mobile.data.dto.payment.OtpResponseDto
import rs.raf.banka2.mobile.data.dto.payment.OtpVerifyRequest
import rs.raf.banka2.mobile.data.dto.payment.OtpVerifyResponse
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val api: PaymentApi
) {
    suspend fun create(request: CreatePaymentRequestDto): ApiResult<PaymentResponseDto> =
        safeApiCall { api.createPayment(request) }

    suspend fun getMyPayments(
        page: Int = 0,
        limit: Int = 20,
        accountNumber: String? = null,
        status: String? = null
    ): ApiResult<List<PaymentListItemDto>> =
        safeApiCall {
            api.getMyPayments(page = page, limit = limit, accountNumber = accountNumber, status = status)
        }.map { it.content }

    suspend fun getPaymentById(id: Long): ApiResult<PaymentResponseDto> =
        safeApiCall { api.getPaymentById(id) }

    suspend fun requestOtpToMobile(): ApiResult<Unit> = mapToUnit(safeApiCall { api.requestOtp() })

    suspend fun requestOtpViaEmail(): ApiResult<Unit> = mapToUnit(safeApiCall { api.requestOtpViaEmail() })

    suspend fun getActiveOtp(): ApiResult<OtpResponseDto> = safeApiCall { api.getActiveOtp() }

    suspend fun verifyOtp(code: String): ApiResult<OtpVerifyResponse> =
        safeApiCall { api.verifyOtp(OtpVerifyRequest(code)) }

    suspend fun downloadReceipt(id: Long): ApiResult<okhttp3.ResponseBody> =
        safeApiCall { api.downloadReceipt(id) }

    private fun <T> mapToUnit(result: ApiResult<T>): ApiResult<Unit> = result.map { }
}
