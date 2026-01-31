package es.cristcd.taskcompanion.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.cristcd.taskcompanion.redmine.dto.IssueListItemDto
import es.cristcd.taskcompanion.redmine.model.IdString
import es.cristcd.taskcompanion.tracker.SettingsCache
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.collections.get
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterialApi::class, ExperimentalTime::class)
@Composable
fun TaskCard(issue: IssueListItemDto, newItemAlphaAnimation: Animatable<Float, AnimationVector1D>, onClick: () -> Unit = {}, onStart: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.small
    ) {
        val statusColor = SettingsCache.redmineStatusColors[issue.status.id] ?: Color.DarkGray
        Box(modifier = Modifier.background(statusColor.copy(alpha = 0.08f)).priorityBorder(issue.priority.name).padding(4.dp).height(IntrinsicSize.Min)) {
            var optionsOverlay by remember { mutableStateOf(false) }
            Row(modifier = Modifier.alpha(if (optionsOverlay) 0.0f else 1.0f)) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(issue.status)
                        TaskParent(issue)
                    }
                    Text(text = issue.subject, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (issue.recentlyChanged) {
                            Box(modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = newItemAlphaAnimation.value)).size(20.dp), contentAlignment = Alignment.Center) {
                                Icon(painterResource(Res.drawable.notification_important_24px), null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text("Actualizado:", style = MaterialTheme.typography.bodySmall)
                        issue.updatedOn?.let { RelativeTimestamp(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PriorityIcon(issue.priority.name)
                }
                Box(modifier = Modifier.fillMaxHeight().width(16.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { optionsOverlay = true }) {
                        Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
                    }
                }
            }
            if (optionsOverlay) {
                IssueOptionsOverlay(onDismiss = { optionsOverlay = false }, onStart = onStart)
            }
        }

    }
}

@Composable
private fun TaskParent(issue: IssueListItemDto) {
    if (issue.fixedVersion?.name != null) {
        Text(text = issue.fixedVersion.name, style = MaterialTheme.typography.labelSmall)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Res.drawable.domain_24px), contentDescription = "Externo", modifier = Modifier.height(12.dp))
            Text(text = issue.project.name ?: "", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun IssueOptionsOverlay(onDismiss: () -> Unit, onStart: () -> Unit) {
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
            IconButton(onClick = { onStart(); onDismiss()}) {
                Icon(painterResource(Res.drawable.play_arrow_24px), null)
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(16.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onDismiss() }) {
                Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
            }
        }
    }
}

private fun Modifier.priorityBorder(priority: String?): Modifier {
    val color = priorityColor(priority)?.copy(alpha = .8f)
    return if (color != null) {
        border(1.dp, color)
    } else this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityIcon(priority: String?) {
    TooltipBox(
        tooltip = { PlainTooltip { Text(priority ?: "") } },
        content = {
            when (priority) {
                "Baja" -> Icon(painterResource(Res.drawable.arrow_cool_down_24px), contentDescription = "", tint = Color(0xff003049))
                "Normal" -> return@TooltipBox
                "Alta" -> Icon(painterResource(Res.drawable.arrow_shape_up_24px), contentDescription = "", tint = Color(0xfffcbf49))
                "Urgente" -> Icon(painterResource(Res.drawable.arrow_shape_up_stack_24px), contentDescription = "", tint = Color(0xfff77f00))
                "Inmediata" -> Icon(painterResource(Res.drawable.arrow_shape_up_stack_2_24px), contentDescription = "", tint = Color(0xffd62828))
            }
        },
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        state = rememberTooltipState()
    )
}

fun priorityColor(priority: String?): Color? {
    return when (priority) {
        "Baja" -> Color(0xff003049)
        "Alta" -> Color(0xfffcbf49)
        "Urgente" -> Color(0xfff77f00)
        "Inmediata" -> Color(0xffd62828)
        else -> null
    }
}

@Composable
fun StatusBadge(status: IdString) {
    val color = SettingsCache.redmineStatusColors[status.id] ?: Color.DarkGray
    Box(modifier = Modifier.background(color = color, shape = MaterialTheme.shapes.extraSmall), contentAlignment = Alignment.Center) {
        Text(text = status.name ?: "", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
    }
}