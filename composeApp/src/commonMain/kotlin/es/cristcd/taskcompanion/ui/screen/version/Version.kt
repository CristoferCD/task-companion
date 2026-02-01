package es.cristcd.taskcompanion.ui.screen.version

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.redmine.model.Issue
import es.cristcd.taskcompanion.redmine.model.IssueList
import es.cristcd.taskcompanion.redmine.model.IssueListAnalytics
import es.cristcd.taskcompanion.redmine.model.SimpleCustomField
import es.cristcd.taskcompanion.tracker.SettingsCache
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.common.FullscreenLoading
import es.cristcd.taskcompanion.ui.common.PriorityIcon
import es.cristcd.taskcompanion.ui.common.StatusBadge
import es.cristcd.taskcompanion.util.toDefaultFormatString
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.Pie
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.Res
import task_companion.composeapp.generated.resources.arrow_back_24px
import task_companion.composeapp.generated.resources.more_vert_24px
import task_companion.composeapp.generated.resources.visibility_24px
import task_companion.composeapp.generated.resources.visibility_off_24px
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun VersionScreen(id: Long, navController: NavHostController, viewmodel: VersionViewmodel = viewModel { VersionViewmodel() }) {
    LaunchedEffect(id) {
        viewmodel.loadVersion(id)
    }

    val state = viewmodel.version.collectAsState()
    when (val result = state.value) {
        is VersionResult.Loading -> FullscreenLoading()
        is VersionResult.Ok -> Version(result, navController, viewmodel::toggleFollowVersion)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Version(version: VersionResult.Ok, navController: NavHostController, onFollowVersion: () -> Unit) {
    var showChartsDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scrollState = scrollBehavior.state
    val appBarExpanded by remember {
        // app bar expanded state
        // collapsedFraction 1f = collapsed
        // collapsedFraction 0f = expanded
        derivedStateOf { scrollState.collapsedFraction < 0.9f }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(version.version.name, style = MaterialTheme.typography.titleMedium)
                            if (appBarExpanded) {
                                Column {
                                    Text("- Tareas: ${version.analytics.totalIssues}", style = MaterialTheme.typography.labelMedium)
                                    Text("- Story points: ${version.analytics.totalStoryPoints}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (appBarExpanded) {
                            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.BottomEnd) {
                                PieIssuesByStatus(version.analytics)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                },
                actions = {
                    FollowButton(version.following, onFollowVersion)
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Desglose tareas") },
                                onClick = { showChartsDialog = true}
                            )
                        }
                    }
                },
                expandedHeight = 180.dp,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        val padding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 8.dp,
            start = 8.dp,
            end = 8.dp
        )
        Column(Modifier.padding(padding)) {
            if (showChartsDialog) {
                BasicAlertDialog(onDismissRequest = { showChartsDialog = false }, modifier = Modifier.width( 1000.dp)) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                        Column {
                            var selectedTabIndex by remember { mutableStateOf(0) }
                            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                                Tab(
                                    selected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0 },
                                    text = { Text("Por asignación") },
                                )
                                Tab(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    text = { Text("Por categoría") },
                                )
                            }
                            Box(modifier = Modifier.padding(24.dp)) {
                                if (selectedTabIndex == 0) {
                                    RowChartByAssigned(version.analytics)
                                } else if (selectedTabIndex == 1) {
                                    RowChartByCategory(version.analytics)
                                }
                            }
                        }
                    }
                }
            }

            IssueTable(version.issueList, onClick = { issue -> navController.navigate(Screen.Issue(issue.id)) })
        }
    }
}

