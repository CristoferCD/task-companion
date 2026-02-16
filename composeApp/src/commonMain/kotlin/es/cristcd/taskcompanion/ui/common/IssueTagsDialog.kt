@file:OptIn(ExperimentalMaterial3Api::class)

package es.cristcd.taskcompanion.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.cristcd.taskcompanion.issue.dto.TagDto
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.Res
import task_companion.composeapp.generated.resources.bookmark_add_24px
import task_companion.composeapp.generated.resources.bookmark_remove_24px

@Composable
fun IssueTagsDialog(selectedTags: List<TagDto>, availableTags: List<TagDto>, onAssign: (name: String) -> Unit, onRemove: (name: String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp)) {
                val tagsBySelected = availableTags.groupBy { it in selectedTags }
                Text("Assigned")
                tagsBySelected[true]?.forEach { tag ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(tag.name) },
                        leadingContent = {
                            Box(Modifier.padding(end = 8.dp).size(18.dp).clip(CircleShape).background(Color(tag.color)))
                        },
                        trailingContent = {
                            IconButton(onClick = { onRemove(tag.name) } ) {
                                Icon(painterResource(Res.drawable.bookmark_remove_24px), contentDescription = null)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Available")
                tagsBySelected[false]?.forEach { tag ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(tag.name) },
                        leadingContent = {
                            Box(Modifier.padding(end = 8.dp).size(18.dp).clip(CircleShape).background(Color(tag.color)))
                        },
                        trailingContent = {
                            IconButton(onClick = { onAssign(tag.name) } ) {
                                Icon(painterResource(Res.drawable.bookmark_add_24px), contentDescription = null)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onConfirm, modifier = Modifier.align(Alignment.End)) {
                    Text("Save")
                }
            }
        }
    }
}