package es.cristcd.taskcompanion.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.cristcd.taskcompanion.issue.IssueService
import es.cristcd.taskcompanion.issue.dto.IssueListItemDto
import es.cristcd.taskcompanion.issue.dto.TagDto
import es.cristcd.taskcompanion.redmine.model.IdString
import es.cristcd.taskcompanion.tracker.SettingsCache
import es.cristcd.taskcompanion.ui.screen.issue.abbreviate
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterialApi::class, ExperimentalTime::class)
@Composable
fun TaskCard(issue: IssueListItemDto, newItemAlphaAnimation: Animatable<Float, AnimationVector1D>, onClick: () -> Unit = {}, onStart: () -> Unit, onUpdateTags: (tags: List<TagDto>) -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.small
    ) {
        val statusColor = SettingsCache.getStatusColor(issue.status.id) ?: Color.DarkGray
        Box(modifier = Modifier.background(statusColor.copy(alpha = 0.08f)).priorityBorder(issue.priority.name).padding(4.dp).height(IntrinsicSize.Min)) {
            var optionsOverlay by remember { mutableStateOf(false) }
            Row(modifier = Modifier.alpha(if (optionsOverlay) 0.0f else 1.0f)) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            StatusBadge(issue.status)
                            TaskParent(issue)
                        }
                        PriorityIcon(issue.priority.name)
                    }
                    Text(text = issue.subject, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IssueTags(issue.tags, modifier = Modifier.weight(1f, true))
                        TaskUpdatedAt(issue.updatedOn, issue.recentlyChanged, newItemAlphaAnimation, modifier = Modifier.weight(1f, false))

                    }
                }
                Box(modifier = Modifier.fillMaxHeight().width(16.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { optionsOverlay = true }) {
                        Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
                    }
                }
            }

            var tagsDialog by remember { mutableStateOf(false) }
            if (optionsOverlay) {
                IssueOptionsOverlay(onDismiss = { optionsOverlay = false }, onStart = onStart, onEditTags = { tagsDialog = true })
            }
            if (tagsDialog) {
                var availableTags by remember { mutableStateOf(emptyList<TagDto>()) } //FIXME
                LaunchedEffect(true) {
                    availableTags = IssueService.listTags()
                }
                var selectedTags by remember { mutableStateOf(issue.tags.toList())}
                IssueTagsDialog(
                    selectedTags,
                    availableTags,
                    onAssign = { name -> availableTags.find { it.name == name }?.let { selectedTags = selectedTags + it } },
                    onRemove = { name -> availableTags.find { it.name == name }?.let { selectedTags = selectedTags.filter{ st -> it != st} } },
                    onConfirm = { onUpdateTags(selectedTags); tagsDialog = false },
                    onDismiss = { tagsDialog = false })
            }
        }

    }
}

@Composable
fun TaskUpdatedAt(updatedOn: Instant?, recentlyChanged: Boolean, newItemAlphaAnimation: Animatable<Float, AnimationVector1D>, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.let {
            if (recentlyChanged) {
                it.border(2.dp, Color.Red.copy(alpha = newItemAlphaAnimation.value), MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            } else {
                it
            }
        }) {
        if (recentlyChanged) {
            Box(modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = newItemAlphaAnimation.value)).size(20.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(Res.drawable.notification_important_24px), null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
        Icon(painterResource(Res.drawable.update_24px), "Updated at", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
        updatedOn?.let { RelativeTimestamp(it, style = MaterialTheme.typography.bodySmall) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueTags(tags: List<TagDto>, modifier: Modifier = Modifier) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.Center, maxLines = 2, modifier = modifier) {
        tags.forEach { tag ->
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .drawBehind {
                        if (tag.color != null) {
                            drawRect(
                                color = Color(tag.color),
                                topLeft = Offset(0f, 0f),
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                    }
                    .padding(vertical = 2.dp, horizontal = 8.dp)
            ) {
                TooltipBox(
                    tooltip = { PlainTooltip { Text(tag.name) } },
                    content = {
                        Text(tag.name.abbreviate(10), style = MaterialTheme.typography.labelSmall)
                    },
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    state = rememberTooltipState()
                )
            }
        }
    }
}

@Composable
private fun IssueOptionsOverlay(onDismiss: () -> Unit, onStart: () -> Unit, onEditTags: () -> Unit) {
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
            IconButton(onClick = { onStart(); onDismiss()}) {
                Icon(painterResource(Res.drawable.play_arrow_24px), null)
            }
            IconButton(onClick = { onEditTags(); onDismiss()}) {
                Icon(painterResource(Res.drawable.bookmarks_24px), null)
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(16.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onDismiss() }) {
                Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
            }
        }
    }
}

@Composable
private fun Modifier.priorityBorder(priority: String?): Modifier {
    val color = priorityColor(priority)?.copy(alpha = .8f)
    return if (color != null) {
        border(1.dp, color, MaterialTheme.shapes.small)
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
    val color = SettingsCache.getStatusColor(status.id) ?: Color.DarkGray
    Box(modifier = Modifier.background(color = color, shape = MaterialTheme.shapes.extraSmall), contentAlignment = Alignment.Center) {
        Text(text = status.name ?: "", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
    }
}