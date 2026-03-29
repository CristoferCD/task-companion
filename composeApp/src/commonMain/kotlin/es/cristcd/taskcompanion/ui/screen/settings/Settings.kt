@file:OptIn(ExperimentalMaterial3Api::class)

package es.cristcd.taskcompanion.ui.screen.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.cristcd.taskcompanion.issue.dto.TagDto
import es.cristcd.taskcompanion.issue.form.NewTagForm
import es.cristcd.taskcompanion.persistence.model.CategoryType
import es.cristcd.taskcompanion.redmine.model.IssueStatus
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.User
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.StatusDto
import es.cristcd.taskcompanion.tracker.form.CategoryForm
import es.cristcd.taskcompanion.tracker.form.StatusForm
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(navController: NavHostController, viewmodel: SettingsViewmodel = viewModel { SettingsViewmodel() }) {
    LaunchedEffect(true) {
        viewmodel.loadRedmineInfo()
        viewmodel.loadCategories()
        viewmodel.loadProjects()
        viewmodel.loadStatuses()
        viewmodel.loadRedmineStatuses()
        viewmodel.loadTags()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 12.dp, start = 12.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val redmineUser = viewmodel.redmineUser.collectAsState().value
            RedmineSettings(redmineUser, onSave = viewmodel::saveRedmineSettings, viewmodel::logout)

            val categories = viewmodel.categories.collectAsState().value
            val projects = viewmodel.projects.collectAsState().value
            CategorySettings(categories, projects, viewmodel::saveCategory, viewmodel::deleteCategory)

            val statuses = viewmodel.statuses.collectAsState().value
            val redmineStatuses = viewmodel.redmineStatuses.collectAsState().value
            StatusesSettings(statuses, redmineStatuses, viewmodel::saveStatus, viewmodel::deleteStatus, viewmodel::importStatusFromRedmine)

            val tags = viewmodel.tags.collectAsState().value
            TagsSettings(tags, viewmodel::createTag, viewmodel::deleteTag)
        }

    }
}

@Composable
fun RedmineSettings(redmineUser: RedmineUserResult, onSave: (url: String, key: String) -> Unit, onLogout: () -> Unit) {
    Column(modifier = Modifier.widthIn(max = 300.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Redmine", style = MaterialTheme.typography.titleSmall)

        when (redmineUser) {
            is RedmineUserResult.Loading -> Text(text = "Loading...")
            is RedmineUserResult.NotLoggedIn -> RedmineSettingsForm(onSave)
            is RedmineUserResult.Ok -> RedmineProfile(redmineUser.user, onLogout)
        }

    }
}

@Composable
fun CategorySettings(categories: List<CategoryDto>, projects: List<Project>, onNewCategory: (CategoryForm) -> Unit, onDelete: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryForm?>(null) }

    SettingsSection(
        "Categories",
        content = {
            categories.forEach { category ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(category.name) },
                    leadingContent = {
                        Box(Modifier.padding(end = 8.dp).size(18.dp).clip(CircleShape).background(category.color))
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingCategory = CategoryForm(category.id, category.name, category.type == CategoryType.WORK, category.color.value.toLong(), category.redmineProjectId); showDialog = true } ) {
                                Icon(painterResource(Res.drawable.edit_24px), contentDescription = null)
                            }
                            IconButton(onClick = { onDelete(category.id) } ) {
                                Icon(painterResource(Res.drawable.delete_24px), contentDescription = null)
                            }
                        }
                    }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = { editingCategory = null; showDialog = true }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(text = "Add", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
    if (showDialog) {
        CategoryDialog(editingCategory, projects, onConfirm = { form -> onNewCategory(form); showDialog = false}, onDismiss = { showDialog = false })
    }
}

