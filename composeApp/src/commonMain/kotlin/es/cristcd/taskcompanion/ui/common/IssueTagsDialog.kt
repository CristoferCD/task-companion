@file:OptIn(ExperimentalMaterial3Api::class)

package es.cristcd.taskcompanion.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(vertical = 18.dp)) {
            Column(Modifier.padding(24.dp)) {
                LazyColumn(Modifier.weight(1f)) {
                    val tagsBySelected = availableTags.groupBy { it in selectedTags }
                    item {
                        Text("Assigned", style = MaterialTheme.typography.titleSmallEmphasized)
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                    }
                    items(tagsBySelected[true] ?: emptyList()) { tag ->
                        ListItem(
                            modifier = Modifier.height(36.dp),
                            headlineContent = { Text(tag.name, style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = {
                                if (tag.color != null) {
                                    Box(
                                        Modifier.size(12.dp).clip(CircleShape)
                                            .background(Color(tag.color))
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onRemove(tag.name) }, modifier = Modifier.size(24.dp)) {
                                    Icon(painterResource(Res.drawable.bookmark_remove_24px), contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                    item {
                        Text("Available", style = MaterialTheme.typography.titleSmallEmphasized)
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                    }
                    items(tagsBySelected[false] ?: emptyList()) { tag ->
                        ListItem(
                            modifier = Modifier.height(36.dp),
                            headlineContent = { Text(tag.name, style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = {
                                if (tag.color != null) {
                                    Box(
                                        Modifier.size(12.dp).clip(CircleShape)
                                            .background(Color(tag.color))
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onAssign(tag.name) }, modifier = Modifier.size(24.dp)) {
                                    Icon(painterResource(Res.drawable.bookmark_add_24px), contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
                Button(onClick = onConfirm, modifier = Modifier.align(Alignment.End).padding(top = 16.dp)) {
                    Text("Save")
                }
            }
        }
    }
}