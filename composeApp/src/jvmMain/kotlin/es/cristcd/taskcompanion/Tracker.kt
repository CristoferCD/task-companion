@file:OptIn(ExperimentalTime::class)

package es.cristcd.taskcompanion

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.TaskDto
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.datetime.*
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.painterResource
import task_companion.composeapp.generated.resources.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tracker(day: LocalDate, viewmodel: TrackerViewmodel = viewModel { TrackerViewmodel() }) {
    LaunchedEffect(day) {
        viewmodel.load(day)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            IconButton(onClick = viewmodel::previousDay) {
                Icon(painterResource(Res.drawable.arrow_left_24px), contentDescription = null)
            }
            val currentDay = viewmodel.currentDay.collectAsState()
            Text(currentDay.value.toString())
            IconButton(onClick = viewmodel::nextDay) {
                Icon(painterResource(Res.drawable.arrow_right_24px), contentDescription = null)
            }
        }

        val tasks = viewmodel.tasks.collectAsState()
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tasks.value) { task ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Max).fillMaxWidth()
                        .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxHeight().width(6.dp).clip(MaterialTheme.shapes.extraLarge).background(Color(task.color)))
                    Column(Modifier.weight(1f).padding(vertical = 0.dp, horizontal = 8.dp)) {
                        Text(
                            modifier = Modifier.padding(0.dp),
                            fontSize = .7.em,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            text = task.code,
                        )
                        Text(modifier = Modifier.padding(bottom = 4.dp, top = 0.dp), fontSize = .9.em, color = MaterialTheme.colorScheme.onBackground, text = task.description)
                    }
                    BadgeDuration(task, modifier = Modifier.padding(end = 8.dp))

                    ListItemActions(task, viewmodel::resume, viewmodel::stop, viewmodel::delete)
                }
                Spacer(Modifier.height(4.dp))

            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            val categories = viewmodel.categories.collectAsState().value
            var expanded by remember { mutableStateOf(false) }
            var selectedCategory by remember { mutableStateOf<CategoryDto?>(null) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.widthIn(max = 150.dp)) {
                OutlinedTextField(
                    label = { Text("Category") },
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
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
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = { selectedCategory = category; expanded = false },
                        )
                    }
                }
            }
            var code by remember { mutableStateOf("") }
            OutlinedTextField(label = { Text("Code") }, value = code, onValueChange = { code = it }, singleLine = true, modifier = Modifier.widthIn(max = 100.dp))

            var description by remember { mutableStateOf("") }
            OutlinedTextField(label = { Text("Description") }, value = description, onValueChange = { description = it }, singleLine = true, modifier = Modifier.weight(1f))
            FilledIconButton(
                onClick = { viewmodel.start(TaskForm(selectedCategory!!.id, code, description)) },
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.height(60.dp).width(54.dp).padding(top = 6.dp)
            ) {
                Icon(painterResource(Res.drawable.add_24px), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun BadgeDuration(task: TaskDto, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(1.dp, Color.LightGray, MaterialTheme.shapes.small).padding(vertical = 2.dp, horizontal = 8.dp),
    ) {
        ProvideTextStyle(TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light)) {
            val formatter = LocalDateTime.Format { hour(); char(':'); minute() }
            val startedAt = task.start.toLocalDateTime(TimeZone.currentSystemDefault()).format(formatter)
            if (task.end == null) {
                Text(text = "Started at $startedAt")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${(task.end - task.start)}")
                    Spacer(Modifier.width(8.dp))

                    val finishedAt = task.end.toLocalDateTime(TimeZone.currentSystemDefault()).format(formatter)
                    Text(text = startedAt)
                    Icon(painterResource(Res.drawable.arrow_right_alt_24px), "today", Modifier.width(18.dp))
                    Text(text = finishedAt)

                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListItemActions(
    it: TaskDto,
    onResumeTask: (TaskDto) -> Unit,
    onStopTask: (TaskDto) -> Unit,
    onDelete: (TaskDto) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    Row(
        Modifier.height(IntrinsicSize.Min).animateContentSize()
            .onPointerEvent(PointerEventType.Enter) { showOptions = true }
            .onPointerEvent(PointerEventType.Exit) { showOptions = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (showOptions) {
            VerticalDivider()
            if (it.end != null) {
                TooltipBox(
                    tooltip = { PlainTooltip { Text("Resume") } },
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { onResumeTask(it) }) {
                        Icon(painterResource(Res.drawable.play_arrow_24px), "resume")
                    }
                }
            }
            if (it.end == null) {
                TooltipBox(
                    tooltip = { PlainTooltip { Text("Stop") } },
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { onStopTask(it) }) {
                        Icon(painterResource(Res.drawable.stop_24px), "stop")
                    }
                }
            }
            TooltipBox(
                tooltip = { PlainTooltip { Text("Delete") } },
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = rememberTooltipState()
            ) {
                IconButton(onClick = { onDelete(it) }) {
                    Icon(painterResource(Res.drawable.delete_24px), "delete")
                }
            }
        }
        IconButton(modifier = Modifier.fillMaxHeight().width(16.dp), onClick = { showOptions = !showOptions }) {
            Icon(painterResource(Res.drawable.more_vert_24px), "options")
        }
    }
}