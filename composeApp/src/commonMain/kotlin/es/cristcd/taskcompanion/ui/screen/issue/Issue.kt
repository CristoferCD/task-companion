package es.cristcd.taskcompanion.ui.screen.issue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.BadgedBox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.core.CachedResult
import es.cristcd.taskcompanion.redmine.model.*
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
import es.cristcd.taskcompanion.ui.common.PriorityIcon
import es.cristcd.taskcompanion.ui.common.StatusBadge
import es.cristcd.taskcompanion.util.popBackStackIfResumed
import kotlinx.coroutines.launch
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
    val scrollState = rememberScrollState()
    when (val issue = state.value) {
        is CachedResult.Loading -> FullscreenLoading(onCancel = { navController.popBackStackIfResumed() })
        is CachedResult.FromDb -> Issue(
            issue.issue,
            project.value,
            versions.value,
            watching.value,
            viewmodel::onAction,
            navController,
            scrollState,
            issue.updatedAt,
        )
        is CachedResult.FromApi -> Issue(
            issue.issue,
            project.value,
            versions.value,
            watching.value,
            viewmodel::onAction,
            navController,
            scrollState
        )
    }

}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun Issue(
    issue: ExtendedIssue,
    project: Project?,
    versions: List<Version>,
    watching: Boolean,
    onAction: (IssueAction) -> Unit,
    navController: NavHostController,
    scrollState: ScrollState,
    valueCachedAt: Instant? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    SelectionContainer {
                        Text("#${issue.id} - ${issue.subject}", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackIfResumed() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                },
                actions = {
                    WatchButton(watching, onClick = {onAction(IssueAction.ToggleWatching)})
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
                                onClick = { expanded = false; onAction(IssueAction.StartTask) }
                            )
                            DropdownMenuItem(
                                text = { Text("Abrir en navegador") },
                                onClick = {
                                    expanded = false
                                    onAction(IssueAction.OpenInBrowser)
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = scrollState.canScrollForward,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                BadgedBox(
                    badge = {
                        if (issue.journals.isNotEmpty()) {
                            Badge {
                                Text(issue.journals.size.toString(), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                ) {
                    FloatingActionButton(onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }) {
                        Icon(painterResource(Res.drawable.arrow_downward_alt_24px), contentDescription = null)
                    }
                }
            }
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityIcon(issue.priority.name)
                EditableStatusBadge(issue, onSelection = { onAction(IssueAction.UpdateAttribute(IssueForm(statusId = it.id))) })
                Text(issue.createdOn?.toLocalDateTime(TimeZone.currentSystemDefault()).toString(), style = MaterialTheme.typography.labelMedium)
                Text("Creado por: ${issue.author.name}", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionContainer(modifier = Modifier.weight(1f, fill = true).padding(start = 8.dp)) {
                    Text(issue.description.trim())
                }
                IssueSidebar(
                    issue,
                    project,
                    versions,
                    updateAttribute = { onAction(IssueAction.UpdateAttribute(it)) },
                    downloadFile = { onAction(IssueAction.DownloadFile(it)) }
                )
            }

            HorizontalDivider()
            Text("Comentarios: ", style = MaterialTheme.typography.titleSmall)
            issue.journals.forEach { Journal(it) }
        }
    }
}

@Composable
fun IssueSidebar(
    issue: ExtendedIssue,
    project: Project?,
    versions: List<Version>,
    updateAttribute: (IssueForm) -> Unit,
    downloadFile: (Attachment) -> Unit
) {
    Column(Modifier.widthIn(50.dp, 250.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SidebarItem("Properties") {
            FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp)) {
                IssueAttributeTag("Tipo", issue.tracker.name) { dismiss ->
                    project?.trackers?.sortedBy { it.id }?.forEach { tracker ->
                        DropdownMenuItem(text = { Text(tracker.name ?: "") }, onClick = { dismiss(); updateAttribute(IssueForm(trackerId = tracker.id)) })
                    }
                }
                if (versions.isNotEmpty()) {
                    IssueAttributeTag("Version prevista", issue.fixedVersion?.name) { dismiss ->
                        versions.sortedByDescending { it.createdOn }.forEach { version ->
                            DropdownMenuItem( text = { Text(version.name) },  onClick = { dismiss(); updateAttribute(IssueForm(fixedVersionId = version.id)) })
                        }
                    }
                }
                IssueAttributeTag("Asignado a", issue.assignedTo?.name)
                IssueAttributeTag("% Realizado", issue.doneRatio.toString())
                IssueAttributeTag("Categoría", issue.category?.name) { dismiss ->
                    project?.issueCategories?.sortedBy { it.name }?.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name ?: "") }, onClick = { dismiss(); updateAttribute(IssueForm(categoryId = category.id)) })
                    }
                }
                IssueAttributeTag("Fecha inicio", issue.startDate.toString())
                IssueAttributeTag("Fecha fin", issue.dueDate.toString())
                IssueAttributeTag("Tiempo estimado", issue.estimatedHours.toString())
            }
        }
        SidebarItem("Custom fields") {
            Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                issue.customFields.sortedBy { it.id }.forEach {
                    val value = when(it) {
                        is SimpleCustomField -> it.value
                        is MultipleCustomField -> it.value?.joinToString(", ")
                    }
                    IssueAttributeTag(it.name, value)
                }
            }
        }
        SidebarItem("Relations") {
            Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                issue.relations.sortedBy { it.id }.forEach {
                    val otherId = if (it.issueId != issue.id) it.issueId else it.issueToId
                    val iconAndText = when(it.relationType) {
                        RelationType.RELATES -> Res.drawable.link_2_24px to "Relates to $otherId"
                        RelationType.DUPLICATES -> Res.drawable.arrows_outward_24px to "Duplicate of $otherId"
                        RelationType.DUPLICATED -> Res.drawable.arrows_outward_24px to "Duplicated by $otherId"
                        RelationType.BLOCKS -> Res.drawable.shield_lock_24px to "Blocks $otherId"
                        RelationType.BLOCKED -> Res.drawable.shield_lock_24px to "Blocked by $otherId"
                        RelationType.PRECEDES -> Res.drawable.swipe_right_alt_24px to "Precedes: $otherId"
                        RelationType.FOLLOWS -> Res.drawable.swipe_right_alt_24px to "Follows $otherId"
                        RelationType.COPIED_TO -> Res.drawable.content_copy_24px to "Copied to $otherId"
                        RelationType.COPIED_FROM -> Res.drawable.content_copy_24px to "Copied from $otherId"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(iconAndText.first), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Text(iconAndText.second, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        SidebarItem("Watchers") {
            Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                issue.watchers.sortedBy { it.id }.forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Res.drawable.person_24px), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Text("${it.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (issue.attachments.isNotEmpty()) {
            SidebarItem("Attachments") {
                Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    issue.attachments.sortedBy { it.createdOn }.forEach {
                        AssistChip(
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(Res.drawable.file_present_24px), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Text(it.filename, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            onClick = { downloadFile(it)}
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SidebarItem(title: String, content: @Composable () -> Unit) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        val degrees by animateFloatAsState(if (expanded) -90f else 90f)
        Column(Modifier.clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainer)) {
            Row(modifier = Modifier.clickable { expanded = expanded.not() }.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Image(
                    painterResource(Res.drawable.chevron_right_24px),
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees).size(16.dp),
                    colorFilter = ColorFilter.tint(Color.Gray)
                )
            }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer)) {
        Column {
            Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(journal.user.name ?: "", style = MaterialTheme.typography.labelSmall)
                Text(journal.createdOn?.toLocalDateTime(TimeZone.currentSystemDefault()).toString(), style = MaterialTheme.typography.bodySmall)
            }
            if (!journal.notes.isNullOrBlank()) {
                SelectionContainer {
                    Text(journal.notes.trim(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp))
                }
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
        Box(Modifier.weight(0.5f, fill = false).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.secondaryFixedDim).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(detail.oldValue.abbreviate(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryFixedVariant)
        }
        Icon(painterResource(Res.drawable.arrow_right_alt_24px), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(0.5f, fill = false).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.secondaryFixed).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(detail.newValue.abbreviate(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryFixed)
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