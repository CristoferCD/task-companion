package es.cristcd.taskcompanion.ui.screen.projectlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.ui.Screen

@Composable
fun ProjectList(navController: NavHostController, viewmodel: ProjectListViewmodel = viewModel { ProjectListViewmodel() }) {
    LaunchedEffect(Unit) {
        viewmodel.load()
    }

    val projects = viewmodel.projects.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Redmine projects", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
        }
        items(projects.value) { project ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = {navController.navigate(Screen.Project(project.id))}
            ) {
                Box(Modifier.padding(12.dp)) {
                    Text(text = project.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}