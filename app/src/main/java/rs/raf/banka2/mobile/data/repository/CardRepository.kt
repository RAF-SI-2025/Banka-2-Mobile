package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.CardApi
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.dto.card.CardLimitUpdateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestCreateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestRejectDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: CardApi
) {
    suspend fun myCards(): ApiResult<List<CardDto>> = safeApiCall { api.getMyCards() }

    suspend fun cardsForAccount(accountId: Long): ApiResult<List<CardDto>> =
        safeApiCall { api.getCardsForAccount(accountId) }

    suspend fun block(id: Long): ApiResult<CardDto> = safeApiCall { api.blockCard(id) }

    suspend fun unblock(id: Long): ApiResult<CardDto> = safeApiCall { api.unblockCard(id) }

    suspend fun deactivate(id: Long): ApiResult<CardDto> = safeApiCall { api.deactivateCard(id) }

    suspend fun updateLimit(id: Long, limit: Double): ApiResult<CardDto> =
        safeApiCall { api.updateLimit(id, CardLimitUpdateDto(limit)) }

    suspend fun submitRequest(
        accountId: Long,
        limit: Double,
        cardType: String
    ): ApiResult<CardRequestResponseDto> =
        safeApiCall { api.submitCardRequest(CardRequestCreateDto(accountId, limit, cardType)) }

    suspend fun confirmRequest(id: Long, otpCode: String): ApiResult<CardRequestResponseDto> =
        safeApiCall {
            api.confirmCardRequest(id, rs.raf.banka2.mobile.data.dto.payment.OtpVerifyRequest(otpCode))
        }

    suspend fun myRequests(): ApiResult<List<CardRequestResponseDto>> =
        safeApiCall { api.getMyCardRequests() }.map { it.content }

    suspend fun listAllRequests(status: String? = null): ApiResult<List<CardRequestResponseDto>> =
        safeApiCall { api.listAllCardRequests(status = status) }.map { it.content }

    suspend fun approveRequest(id: Long): ApiResult<CardRequestResponseDto> =
        safeApiCall { api.approveCardRequest(id) }

    suspend fun rejectRequest(id: Long, reason: String): ApiResult<CardRequestResponseDto> =
        safeApiCall { api.rejectCardRequest(id, CardRequestRejectDto(reason)) }
}
