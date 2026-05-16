package es.cristcd.taskcompanion.ui.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import es.cristcd.taskcompanion.search.dto.SearchResultDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onSelect: (id: Long) -> Unit,
    onDismiss: () -> Unit,
    searching: Boolean,
    searchResults: List<SearchResultDto>,
    onSearch: (String) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(1000.dp).heightIn(max = 600.dp).padding(48.dp)
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            var text by remember { mutableStateOf("") }
            var expanded by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }
            SearchBar(
                modifier = Modifier.width(350.dp).padding(top = 0.dp, bottom = 8.dp, end = 8.dp),
                inputField = {
                    TextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onSearch(it)
                            if (it.isNotEmpty() && !expanded) {
                                expanded = true
                            }
                        },
                        placeholder = { Text(text = "Search...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.focusRequester(focusRequester).onKeyEvent {
                            if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                if (text.toLongOrNull() != null) {
                                    onSelect(text.toLong())
                                } else if (!searching && searchResults.size == 1) {
                                    onSelect(searchResults.first().redmineId)
                                }
                                true
                            }
                            false
                        },
                    )
                },
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                    if (!expanded) {
                        onDismiss()
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults) {
                        val containerColor =
                            if (it.statusColor != null) Color(it.statusColor).copy(alpha = 0.15f) else ListItemDefaults.containerColor
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = containerColor),
                            overlineContent = { Text(it.redmineId.toString()) },
                            headlineContent = { Text(it.subject, style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.clickable(onClick = { onSelect(it.redmineId) })
                        )
                    }
                    if (text.toLongOrNull() != null) {
                        item {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                headlineContent = { Text("Ir a $text", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.padding(all = 12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(onClick = { onSelect(text.toLong()) })
                            )
                        }
                    }
                    if (searching) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text("Searching...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }

}