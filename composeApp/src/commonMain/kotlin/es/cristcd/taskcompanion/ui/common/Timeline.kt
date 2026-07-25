package es.cristcd.taskcompanion.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun TimelineNode(icon: DrawableResource? = null, content: @Composable BoxScope.(modifier: Modifier) -> Unit) {
    val circleRadius = 14.dp
    val color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val iconColor = MaterialTheme.colorScheme.onPrimaryContainer
    val iconPainter = icon?.let { painterResource(it) }
    Box(modifier = Modifier.wrapContentSize().drawBehind {
        val circleRadiusPx = circleRadius.toPx()
        drawLine(
            brush = SolidColor(color),
            start = Offset(circleRadiusPx, circleRadiusPx * 2),
            end = Offset(circleRadiusPx, this.size.height),
            strokeWidth = 2.dp.toPx()
        )

        drawCircle(
            color = color,
            radius = circleRadiusPx,
            center = Offset(circleRadiusPx, circleRadiusPx)
        )

        iconPainter?.let { painter ->
            val iconSize = circleRadiusPx * 1.25f
            this.withTransform(
                transformBlock = {
                    translate(
                        left = circleRadiusPx - iconSize / 2f,
                        top = circleRadiusPx - iconSize / 2f
                    )
                },
                drawBlock = {
                    this.drawIntoCanvas {
                        with(painter) {
                            draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(iconColor))
                        }
                    }
                })
        }


    }) {
        content(Modifier.padding(start = (circleRadius * 2) + 16.dp, bottom = 32.dp))
    }
}