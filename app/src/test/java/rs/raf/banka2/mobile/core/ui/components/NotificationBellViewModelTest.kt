package rs.raf.banka2.mobile.core.ui.components

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.notification.UnreadCountDto
import rs.raf.banka2.mobile.data.repository.NotificationRepository

/**
 * R1-601: NotificationBell polling mora biti LIFECYCLE-AWARE — petlja se gasi kad
 * se njena (lifecycle-scoped) korutina cancel-uje (npr. ekran ode u background).
 *
 * VM eksponira `suspend fun pollLoop()` koju composable pokrece kroz
 * `repeatOnLifecycle(STARTED)`. Ovde simuliramo lifecycle gasenjem job-a i
 * proveravamo da fetch PRESTANE (vise nema poziva repozitorijuma).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBellViewModelTest {

    private val repository = mockk<NotificationRepository>()

    @Test
    fun pollLoop_fetchesPeriodically_andStopsWhenCancelled() = runTest {
        coEvery { repository.getUnreadCount() } returns ApiResult.Success(UnreadCountDto(count = 3))
        val vm = NotificationBellViewModel(repository)

        // Pokreni petlju u zasebnom job-u (predstavlja lifecycle-scoped korutinu).
        val job = launch { vm.pollLoop() }

        runCurrent()
        // Prvi fetch je odmah.
        assertEquals(3, vm.unreadCount.value)
        coVerify(exactly = 1) { repository.getUnreadCount() }

        // Posle jednog intervala (30s) sledi novi fetch.
        advanceTimeBy(30_001L)
        runCurrent()
        coVerify(exactly = 2) { repository.getUnreadCount() }

        // Lifecycle STOP → cancel korutine → petlja prestaje.
        job.cancel()
        advanceTimeBy(120_000L)
        runCurrent()

        // Nema dodatnih fetch-eva posle cancel-a (lifecycle-aware gasenje).
        coVerify(exactly = 2) { repository.getUnreadCount() }
    }
}
