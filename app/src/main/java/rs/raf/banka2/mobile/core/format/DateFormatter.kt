package rs.raf.banka2.mobile.core.format

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Backend salje datume kao ISO-8601 (sa ili bez milisekundi, sa ili bez timezone-a).
 * Radimo defenzivno — pokusavamo nekoliko parsera redom dok ne uhvatimo.
 */
object DateFormatter {

    private val srLocale: Locale = Locale.Builder().setLanguage("sr").setRegion("RS").build()

    private val dateOutput: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", srLocale)
    private val dateTimeOutput: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", srLocale)

    private val parsers: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd HH:mm[:ss]")
            .toFormatter()
    )

    fun formatDate(input: String?): String {
        val date = parseDate(input) ?: return "—"
        return date.format(dateOutput)
    }

    fun formatDateTime(input: String?): String {
        val dateTime = parseDateTime(input) ?: return "—"
        return dateTime.format(dateTimeOutput)
    }

    fun parseDate(input: String?): LocalDate? {
        if (input.isNullOrBlank()) return null
        for (parser in parsers) {
            try {
                return when {
                    parser === DateTimeFormatter.ISO_LOCAL_DATE -> LocalDate.parse(input, parser)
                    else -> {
                        val parsed = parser.parseBest(
                            input,
                            OffsetDateTime::from,
                            LocalDateTime::from,
                            LocalDate::from
                        )
                        when (parsed) {
                            is OffsetDateTime -> parsed.toLocalDate()
                            is LocalDateTime -> parsed.toLocalDate()
                            is LocalDate -> parsed
                            else -> null
                        }
                    }
                }
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        return null
    }

    fun parseDateTime(input: String?): LocalDateTime? {
        if (input.isNullOrBlank()) return null
        for (parser in parsers) {
            try {
                val parsed = parser.parseBest(
                    input,
                    ZonedDateTime::from,
                    OffsetDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from
                )
                return when (parsed) {
                    is ZonedDateTime -> parsed.toLocalDateTime()
                    is OffsetDateTime -> parsed.toLocalDateTime()
                    is LocalDateTime -> parsed
                    is LocalDate -> parsed.atStartOfDay()
                    else -> null
                }
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        return null
    }

    fun nowIsoLocalDate(): String =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)

    /**
     * R1-597: validacija datumskog filtera PRE BE poziva. Slobodan unos
     * ("abc", "2026-13-99", "30.05.2026") je ranije isao direktno BE-u koji radi
     * `LocalDate.parse`/`LocalDateTime.parse` → `DateTimeParseException` → 400/500.
     * Prihvatamo SAMO striktni `YYYY-MM-DD` (ISO_LOCAL_DATE) jer BE date filteri
     * ocekuju bas taj oblik (servisi ga normalizuju u `...T00:00:00`).
     *
     * Prazan/blank string je VALIDAN (znaci "bez filtera") — pozivalac sam odlucuje
     * da li blank tretira kao null.
     */
    fun isValidIsoDate(input: String?): Boolean {
        if (input.isNullOrBlank()) return true
        return try {
            LocalDate.parse(input.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
