package rs.raf.banka2.mobile.feature.watchlist

import org.junit.Assert.assertEquals
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError

/**
 * Unit testovi za WatchlistViewModel.errorMessage / AddToWatchlistViewModel.friendly
 * mapping ApiError kind-a u user-facing srpsku poruku.
 */
class WatchlistViewModelErrorMappingTest {

    private fun err(kind: ApiError.Kind, message: String = "raw", code: Int? = null) =
        ApiError(httpCode = code, message = message, kind = kind)

    @Test
    fun errorMessage_conflict_isItemExists() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Conflict), "default")
        assertEquals("Stavka vec postoji u listi.", msg)
    }

    @Test
    fun errorMessage_validation_passesThroughBeMessage() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Validation, "Naziv ne moze biti prazan"), "default")
        assertEquals("Naziv ne moze biti prazan", msg)
    }

    @Test
    fun errorMessage_unauthorized_isFriendlyAuth() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Unauthorized), "default")
        assertEquals("Niste prijavljeni.", msg)
    }

    @Test
    fun errorMessage_forbidden_isFriendlyAuth() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Forbidden), "default")
        assertEquals("Nemate dozvolu za ovu akciju.", msg)
    }

    @Test
    fun errorMessage_notFound_isFriendlyAuth() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.NotFound), "default")
        assertEquals("Lista nije pronadjena.", msg)
    }

    @Test
    fun errorMessage_unknownWithBlankMessage_returnsDefault() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Unknown, ""), "Default poruka.")
        assertEquals("Default poruka.", msg)
    }

    @Test
    fun errorMessage_serverWithMessage_returnsRawMessage() {
        val msg = WatchlistViewModel.errorMessage(err(ApiError.Kind.Server, "internal server error"), "default")
        assertEquals("internal server error", msg)
    }

    @Test
    fun friendly_conflict_isItemExists() {
        val msg = AddToWatchlistViewModel.friendly(err(ApiError.Kind.Conflict))
        assertEquals("Hartija je vec u toj listi.", msg)
    }

    @Test
    fun friendly_validation_passesThroughBeMessage() {
        val msg = AddToWatchlistViewModel.friendly(err(ApiError.Kind.Validation, "duplicate name"))
        assertEquals("duplicate name", msg)
    }

    @Test
    fun friendly_unknownBlankMessage_returnsFallback() {
        val msg = AddToWatchlistViewModel.friendly(err(ApiError.Kind.Unknown, ""))
        assertEquals("Doslo je do greske.", msg)
    }
}
