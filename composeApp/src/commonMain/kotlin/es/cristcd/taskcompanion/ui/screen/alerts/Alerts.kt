package es.cristcd.taskcompanion.ui.screen.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.screen.issue.JournalAttributeChange
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.Res
import task_companion.composeapp.generated.resources.clear_all_24px
import task_companion.composeapp.generated.resources.close_small_24px

@Composable
fun Alerts(navController: NavController, viewmodel: AlertsViewmodel = viewModel { AlertsViewmodel() }) {

    val changedIssues = viewmodel.changedIssues.collectAsStateWithLifecycle(emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Alerts", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { viewmodel.clearAll(changedIssues.value) }) {
                    Icon(painterResource(Res.drawable.clear_all_24px), null)
                }
            }
            HorizontalDivider()
        }
        items(changedIssues.value, key = { it.id }) {
            Box {
                ListItem(
                    overlineContent = { Text(it.id.toString()) },
                    headlineContent = { Text(it.subject, style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                            if (it.summary.comments.isNotBlank()) {
                                Text(it.summary.comments, style = MaterialTheme.typography.bodySmall)
                            }
                            it.summary.attributeChanges.forEach { change ->
                                JournalAttributeChange(detail = change)
                            }
                        }
                    },
                    trailingContent = {
                    },
                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                        .clickable(onClick = { navController.navigate(Screen.Issue(it.id)) })
                        .animateItem(),
                )
                IconButton(onClick = { viewmodel.clearAlert(it) }, Modifier.align(Alignment.TopEnd)) {
                    Icon(painterResource(Res.drawable.close_small_24px), contentDescription = null)
                }
            }
        }
    }

}