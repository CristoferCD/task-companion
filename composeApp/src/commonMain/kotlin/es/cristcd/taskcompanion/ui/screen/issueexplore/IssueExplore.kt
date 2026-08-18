package es.cristcd.taskcompanion.ui.screen.issueexplore

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import es.cristcd.taskcompanion.filter.ColumnDefinition
import es.cristcd.taskcompanion.redmine.model.Query
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
import es.cristcd.taskcompanion.ui.screen.dashboard.RedmineQueriesByProject
import es.cristcd.taskcompanion.ui.screen.version.IssueTable
import es.cristcd.taskcompanion.util.popBackStackIfResumed
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.Res
import task_companion.composeapp.generated.resources.arrow_back_24px

@Composable
fun IssueExploreScreen(filter: IssueFilter, navController: NavHostController, viewmodel: IssueExploreViewmodel = viewModel { IssueExploreViewmodel() }) {
    LaunchedEffect(filter) {
        viewmodel.load(filter)
    }

    val state = viewmodel.exploreState.collectAsState()
    when (val result = state.value) {
        is ExploreResult.Loading -> FullscreenLoading(onCancel = { navController.popBackStackIfResumed() })
        is ExploreResult.Ok -> IssueExplore(result, navController, viewmodel::onFilterChange, viewmodel::updateColumnSelection)
    }
}

@Composable
fun IssueExplore(
    result: ExploreResult.Ok,
    navController: NavHostController,
    onFilterChange: (IssueFilter) -> Unit,
    onColumnVisibilityChange: (ColumnDefinition) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackIfResumed() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        val padding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 8.dp,
            start = 8.dp,
            end = 8.dp
        )

        Column(modifier = Modifier.padding(padding)) {
            FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedQuery = remember(result) {
                    derivedStateOf {
                        result.filterOptions.queriesByProject.flatMap { it.queries }
                            .find { query -> query.id == result.filter.customQueryId }
                    }
                }
                DropdownQueriesFilterOption(result.filterOptions.queriesByProject, selectedQuery.value, onSelectionChange = { onFilterChange(result.filter.copy(customQueryId = it.id, projectId = it.projectId))})

            }

            val issues = result.issues.collectAsLazyPagingItems()
            IssueTable(
                issues,
                result.resultColumns,
                onColumnVisibilityChange = onColumnVisibilityChange,
                onClick = { issue -> navController.navigate(Screen.Issue(issue.id)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownFilterOption(label: String, options: List<T>, selectedOption: T = options[0], itemLabel: (T) -> String = { it.toString() }) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            value = itemLabel(selectedOption),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(itemLabel(option)) },
                    onClick = {
//                        selectedOption = option
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownQueriesFilterOption(queries: List<RedmineQueriesByProject>, selectedQuery: Query?, onSelectionChange: (Query) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember(selectedQuery) { mutableStateOf(selectedQuery) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            value = selectedOption?.name ?: "",
            onValueChange = {},
            label = { Text("Queries") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            queries.forEach { (project, queryList) ->
                HorizontalDivider()
                Text(
                    text = project,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                HorizontalDivider()
                queryList.forEach { query ->
                    DropdownMenuItem(
                        text = { Text(query.name) },
                        onClick = {
                            onSelectionChange(query)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}