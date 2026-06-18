package es.cristcd.taskcompanion.ui.screen.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.events.ShortcutEvent
import es.cristcd.taskcompanion.events.ShortcutEventBus
import es.cristcd.taskcompanion.persistence.model.DashboardItem
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.common.TaskCard
import es.cristcd.taskcompanion.ui.common.VersionCard
import es.cristcd.taskcompanion.ui.screen.projectlist.ProjectList
import es.cristcd.taskcompanion.ui.screen.tracker.Tracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalMaterialApi::class)
@Composable
fun Dashboard(navController: NavHostController, viewmodel: DashboardViewmodel = viewModel { DashboardViewmodel() }) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewmodel.load()
                delay(5.minutes)
            }
        }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            ShortcutEventBus.events.collectLatest { event ->
                if (event is ShortcutEvent.Refresh) {
                    viewmodel.reloadAll()
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        var showSearchDialog by remember { mutableStateOf(false) }

        if (showSearchDialog) {
            val searching = viewmodel.searching.collectAsState()
            val searchResults = viewmodel.searchResults.collectAsState()
            SearchDialog(
                onSelect = {
                    navController.navigate(Screen.Issue(it))
                    showSearchDialog = false
                },
                onDismiss = { showSearchDialog = false },
                searching = searching.value,
                searchResults = searchResults.value,
                onSearch = { viewmodel.searchQuery(it) }
            )
        }


        val items = viewmodel.layoutItems.collectAsStateWithLifecycle()
        Row(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxHeight(),
        ) {
            val alphaAnimation = remember {
                Animatable(0.8f)
            }
            LaunchedEffect(items.value) {
                alphaAnimation.snapTo(0.8f)
                alphaAnimation.animateTo(0.3f, animationSpec = tween(3000))
            }
            var sidebarNavigation by remember { mutableStateOf<SidebarNavigation>(SidebarNavigation.None) }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(280.dp),
                contentPadding = PaddingValues(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)).background(MaterialTheme.colorScheme.surface).weight(1f)
            ) {
                items.value.forEach { group ->
                    when(val content = group.content) {
                        is DashboardGroupContent.IssueList -> {
                            dashboardSection(group.title, { viewmodel.reloadGroup(group.id) }, {viewmodel.updateGroupName(group.id, it)}, {viewmodel.deleteGroup(group.id)}, content.list.issues, content.list.total, "No ${group.title.lowercase()}", group.reloading) {
                                TaskCard(
                                    issue = it,
                                    newItemAlphaAnimation = alphaAnimation,
                                    onClick = { navController.navigate(Screen.Issue(it.id)) },
                                    onStart = { viewmodel.startTask(it); sidebarNavigation = SidebarNavigation.Tracker },
                                    onUpdateTags = { tags -> viewmodel.updateIssueTags(it, tags); viewmodel.reloadGroup(group.id) },
                                )
                            }
                        }
                        is DashboardGroupContent.VersionList -> {
                            dashboardSection(group.title, { viewmodel.reloadGroup(group.id) }, {viewmodel.updateGroupName(group.id, it)}, {viewmodel.deleteGroup(group.id)}, content.list, null, "No ${group.title.lowercase()}", group.reloading) {
                                VersionCard(it.version, it.analytics, onClick = { navController.navigate(Screen.Version(it.version.id)) })
                            }
                        }
                        is DashboardGroupContent.Loading -> {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(group.title, style = MaterialTheme.typography.titleMedium)
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(
                                    MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(42.dp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                        is DashboardGroupContent.Error -> {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionTitle(group.title, { viewmodel.reloadGroup(group.id) }, {viewmodel.updateGroupName(group.id, it)}, {viewmodel.deleteGroup(group.id)}, null)
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(
                                    MaterialTheme.colorScheme.errorContainer).padding(28.dp), contentAlignment = Alignment.Center) {
                                    Text(content.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    var showNewGroupForm by remember { mutableStateOf(false) }
                    if (!showNewGroupForm) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(40.dp).clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showNewGroupForm = true }) {
                                Icon(painterResource(Res.drawable.add_24px), "Add")
                            }
                        }
                    } else {
                        val queries = viewmodel.availableQueries.collectAsState()
                        LaunchedEffect(showNewGroupForm) {
                            if (showNewGroupForm) {
                                viewmodel.loadRedmineQueries()
                            }
                        }
                        GroupForm(
                            queries.value,
                            onAdd = { name, item -> showNewGroupForm = false; viewmodel.createGroup(name, item) },
                            onCancel = { showNewGroupForm = false }
                        )
                    }
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


            Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val selectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                IconButton(onClick = { showSearchDialog = true }) {
                    Icon(painterResource(Res.drawable.search_24px), contentDescription = null)
                }
                IconButton(onClick = { sidebarNavigation = sidebarNavigation.toggle(SidebarNavigation.Tracker) }, colors = if (sidebarNavigation == SidebarNavigation.Tracker) IconButtonDefaults.iconButtonColors(containerColor = selectedColor) else IconButtonDefaults.iconButtonColors()) {
                    Icon(painterResource(Res.drawable.assignment_24px), contentDescription = null)
                }
                IconButton(onClick = { sidebarNavigation = sidebarNavigation.toggle(SidebarNavigation.Projects) }, colors = if (sidebarNavigation == SidebarNavigation.Projects) IconButtonDefaults.iconButtonColors(containerColor = selectedColor) else IconButtonDefaults.iconButtonColors()) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupForm(queries: List<RedmineQueriesByProject>, onAdd: (name: String, item: DashboardItem) -> Unit, onCancel: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var groupName by remember { mutableStateOf("") }
        var itemBoxExpanded by remember { mutableStateOf(false) }
        var selectedGroupItemName by remember { mutableStateOf("") }
        var groupItem by remember { mutableStateOf<DashboardItem?>(null)}

        val onGroupSelection = { name: String, item: DashboardItem ->
            selectedGroupItemName = name
            if (groupName.isBlank()) {
                groupName = selectedGroupItemName
            }
            groupItem = item
            itemBoxExpanded = false
        }

        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group Name") })
        ExposedDropdownMenuBox(expanded = itemBoxExpanded, onExpandedChange = { itemBoxExpanded = !itemBoxExpanded }, modifier = Modifier.widthIn(max = 500.dp)) {
            OutlinedTextField(
                label = { Text("Item") },
                value = selectedGroupItemName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemBoxExpanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = itemBoxExpanded,
                onDismissRequest = { itemBoxExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Asignados a mi") },
                    onClick = { onGroupSelection("Asignados a mi", DashboardItem.AssignedToMe) }
                )
                DropdownMenuItem(
                    text = { Text("Monitorizados") },
                    onClick = { onGroupSelection("Monitorizados", DashboardItem.Monitored) }
                )
                DropdownMenuItem(
                    text = { Text("Versiones seguidas") },
                    onClick = { onGroupSelection("Versiones seguidas", DashboardItem.FollowedVersions) }
                )
                queries.forEach { (project, queryList) ->
                    HorizontalDivider()
                    Text(
                        text = project,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    queryList.forEach { query ->
                        DropdownMenuItem(
                            text = { Text(query.name) },
                            onClick = { onGroupSelection(query.name, DashboardItem.CustomQuery(query.id, query.projectId)) }
                        )
                    }
                }
            }

        }
        IconButton(onClick = { onAdd(groupName, groupItem!!) }) {
            Icon(painterResource(Res.drawable.add_24px), "Add")
        }
        IconButton(onClick = onCancel) {
            Icon(painterResource(Res.drawable.cancel_24px), contentDescription = "Cancel")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> LazyStaggeredGridScope.dashboardSection(title: String, onReload: () -> Unit, onUpdateName: (String) -> Unit, onDelete: () -> Unit, items: List<T>, count: Long?, emptyMessage: String, reloading: Boolean, content: @Composable LazyStaggeredGridItemScope.(T) -> Unit) {
    item(span = StaggeredGridItemSpan.FullLine) {
        SectionTitle(title, onReload, onUpdateName, onDelete, count)
    }
    if (items.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (reloading) {
        item(span = StaggeredGridItemSpan.FullLine) {
            LinearWavyProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp), wavelength = 40.dp, waveSpeed = 10.dp)
        }
    }
    items(items, itemContent = content)
}

@Composable
private fun SectionTitle(title: String, onReload: () -> Unit, onUpdateName: (String) -> Unit, onDelete: () -> Unit, count: Long?,) {
    Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        var editing by remember { mutableStateOf(false) }
        if (!editing) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                Text(title, style = MaterialTheme.typography.titleMedium)
                if (count != null) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(count.toString())
                    }
                }
            }
            IconButton(onClick = onReload, modifier = Modifier.size(20.dp)) {
                Icon(painterResource(Res.drawable.refresh_24px), null, modifier = Modifier.size(16.dp))
            }
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.size(20.dp)) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(painterResource(Res.drawable.more_vert_24px), contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar nombre") },
                        onClick = { expanded = false; editing = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        } else {
            var newName by remember { mutableStateOf(title) }
            TextField(value = newName, onValueChange = { newName = it })
            IconButton(onClick = { onUpdateName(newName); editing = false }) {
                Icon(painterResource(Res.drawable.save_24px), contentDescription = "More options")
            }
            IconButton(onClick = { editing = false }) {
                Icon(painterResource(Res.drawable.cancel_24px), contentDescription = null)
            }
        }
    }
}