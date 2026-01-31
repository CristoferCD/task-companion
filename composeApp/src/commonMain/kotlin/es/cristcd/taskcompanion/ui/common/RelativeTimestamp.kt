package es.cristcd.taskcompanion.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import es.cristcd.taskcompanion.util.toDefaultFormatString
import es.cristcd.taskcompanion.util.toRelativeHumanReadableString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun RelativeTimestamp(instant: Instant, style: TextStyle = LocalTextStyle.current) {
    TooltipBox(
        tooltip = { PlainTooltip { Text(instant.toDefaultFormatString()) } },
        content = {
            Text(text = instant.toRelativeHumanReadableString(), style = style)
        },
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        state = rememberTooltipState()
    )
}