@Composable
fun FollowButton(following: Boolean, onClick: () -> Unit) {
    val icon = if (following) Res.drawable.visibility_24px else Res.drawable.visibility_off_24px
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Composable
fun PieIssuesByStatus(analytics: IssueListAnalytics, showLabels: Boolean = true) {
    val issuePie by remember {
        mutableStateOf(analytics.byStatus.map { (status, count) ->
            val color = status?.id?.let { SettingsCache.redmineStatusColors[it] } ?: Color.DarkGray
            Pie(label = status?.name, data = count.toDouble() / (analytics.totalIssues.toDouble()), color = color)
        })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PieChart(
            modifier = Modifier.size(100.dp),
            data = issuePie,
            spaceDegree = 2f,
            style = Pie.Style.Stroke(width = 20.dp),
        )
        if (showLabels) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                issuePie.forEach {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(it.color))
                        Text(it.label ?: "", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun RowChartByAssigned(analytics: IssueListAnalytics) {
    val assignedChart by remember {
        mutableStateOf(analytics.byAssigned.sortedBy { it.id?.id }.map { (assigned, count, sp) ->
            Bars(
                assigned?.name ?: "No asignado",
                listOf(
                    Bars.Data(label = "Count",  value = count.toDouble(), color = SolidColor(Color(0xff89ce94))),
                    Bars.Data(label = "SP",  value = sp.toDouble(), color = SolidColor(Color(0xff7d5ba6))),
                )
            )
        })
    }
    if (assignedChart.isNotEmpty()) {
        RowChart(
            modifier = Modifier.size(500.dp).padding(horizontal = 22.dp),
            data = assignedChart,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 2.dp, topLeft = 2.dp),
                spacing = 3.dp,
                thickness = 8.dp
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            animationMode = AnimationMode.Together()
        )
    }
}

@Composable
fun RowChartByCategory(analytics: IssueListAnalytics) {
    val categoryChart by remember {
        mutableStateOf(analytics.byCategory.sortedBy { it.id?.id }.map { (assigned, count, sp) ->
            Bars(
                assigned?.name ?: "Sin categoria",
                listOf(
                    Bars.Data(label = "Count",  value = count.toDouble(), color = SolidColor(Color(0xff89ce94))),
                    Bars.Data(label = "SP",  value = sp.toDouble(), color = SolidColor(Color(0xff7d5ba6))),
                )
            )
        })
    }
    if (categoryChart.isNotEmpty()) {
        RowChart(
            modifier = Modifier.size(500.dp).padding(horizontal = 22.dp),
            data = categoryChart,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 2.dp, topLeft = 2.dp),
                spacing = 3.dp,
                thickness = 8.dp
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            animationMode = AnimationMode.Together()
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun IssueTable(issues: IssueList, onClick: (Issue) -> Unit) {
    val columnWidths = remember { mutableStateMapOf<Int, Int>() }
    LazyColumn(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        item {
            Column {
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(modifier = Modifier.widthPx(columnWidths[0] ?: 0).padding(start = 2.dp), text = "Estado", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(modifier = Modifier.widthPx(columnWidths[1] ?: 0), text = "", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(modifier = Modifier.weight(1f), text = "Asunto", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(modifier = Modifier.widthPx(columnWidths[2] ?: 0), text = "Asignado a", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(modifier = Modifier.widthPx(columnWidths[3] ?: 0), text = "Responsable", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(modifier = Modifier.widthPx(columnWidths[4] ?: 0), text = "Actualizado", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)

                }
                HorizontalDivider()
            }
        }
        items(issues.issues) { issue ->
            Column {
                Row(
                    Modifier.clickable(onClick = {onClick(issue)}).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.widthIn(0.dp, 80.dp).maxWidthForColumn(columnWidths, 0)) {
                        StatusBadge(issue.status)
                    }
                    Box(Modifier.maxWidthForColumn(columnWidths, 1)) {
                        PriorityIcon(issue.priority.name)
                    }
                    Text(modifier = Modifier.weight(1f), text = issue.subject, style = MaterialTheme.typography.bodyMedium)

                    Box(Modifier.widthIn(0.dp, 100.dp).maxWidthForColumn(columnWidths, 2)) {
                        Text(text = issue.assignedTo?.name ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.widthIn(0.dp, 100.dp).maxWidthForColumn(columnWidths, 3)) {
                        Text((issue.customFields.firstOrNull { it.name == "Responsable" } as SimpleCustomField?)?.value ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.maxWidthForColumn(columnWidths, 4)) {
                        Text(
                            issue.updatedOn?.toDefaultFormatString() ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

fun Modifier.maxWidthForColumn(columnWidths: SnapshotStateMap<Int, Int>, idx: Int) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)


    val existingWidth = columnWidths[idx] ?: 0
    val maxWidth = maxOf(existingWidth, placeable.width)

    columnWidths[idx] = maxWidth

    layout(width = maxWidth, height = placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

fun Modifier.widthPx(px: Int) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(width = px, height = placeable.height) {
        placeable.placeRelative(0, 0)
    }
}


