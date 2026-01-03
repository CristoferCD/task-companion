package es.cristcd.taskcompanion.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun RelativeTimestamp(instant: Instant, style: TextStyle = LocalTextStyle.current) {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    var finalText by remember { mutableStateOf(localDateTime.toString()) }
    val now = Clock.System.now()
    if (now > instant) {
        finalText = when (val duration = now - instant) {
            in 0.minutes ..< 1.minutes ->  "Ahora"
            in 1.minutes ..< 1.hours ->  "Hace ${duration.inWholeMinutes.toInt()} minutos"
            in 1.hours ..< 1.days ->  "Hace ${duration.inWholeHours.toInt()} horas"
            in 1.days ..< 30.days ->  "Hace ${duration.inWholeDays.toInt()} dias"
            else -> localDateTime.toString()
        }
    }

    TooltipBox(
        tooltip = { PlainTooltip { Text(localDateTime.toString()) } },
        content = {
            Text(text =  finalText, style = style)
        },
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState()
    )
}