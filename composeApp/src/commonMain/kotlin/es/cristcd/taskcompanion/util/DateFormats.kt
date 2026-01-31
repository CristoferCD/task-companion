package es.cristcd.taskcompanion.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

fun Instant.toRelativeHumanReadableString(): String {
    return when (val duration = Clock.System.now() - this) {
        in 0.minutes ..< 1.minutes ->  "Ahora"
        in 1.minutes ..< 1.hours ->  "Hace ${duration.inWholeMinutes.toInt()} minutos"
        in 1.hours ..< 1.days ->  "Hace ${duration.inWholeHours.toInt()} horas"
        in 1.days ..< 30.days ->  "Hace ${duration.inWholeDays.toInt()} dias"
        else -> this.toDefaultFormatString()
    }
}

fun Instant.toDefaultFormatString(): String {
    return this.toLocalDateTime(TimeZone.currentSystemDefault()).format(
        LocalDateTime.Format { year(); char('-'); monthNumber(); char('-'); day(); char(' '); hour(); char(':'); minute()}
    )
}