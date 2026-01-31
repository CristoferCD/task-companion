package es.cristcd.taskcompanion.ui.screen.issue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.core.CachedResult
import es.cristcd.taskcompanion.redmine.model.*
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
import es.cristcd.taskcompanion.ui.common.PriorityIcon
import es.cristcd.taskcompanion.ui.common.StatusBadge
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun IssueScreen(issueId: Long, navController: NavHostController, viewmodel: IssueViewmodel = viewModel { IssueViewmodel() }) {
    LaunchedEffect(issueId) {
        viewmodel.loadIssue(issueId)
    }
    val watching = viewmodel.watching.collectAsState()
    val state = viewmodel.issue.collectAsState()
    val project = viewmodel.project.collectAsState()
    val versions = viewmodel.versions.collectAsState()
    when (val issue = state.value) {
        is CachedResult.Loading -> FullscreenLoading()
        is CachedResult.FromDb -> Issue(issue.issue, project.value, versions.value, watching.value, viewmodel::toggleWatching, viewmodel::updateIssueAttribute, viewmodel::startTask, viewmodel::openInBrowser, navController, issue.updatedAt)
        is CachedResult.FromApi -> Issue(issue.issue, project.value, versions.value, watching.value, viewmodel::toggleWatching, viewmodel::updateIssueAttribute, viewmodel::startTask, viewmodel::openInBrowser, navController)
    }

}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun Issue(
    issue: ExtendedIssue,
    project: Project?,
    versions: List<Version>,
    watching: Boolean,
    toggleWatching: () -> Unit,
    updateAttribute: (form: IssueForm) -> Unit,
    startTask: () -> Unit,
    openInBrowser: () -> Unit,
    navController: NavHostController,
    valueCachedAt: Instant? = null
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text("#${issue.id} - ${issue.subject}", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                },
                actions = {
                    WatchButton(watching, onClick = toggleWatching)
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Start")},
                                onClick = { expanded = false; startTask() }
                            )
                            DropdownMenuItem(
                                text = { Text("Abrir en navegador") },
                                onClick = {
                                    expanded = false
                                    openInBrowser()
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (valueCachedAt != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Version en cache de: $valueCachedAt",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 12.dp, start = 12.dp, end = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityIcon(issue.priority.name)
                EditableStatusBadge(issue, onSelection = { updateAttribute(IssueForm(statusId = it.id)) })
                Text(issue.createdOn?.toLocalDateTime(TimeZone.currentSystemDefault()).toString(), style = MaterialTheme.typography.labelMedium)
                Text("Creado por: ${issue.author.name}", style = MaterialTheme.typography.labelMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp, progress = { issue.doneRatio / 100f }, color = Color.Green)
                    Text((issue.customFields.firstOrNull { it.name == "Puntos de historia" } as SimpleCustomField?)?.value ?: "", style = MaterialTheme.typography.labelMedium)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    IssueAttributeTag("Tipo", issue.tracker.name) { dismiss ->
                        project?.trackers?.sortedBy { it.id }?.forEach { tracker ->
                            DropdownMenuItem(text = { Text(tracker.name ?: "") }, onClick = { dismiss(); updateAttribute(IssueForm(trackerId = tracker.id)) })
                        }
                    }
                    IssueAttributeTag("Version prevista", issue.fixedVersion?.name) { dismiss ->
                        versions.sortedByDescending { it.createdOn }.forEach { version ->
                            DropdownMenuItem(text = { Text(version.name) }, onClick = { dismiss(); updateAttribute(IssueForm(fixedVersionId = version.id)) })
                        }
                    }
                    IssueAttributeTag("Asignado a", issue.assignedTo?.name)
                    IssueAttributeTag("Responsable", (issue.customFields.firstOrNull { it.name == "Responsable" } as SimpleCustomField?)?.value)
                    IssueAttributeTag("Puntos de historia", (issue.customFields.firstOrNull { it.name == "Puntos de historia" } as SimpleCustomField?)?.value)
                    IssueAttributeTag("Tiempo estimado", issue.estimatedHours.toString())
                    IssueAttributeTag("Fecha fin", issue.dueDate.toString())
                    IssueAttributeTag("% Realizado", issue.doneRatio.toString())
                    IssueAttributeTag("Categoría", issue.category?.name) { dismiss ->
                        project?.issueCategories?.sortedBy { it.name }?.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name ?: "") }, onClick = { dismiss(); updateAttribute(IssueForm(categoryId = category.id)) })
                        }
                    }
                }
            }

            Text(issue.description.trim())
            HorizontalDivider()
            Text("Comentarios: ", style = MaterialTheme.typography.titleSmall)
            issue.journals.forEach { Journal(it) }
        }
    }
}

@Composable
fun EditableStatusBadge(issue: ExtendedIssue, onSelection: (IssueStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.clickable(onClick = { expanded = true })) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            issue.allowedStatuses.forEach { status ->
                DropdownMenuItem(text = { Text(status.name) }, onClick = { expanded = false; onSelection(status) })
            }
        }
        StatusBadge(issue.status)
    }
}

@Composable
fun IssueAttributeTag(label: String, value: String?, dropdownContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            label = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$label:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(value ?: "", style = MaterialTheme.typography.labelMedium)
                }
            },
            modifier = Modifier.padding(0.dp),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            content = { dropdownContent { expanded = false } },
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun Journal(journal: Journal) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        Column {
            Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(journal.user.name ?: "", style = MaterialTheme.typography.labelSmall)
                Text(journal.createdOn?.toLocalDateTime(TimeZone.currentSystemDefault()).toString(), style = MaterialTheme.typography.bodySmall)
            }
            if (!journal.notes.isNullOrBlank()) {
                Text(journal.notes.trim(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp))
            }
            if (journal.details.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                journal.details.forEach {
                    JournalAttributeChange(Modifier.padding(horizontal = 12.dp), it)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun JournalAttributeChange(modifier: Modifier = Modifier, detail: JournalDetail) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${detail.property} (${detail.name})", style = MaterialTheme.typography.labelSmall)
        Badge(containerColor = MaterialTheme.colorScheme.inversePrimary, modifier = Modifier.weight(0.5f, fill = false)) {
            Text(detail.oldValue.abbreviate(80), style = MaterialTheme.typography.bodySmall)
        }
        Icon(painterResource(Res.drawable.arrow_right_alt_24px), contentDescription = null)
        Badge(containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(0.5f, fill = false)) {
            Text(detail.newValue.abbreviate(80), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WatchButton(watching: Boolean, onClick: () -> Unit) {
    val icon = if (watching) Res.drawable.visibility_24px else Res.drawable.visibility_off_24px
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

fun String?.abbreviate(maxLength: Int): String {
    if (this.isNullOrBlank()) return ""

    return if (length > maxLength) substring(0, maxLength - 4) + "..." else this
}