package es.cristcd.taskcompanion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun Dashboard(navController: NavHostController, viewmodel: DashboardViewmodel = viewModel { DashboardViewmodel() }) {
    LaunchedEffect(true) {
        viewmodel.load()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        val issues = viewmodel.issuesAssigned.collectAsState()
        val issuesMonitorizados = viewmodel.issuesMonitored.collectAsState()
        val versions = viewmodel.versions.collectAsState()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            var text by remember { mutableStateOf("") }
            SearchBar(
                modifier = Modifier.width(350.dp),
                inputField = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Search", color = MaterialTheme.colorScheme.onSurface) },
                        trailingIcon = {
                            IconButton(onClick = { navController.navigate(Screen.Issue(text.toLong())) }) {
                                Icon(painterResource(Res.drawable.visibility_24px), "")
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,

                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 8.dp, end = 8.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
                    )
                },
                expanded = false,
                onExpandedChange = {}
            ) {}
        }
        Row(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxHeight(),
        ) {
            val alphaAnimation = remember {
                Animatable(0.5f)
            }
            LaunchedEffect(issues.value, issuesMonitorizados.value) {
                alphaAnimation.snapTo(0.5f)
                alphaAnimation.animateTo(0f, animationSpec = tween(3000))
            }
            var sidebarNavigation by remember { mutableStateOf<SidebarNavigation>(SidebarNavigation.None) }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(280.dp),
                contentPadding = PaddingValues(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)).background(MaterialTheme.colorScheme.surface).weight(1f)
            ) {
                dashboardSection("Asignados a mi", viewmodel::reloadAssigned, issues.value, "No issues assigned") {
                    TaskCard(it, alphaAnimation, onClick = { navController.navigate(Screen.Issue(it.id)) }, onStart = { viewmodel.startTask(it); sidebarNavigation = SidebarNavigation.Tracker })
                }

                dashboardSection("Monitorizados", viewmodel::reloadMonitored, issuesMonitorizados.value, "No monitored issues") {
                    TaskCard(it, alphaAnimation, onClick = { navController.navigate(Screen.Issue(it.id)) }, onStart = { viewmodel.startTask(it); sidebarNavigation = SidebarNavigation.Tracker })
                }

                dashboardSection("Versiones seguidas", viewmodel::reloadVersions, versions.value, "No versions followed") {
                    VersionCard(it.version, it.analytics, onClick = { navController.navigate(Screen.Version(it.version.id)) })
                }
            }

            AnimatedContent(
                targetState = sidebarNavigation,
            ) { optionSelected ->
                when (optionSelected) {
                    SidebarNavigation.None -> Unit
                    SidebarNavigation.Tracker -> {
                        Box(modifier = Modifier.width(650.dp).fillMaxHeight()) {
                            Tracker(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
                        }
                    }
                    SidebarNavigation.Projects -> {
                        Box(modifier = Modifier.width(250.dp).fillMaxHeight()) {
                            ProjectList(navController = navController)
                        }
                    }
                }
            }


            Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val selectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                IconButton(onClick = { sidebarNavigation = if (sidebarNavigation == SidebarNavigation.Tracker) SidebarNavigation.None else SidebarNavigation.Tracker }, colors = if (sidebarNavigation == SidebarNavigation.Tracker) IconButtonDefaults.iconButtonColors(containerColor = selectedColor) else IconButtonDefaults.iconButtonColors()) {
                    Icon(painterResource(Res.drawable.assignment_24px), contentDescription = null)
                }
                IconButton(onClick = { sidebarNavigation = if (sidebarNavigation == SidebarNavigation.Projects) SidebarNavigation.None else SidebarNavigation.Projects }, colors = if (sidebarNavigation == SidebarNavigation.Projects) IconButtonDefaults.iconButtonColors(containerColor = selectedColor) else IconButtonDefaults.iconButtonColors()) {
                    Icon(painterResource(Res.drawable.team_dashboard_24px), contentDescription = null)
                }
                Spacer(Modifier.weight(1f))
                Column {
                    HorizontalDivider(Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { navController.navigate(Screen.Settings) }) {
                        Icon(painterResource(Res.drawable.settings_24px), contentDescription = "Settings")
                    }
                }

            }
        }
    }
}

private fun <T> LazyStaggeredGridScope.dashboardSection(title: String, onReload: () -> Unit, items: List<T>, emptyMessage: String, content: @Composable LazyStaggeredGridItemScope.(T) -> Unit) {
    item(span = StaggeredGridItemSpan.FullLine) {
        Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onReload, modifier = Modifier.size(20.dp)) {
                Icon(painterResource(Res.drawable.refresh_24px), null, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (items.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    items(items, itemContent = content)
}