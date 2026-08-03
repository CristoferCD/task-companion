package es.cristcd.taskcompanion.ui.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import es.cristcd.taskcompanion.persistence.model.DashboardItem
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardSectionSelector(
    queries: List<RedmineQueriesByProject>,
    onSelect: (name: String, item: DashboardItem) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).heightIn(max = 600.dp).padding(48.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var groupName by remember { mutableStateOf("") }
        var groupItem by remember { mutableStateOf<DashboardItem?>(null)}

        val onGroupSelection = { name: String, item: DashboardItem ->
            groupName = name
            groupItem = item
        }

        var searchQuery by remember { mutableStateOf("") }
        val filteredItems by remember(queries) {
            derivedStateOf {
                if (searchQuery.length < 3) {
                    queries
                } else {
                    queries.mapNotNull {
                        if (it.projectName.contains(searchQuery, true)) {
                            it
                        } else if (it.queries.any { q -> q.name.contains(searchQuery, true) })  {
                            it.copy(queries = it.queries.filter { q -> q.name.contains(searchQuery, true) })
                        } else {
                            null
                        }
                    }
                }
            }
        }

        val focusRequester = remember { FocusRequester() }

        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {

                val searchState = rememberSearchBarState()
                SearchBar(
                    searchState,
                    inputField = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(painterResource(Res.drawable.search_24px), contentDescription = null)},
                            trailingIcon = {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(painterResource(Res.drawable.cancel_24px), contentDescription = null)
                                }
                            },
                            modifier = Modifier.focusRequester(focusRequester)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 32.dp).border(0.dp, Color.Transparent),
                    shape = MaterialTheme.shapes.medium,
                )

                Column(Modifier.verticalScroll(rememberScrollState()).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    ListItem(
                        content = { Text("Asignados a mi") },
                        onClick = { onGroupSelection("Asignados a mi", DashboardItem.AssignedToMe) }
                    )
                    ListItem(
                        content = { Text("Monitorizados") },
                        onClick = { onGroupSelection("Monitorizados", DashboardItem.Monitored) }
                    )
                    ListItem(
                        content = { Text("Versiones seguidas") },
                        onClick = { onGroupSelection("Versiones seguidas", DashboardItem.FollowedVersions) }
                    )
                    filteredItems.forEach { (project, queryList) ->
                        ExpandableListItemGroup(title = project, expanded = searchQuery.length >= 3) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                queryList.forEach { query ->
                                    ListItem(
                                        content = { Text(query.name) },
                                        onClick = {
                                            onGroupSelection(
                                                query.name,
                                                DashboardItem.CustomQuery(query.id, query.projectId)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )

                    FilledIconButton(
                        onClick = { onSelect(groupName, groupItem!!) },
                        enabled = groupItem != null,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.height(60.dp).width(54.dp).padding(top = 6.dp),
                        colors = IconButtonDefaults.filledIconButtonColors()
                            .let { if (groupItem != null) it.copy(containerColor = MaterialTheme.colorScheme.primaryContainer) else it }) {
                        Icon(painterResource(Res.drawable.add_24px), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun ExpandableListItemGroup(title: String, expanded: Boolean, content: @Composable () -> Unit) {
    Box {
        var expanded by remember(expanded) { mutableStateOf(expanded) }
        val degrees by animateFloatAsState(if (expanded) -90f else 90f)
        Column {
            HorizontalDivider()
            Row(modifier = Modifier.clickable { expanded = expanded.not() }.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                Image(
                    painterResource(Res.drawable.chevron_right_24px),
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees).size(16.dp),
                    colorFilter = ColorFilter.tint(Color.Gray)
                )
            }
            HorizontalDivider()
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntSize.VisibilityThreshold
                    )
                ),
                exit = shrinkVertically()
            ) {
                content()
            }
        }
    }
}