@Composable
fun CategoryDialog(initialValue: CategoryForm?, projects: List<Project>, onConfirm: (CategoryForm) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.width(1000.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp)) {
                Text(text = "Category", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                var name by remember { mutableStateOf(initialValue?.name ?:"") }
                var redmineId by remember { mutableStateOf<Long?>(initialValue?.redmineId) }

                var expanded by remember { mutableStateOf(false) }
                var selectedOption by remember { mutableStateOf(projects.firstOrNull { it.id == initialValue?.redmineId }) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        label = { Text("From Redmine") },
                        value = selectedOption?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear") },
                            onClick = {
                                selectedOption = null
                                name = ""
                                redmineId = null
                                expanded = false
                            }
                        )
                        projects.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedOption = option
                                    name = option.name
                                    redmineId = option.id
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(label = { Text("Name") }, value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())

                var color by remember { mutableStateOf<Long?>(null) }
                ColorSelector(initialValue?.color, { color = it })

                var countAsWork by remember { mutableStateOf(initialValue?.work ?: true) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Is work")
                    Switch(checked = countAsWork, onCheckedChange = { countAsWork = it })
                }

                //TODO: validation
                Button(onClick = { onConfirm(CategoryForm(initialValue?.id, name, countAsWork, color!!, redmineId )) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Save")
                }
            }
        }
    }
}

fun String.toColorInt(): Int? {
    try {
        val colorStr = removePrefix("#")
        var color = colorStr.toLong(16)
        if (colorStr.length == 6) {
            color = color or 0x00000000ff000000L
        }
        return color.toInt()
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun RedmineSettingsForm(onSave: (url: String, key: String) -> Unit) {
    var redmineUrl by remember { mutableStateOf("") }
    var redmineApiKey by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(label = { Text("Url") }, value = redmineUrl, onValueChange = { redmineUrl = it }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(label = { Text("API Key") }, value = redmineApiKey, onValueChange = { redmineApiKey = it }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(redmineUrl, redmineApiKey) }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), shape = MaterialTheme.shapes.extraSmall) {
            Text(text = "Save")
        }
    }
}

@Composable
fun RedmineProfile(redmineUser: User, onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(Res.drawable.account_circle_24px), tint =  Color(0x6B000000), modifier = Modifier.size(32.dp), contentDescription = null)
            Text(text = "User: ${redmineUser.firstname} ${redmineUser.lastname} (${redmineUser.mail})", style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors().copy(contentColor = Color.Red), shape = MaterialTheme.shapes.small) {
            Text(text = "Log out", style = MaterialTheme.typography.labelMedium)
        }
    }
}



@Composable
fun StatusesSettings(statuses: List<StatusDto>, redmineStatuses: List<IssueStatus>, onNewStatus: (StatusForm) -> Unit, onDelete: (Int) -> Unit, onImportRedmine: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var editingStatus by remember { mutableStateOf<StatusForm?>(null) }

    SettingsSection(
        "Status",
        content = {
            statuses.forEachIndexed { idx, status ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(status.name, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent = {
                        Box(Modifier.padding(end = 8.dp).size(18.dp).clip(CircleShape).background(status.color))
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingStatus = StatusForm(status.id, status.name, status.color.value.toLong(), status.redmineStatusId); showDialog = true }) {
                                Icon(painterResource(Res.drawable.edit_24px), contentDescription = null)
                            }
                            IconButton(onClick = { onDelete(status.id) }) {
                                Icon(painterResource(Res.drawable.delete_24px), contentDescription = null)
                            }
                        }
                    }
                )
                if (idx != (statuses.size - 1)) {
                    HorizontalDivider(Modifier.padding(0.dp))
                }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onImportRedmine,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(text = "Import from redmine", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { editingStatus = null; showDialog = true },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(text = "Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )

    if (showDialog) {
        StatusDialog(editingStatus, redmineStatuses, onConfirm = { form -> onNewStatus(form); showDialog = false}, onDismiss = { showDialog = false })
    }

}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit, actions: @Composable ColumnScope.() -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.widthIn(max = 300.dp).padding(8.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)

            Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                content()
            }

            actions()
        }
    }
}

