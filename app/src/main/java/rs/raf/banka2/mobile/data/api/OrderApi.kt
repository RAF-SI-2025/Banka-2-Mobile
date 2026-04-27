package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.order.CreateOrderDto
import rs.raf.banka2.mobile.data.dto.order.OrderDto

interface OrderApi {

    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderDto): Response<OrderDto>

    @GET("orders/my")
    suspend fun getMyOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<OrderDto>>

    @GET("orders")
    suspend fun listAll(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<OrderDto>>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Long): Response<OrderDto>

    @PATCH("orders/{id}/approve")
    suspend fun approve(@Path("id") id: Long): Response<OrderDto>

    @PATCH("orders/{id}/decline")
    suspend fun decline(
        @Path("id") id: Long,
        @Query("quantity") quantity: Int? = null
    ): Response<OrderDto>
}
