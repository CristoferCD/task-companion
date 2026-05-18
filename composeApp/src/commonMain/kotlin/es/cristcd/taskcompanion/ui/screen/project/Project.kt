@file:OptIn(ExperimentalMaterial3Api::class)

package es.cristcd.taskcompanion.ui.screen.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.redmine.model.Version
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
import es.cristcd.taskcompanion.ui.screen.version.IssueTable
import es.cristcd.taskcompanion.util.popBackStackIfResumed
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.Res
import task_companion.composeapp.generated.resources.arrow_back_24px
import task_companion.composeapp.generated.resources.lock_24px
import task_companion.composeapp.generated.resources.lock_open_right_24px

@Composable
fun ProjectScreen(projectId: Long, navController: NavHostController, viewmodel: ProjectViewmodel = viewModel { ProjectViewmodel() }) {
    LaunchedEffect(projectId) {
        viewmodel.load(projectId)
    }

    val state = viewmodel.version.collectAsState()
    when (val result = state.value) {
        is ProjectResult.Loading -> FullscreenLoading(onCancel = { navController.popBackStackIfResumed() })
        is ProjectResult.Ok -> Project(result, navController)
    }
}

@Composable
fun Project(project: ProjectResult.Ok, navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.project.name) },
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
        Column(modifier = Modifier.padding(padding).background(MaterialTheme.colorScheme.surfaceContainer)) {
            var selectedTabIndex by remember { mutableStateOf(0) }
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Tickets recientes") })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Versiones") })
            }
            if (selectedTabIndex == 0) {
                IssueTable(project.recentIssues, onClick = { issue -> navController.navigate(Screen.Issue(issue.id)) })
            } else if (selectedTabIndex == 1) {
                VersionList(project.versions, onClick = { navController.navigate(Screen.Version(it.id)) })
            }
        }
    }
}

@Composable
private fun VersionList(versions: List<Version>, onClick: (Version) -> Unit) {
    if (versions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
            Text("No hay versiones", style = MaterialTheme.typography.bodyMedium)
        }
    }
    LazyColumn {
        items(versions) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable(onClick = { onClick(it) }).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (it.status == "open") {
                        Icon(
                            painterResource(Res.drawable.lock_open_right_24px), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xff90be6d)
                        )
                    } else {
                        Icon(painterResource(Res.drawable.lock_24px), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xffef476f))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = it.name, style = MaterialTheme.typography.bodyMedium)
                            if (it.dueDate != null) {
                                Text(text = it.dueDate.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (!it.description.isNullOrBlank()) {
                            Text(text = it.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}