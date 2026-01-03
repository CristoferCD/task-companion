package es.cristcd.taskcompanion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.IssueListAnalytics
import es.cristcd.taskcompanion.redmine.model.Version

@Composable
fun VersionCard(version: Version, analytics: IssueListAnalytics, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(version.name, style = MaterialTheme.typography.titleMedium)
                Column {
                    Text("- Tareas: ${analytics.totalIssues}", style = MaterialTheme.typography.labelMedium)
                    Text("- Story points: ${analytics.totalStoryPoints}", style = MaterialTheme.typography.labelMedium)
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.BottomEnd) {
                PieIssuesByStatus(analytics, showLabels = false)
            }
        }
    }
}