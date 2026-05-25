package rs.raf.banka2.mobile.feature.notifications

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter
import rs.raf.banka2.mobile.data.dto.notification.NotificationPageDto
import rs.raf.banka2.mobile.data.dto.notification.UnreadCountDto
import rs.raf.banka2.mobile.data.repository.NotificationRepository

/**
 * TODO_final C2 #4 — Notifications VM behavior test.
 *
 * Pokrivamo: refresh (loading→success), filter switch, mark-as-read
 * optimisticki update + rollback na failure, mark-all-as-read,
 * navigate event na klik notifikacije sa deep-link target-om.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<NotificationRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleNotification(
        id: Long,
        read: Boolean = false,
        type: String = "PAYMENT_RECEIVED",
        entity: String? = "PAYMENT",
        entityId: Long? = id,
    ) = NotificationDto(
        id = id,
        type = type,
        title = "Notif $id",
        message = "Msg $id",
        read = read,
        createdAt = "2026-05-26T10:00:00Z",
        relatedEntityType = entity,
        relatedEntityId = entityId,
    )

    private fun makeVm(): NotificationsViewModel {
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.Success(
            NotificationPageDto(
                content = listOf(sampleNotification(1), sampleNotification(2)),
                totalElements = 2,
                totalPages = 1,
                number = 0,
                size = 20,
            )
        )
        coEvery { repository.getUnreadCount() } returns ApiResult.Success(UnreadCountDto(count = 2))
        return NotificationsViewModel(repository)
    }

    @Test
    fun `init loads items and unread count`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(2, state.items.size)
        assertEquals(2, state.unreadCount)
        assertEquals(null, state.error)
    }

    @Test
    fun `setFilter to UNREAD reloads with read=false`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery {
            repository.list(NotificationFilter.UNREAD, any(), any())
        } returns ApiResult.Success(
            NotificationPageDto(
                content = listOf(sampleNotification(3)),
                totalPages = 1,
                number = 0,
            )
        )

        vm.setFilter(NotificationFilter.UNREAD)
        advanceUntilIdle()

        assertEquals(NotificationFilter.UNREAD, vm.state.value.filter)
        assertEquals(1, vm.state.value.items.size)
        assertEquals(3L, vm.state.value.items[0].id)
        coVerify { repository.list(NotificationFilter.UNREAD, 0, 20) }
    }

    @Test
    fun `setFilter to same value is no-op`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        // pocetni state je ALL — drugi set-filter na ALL ne sme da pozove BE.
        vm.setFilter(NotificationFilter.ALL)
        advanceUntilIdle()

        // 1 poziv iz init + 1 (eventualno) — ali ovde ne ocekujemo dodatni.
        coVerify(exactly = 1) { repository.list(NotificationFilter.ALL, 0, 20) }
    }

    @Test
    fun `onRowClick marks as read optimistically and calls repository`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery { repository.markAsRead(1L) } returns ApiResult.Success(
            sampleNotification(id = 1, read = true)
        )

        // Drugi unreadCount fetch posle markAsRead
        coEvery { repository.getUnreadCount() } returns ApiResult.Success(UnreadCountDto(count = 1))

        val targetNotification = vm.state.value.items.first { it.id == 1L }
        vm.onRowClick(targetNotification)
        advanceUntilIdle()

        // Optimisticki: item.read = true
        val updated = vm.state.value.items.first { it.id == 1L }
        assertTrue(updated.read)
        coVerify { repository.markAsRead(1L) }
    }

    @Test
    fun `onRowClick rollbacks on repository failure`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery { repository.markAsRead(1L) } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Server down", kind = ApiError.Kind.Server)
        )

        val targetNotification = vm.state.value.items.first { it.id == 1L }
        vm.onRowClick(targetNotification)
        advanceUntilIdle()

        // Rollback — read = false
        val rolledBack = vm.state.value.items.first { it.id == 1L }
        assertFalse(rolledBack.read)
    }

    @Test
    fun `onRowClick emits navigate event for PAYMENT entity`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery { repository.markAsRead(any()) } returns ApiResult.Success(sampleNotification(1, true))

        val targetNotification = vm.state.value.items.first { it.id == 1L }

        val collected = mutableListOf<NotificationsEvent>()
        val job = CoroutineScope(dispatcher).launch {
            vm.events.collect { collected.add(it) }
        }

        vm.onRowClick(targetNotification)
        advanceUntilIdle()
        job.cancel()

        val nav = collected.firstOrNull { it is NotificationsEvent.Navigate }
        assertTrue("Expected Navigate event, got=$collected", nav != null)
        nav as NotificationsEvent.Navigate
        assertEquals(NotificationTarget.Payments, nav.target)
    }

    @Test
    fun `markAllAsRead updates all items to read`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery { repository.markAllAsRead() } returns ApiResult.Success(Unit)
        coEvery { repository.getUnreadCount() } returns ApiResult.Success(UnreadCountDto(count = 0))

        vm.markAllAsRead()
        advanceUntilIdle()

        assertTrue(vm.state.value.items.all { it.read })
        assertEquals(0, vm.state.value.unreadCount)
    }

    @Test
    fun `refresh failure exposes error in state`() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        coEvery {
            repository.list(any(), any(), any())
        } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Server down", kind = ApiError.Kind.Server)
        )

        vm.refresh()
        advanceUntilIdle()

        assertEquals("Server down", vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }
}
