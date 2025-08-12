package hr.squidpai.zetlive.news

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val RFC_822_2_DIGIT_YEAR_DATE_TIME =
    DateTimeFormatter.ofPattern("EEE, dd MMM yy HH:mm:ss zzz")

val String.rssDate: LocalDateTime
    get() = try {
        LocalDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME)
    } catch (_: DateTimeParseException) {
        LocalDateTime.parse(this, RFC_822_2_DIGIT_YEAR_DATE_TIME)
    }

private val localFormat = DateTimeFormatter.ofPattern(
    "EEE, dd. MMMM yyyy., HH:mm",
    Locale.forLanguageTag("hr")
)

fun LocalDateTime.toFormattedString(): String = format(localFormat)
