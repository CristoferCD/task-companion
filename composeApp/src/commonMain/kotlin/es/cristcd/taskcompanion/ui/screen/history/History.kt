package es.cristcd.taskcompanion.ui.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.util.toRelativeDayString

@Composable
fun History(navController: NavHostController, viewmodel: HistoryViewmodel = viewModel { HistoryViewmodel() }) {
    LaunchedEffect(Unit) {
        viewmodel.load()
    }

    val issues = viewmodel.recentIssues.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("History", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
        }
        val issuesByDate = issues.value.groupBy { it.lastVisited.toRelativeDayString() }
        issuesByDate.forEach { (date, issueList) ->
            item {
                Text(
                    date,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(issueList) {
                val containerColor =
                    if (it.statusColor != null) Color(it.statusColor).copy(alpha = 0.15f) else ListItemDefaults.containerColor
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = containerColor),
                    overlineContent = { Text(it.redmineId.toString()) },
                    headlineContent = { Text(it.subject, style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = { navController.navigate(Screen.Issue(it.redmineId)) }),
                )
            }
        }
    }

}