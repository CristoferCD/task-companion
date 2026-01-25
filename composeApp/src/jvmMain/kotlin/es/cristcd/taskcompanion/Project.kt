@file:OptIn(ExperimentalMaterial3Api::class)

package es.cristcd.taskcompanion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
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
        is ProjectResult.Loading -> FullscreenLoading()
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
                    IconButton(onClick = { navController.popBackStack() }) {
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
        LazyColumn(Modifier.padding(padding).background(MaterialTheme.colorScheme.surfaceContainer)) {
            items(project.versions) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = {navController.navigate(Screen.Version(it.id))}).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
}