@Composable
fun StatusDialog(initialValue: StatusForm?, redmineStatuses: List<IssueStatus>, onConfirm: (StatusForm) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.width(1000.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp)) {
                Text(text = "Status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                var name by remember { mutableStateOf(initialValue?.name ?:"") }
                var redmineId by remember { mutableStateOf<Long?>(initialValue?.redmineId) }

                var expanded by remember { mutableStateOf(false) }
                var selectedOption by remember { mutableStateOf(redmineStatuses.firstOrNull { it.id == initialValue?.redmineId }) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        label = { Text("From Redmine") },
                        value = selectedOption?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear") },
                            onClick = {
                                selectedOption = null
                                name = ""
                                redmineId = null
                                expanded = false
                            }
                        )
                        redmineStatuses.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedOption = option
                                    name = option.name
                                    redmineId = option.id
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(label = { Text("Name") }, value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())

                var color by remember { mutableStateOf<Long?>(null) }
                ColorSelector(initialValue?.color, { color = it })

                //TODO: validation
                Button(onClick = { onConfirm(StatusForm(initialValue?.id, name, color!!, redmineId )) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun TagsSettings(tags: List<TagDto>, onNewTag: (NewTagForm) -> Unit, onDelete: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<NewTagForm?>(null) }

    SettingsSection(
        "Tags",
        content = {
            tags.forEachIndexed { idx, tag ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(tag.name, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent = {
                        if (tag.color != null) {
                            Box(Modifier.padding(end = 8.dp).size(18.dp).clip(CircleShape).background(Color(tag.color)))
                        }
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingTag = NewTagForm(tag.id, tag.name, tag.color?.let { Color(it).value.toLong() } ?: 0); showDialog = true }) {
                                Icon(painterResource(Res.drawable.edit_24px), contentDescription = null)
                            }
                            IconButton(onClick = { onDelete(tag.id) }) {
                                Icon(painterResource(Res.drawable.delete_24px), contentDescription = null)
                            }
                        }
                    }
                )
                if (idx != (tags.size - 1)) {
                    HorizontalDivider(Modifier.padding(0.dp))
                }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { editingTag = null; showDialog = true },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(text = "Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )

    if (showDialog) {
        TagDialog(editingTag, onConfirm = { form -> onNewTag(form); showDialog = false}, onDismiss = { showDialog = false })
    }

}

@Composable
fun TagDialog(initialValue: NewTagForm?, onConfirm: (NewTagForm) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.width(1000.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp)) {
                Text(text = "Status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                var name by remember { mutableStateOf(initialValue?.name ?:"") }

                OutlinedTextField(label = { Text("Name") }, value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())

                var color by remember { mutableStateOf<Long?>(null) }
                ColorSelector(initialValue?.color, { color = it })

                //TODO: validation
                Button(onClick = { onConfirm(NewTagForm(initialValue?.id, name, color!! )) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun ColorSelector(initialColor: Long?, onValueChange: (Long) -> Unit) {
    val examplePalette = listOf("#f94144", "#f3722c", "#f8961e", "#f9844a", "#f9c74f", "#90be6d", "#43aa8b", "#4d908e", "#577590", "#277da1")
    var hexCode by remember { mutableStateOf(initialColor?.let { it shr 32 }?.toHexString()?.substring(8) ?: "") }
    var paletteDropdownExpanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(label = { Text("Color") }, value = hexCode, onValueChange = { hexCode = it; hexCode.toColorInt()?.toLong()?.let { onValueChange(it) }}, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(62.dp).padding(top = 7.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
                .clip(MaterialTheme.shapes.small)
                .background(hexCode.toColorInt()?.let { Color(it) } ?: Color.Transparent)
                .clickable(onClick = { paletteDropdownExpanded = true })
        ) {
            DropdownMenu(
                expanded = paletteDropdownExpanded,
                onDismissRequest = { paletteDropdownExpanded = false },
            ) {
                examplePalette.map { colorSuggestion ->
                    DropdownMenuItem(text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(colorSuggestion.toColorInt()!!)))
                            Text(colorSuggestion)
                        }
                    }, onClick = { hexCode = colorSuggestion; paletteDropdownExpanded = false; hexCode.toColorInt()?.toLong()?.let { onValueChange(it) }})
                }
            }
        }
    }
}

@Preview
@Composable
fun RedmineSettingsPreview() {
//    RedmineSettings(RedmineUserResult.Ok(User(1L, "user", "Nombre", "Apellido", "ccc@abc.com"))) { _, _ -> }
    RedmineSettings(RedmineUserResult.NotLoggedIn, { _, _ -> }, {  })
}