package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.RecurringOrderApi
import rs.raf.banka2.mobile.data.dto.recurringorder.CreateRecurringOrderRequest
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringCadence
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringDirection
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringMode
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderDto
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FE3 Mobile port] Wrapper oko [RecurringOrderApi] koji vraca [ApiResult].
 */
@Singleton
class RecurringOrderRepository @Inject constructor(
    private val api: RecurringOrderApi,
) {
    suspend fun listMy(): ApiResult<List<RecurringOrderDto>> =
        safeApiCall { api.listMy() }

    suspend fun create(
        listingId: Long,
        direction: RecurringDirection,
        mode: RecurringMode,
        value: BigDecimal,
        accountId: Long,
        cadence: RecurringCadence,
        firstRun: String? = null,
    ): ApiResult<RecurringOrderDto> = safeApiCall {
        api.create(
            CreateRecurringOrderRequest(
                listingId = listingId,
                direction = direction.apiValue,
                mode = mode.apiValue,
                value = value,
                accountId = accountId,
                cadence = cadence.apiValue,
                firstRun = firstRun,
            )
        )
    }

    suspend fun pause(id: Long): ApiResult<RecurringOrderDto> =
        safeApiCall { api.pause(id) }

    suspend fun resume(id: Long): ApiResult<RecurringOrderDto> =
        safeApiCall { api.resume(id) }

    suspend fun cancel(id: Long): ApiResult<Unit> =
        safeApiCall { api.cancel(id) }
}
