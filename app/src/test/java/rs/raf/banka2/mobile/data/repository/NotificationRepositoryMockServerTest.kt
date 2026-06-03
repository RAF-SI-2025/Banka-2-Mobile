package rs.raf.banka2.mobile.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.NotificationApi
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter

/**
 * P0-M1 N1 — Notifikacije su bile mrtve: stari Mobile [NotificationDto] je imao
 * non-null `message` + citao `relatedEntity*`, ALI banka-core
 * `NotificationDto.java` salje `body` + `referenceType`/`referenceId`. Moshi je
 * bacao na SVAKI red → cela lista je UVEK error (feature mrtav).
 *
 * Ovaj test gadja STVARNI BE JSON oblik (`body`/`reference*`) kroz MockWebServer
 * i validira da se Page<NotificationDto> parsira bez crash-a i da je popunjen.
 *
 * Pre fix-a: parsiranje baca (missing non-null `message`) → ApiResult.Failure.
 */
class NotificationRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: NotificationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder()
            .add(BigDecimalAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NotificationApi::class.java)
        repo = NotificationRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Tacan BE Page<NotificationDto> oblik: `body`, `referenceType`, `referenceId`. */
    private val beNotificationPage = """
        {
          "content": [
            {
              "id": 1,
              "type": "PAYMENT_RECEIVED",
              "title": "Primljeno placanje",
              "body": "Primili ste 100 USD",
              "read": false,
              "createdAt": "2026-05-31T10:00:00",
              "referenceType": "PAYMENT",
              "referenceId": 555
            },
            {
              "id": 2,
              "type": "ORDER_EXECUTED",
              "title": "Order izvrsen",
              "body": null,
              "read": true,
              "createdAt": "2026-05-31T09:00:00",
              "referenceType": null,
              "referenceId": null
            }
          ],
          "totalElements": 2,
          "totalPages": 1,
          "number": 0,
          "size": 20
        }
    """.trimIndent()

    @Test
    fun list_parsesBackendBodyAndReferenceFields_withoutCrash() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(beNotificationPage))

        val result = repo.list(NotificationFilter.ALL)

        assertTrue("Parsiranje BE oblika ne sme da pukne, got=$result", result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(2, result.data.content.size)

        val first = result.data.content[0]
        assertEquals(1L, first.id)
        // `body` se mapira u `message`
        assertEquals("Primili ste 100 USD", first.message)
        // `referenceType`/`referenceId` se mapiraju u `relatedEntity*`
        assertEquals("PAYMENT", first.relatedEntityType)
        assertEquals(555L, first.relatedEntityId)

        val second = result.data.content[1]
        // null body / reference ne sme da pukne
        assertNull(second.message)
        assertNull(second.relatedEntityType)
        assertNull(second.relatedEntityId)
    }

    @Test
    fun markAsRead_parsesBackendBodyField() = runTest {
        val singleDto = """
            {
              "id": 7,
              "type": "GENERIC",
              "title": "Obavestenje",
              "body": "Telo",
              "read": true,
              "createdAt": "2026-05-31T11:00:00",
              "referenceType": null,
              "referenceId": null
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(singleDto))

        val result = repo.markAsRead(7L)

        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(7L, result.data.id)
        assertEquals("Telo", result.data.message)
        assertTrue(result.data.read)
    }
}
