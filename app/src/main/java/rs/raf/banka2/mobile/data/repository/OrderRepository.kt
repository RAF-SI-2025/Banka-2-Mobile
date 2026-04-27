package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.OrderApi
import rs.raf.banka2.mobile.data.dto.order.CreateOrderDto
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val api: OrderApi
) {
    suspend fun create(request: CreateOrderDto): ApiResult<OrderDto> =
        safeApiCall { api.createOrder(request) }

    suspend fun myOrders(page: Int = 0, size: Int = 50): ApiResult<List<OrderDto>> =
        safeApiCall { api.getMyOrders(page = page, size = size) }.map { it.content }

    suspend fun listAll(status: String? = null, page: Int = 0, size: Int = 50): ApiResult<List<OrderDto>> =
        safeApiCall { api.listAll(status = status, page = page, size = size) }.map { it.content }

    suspend fun byId(id: Long): ApiResult<OrderDto> = safeApiCall { api.getOrder(id) }

    suspend fun approve(id: Long): ApiResult<OrderDto> = safeApiCall { api.approve(id) }

    suspend fun decline(id: Long, partialQuantity: Int? = null): ApiResult<OrderDto> =
        safeApiCall { api.decline(id, partialQuantity) }
}
