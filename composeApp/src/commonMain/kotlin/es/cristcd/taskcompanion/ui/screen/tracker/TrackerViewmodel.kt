package es.cristcd.taskcompanion.ui.screen.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.TaskDto
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrackerViewmodel : ViewModel() {
    val tasks: StateFlow<List<TaskDto>>
        field = MutableStateFlow<List<TaskDto>>(emptyList())

    val categories: StateFlow<List<CategoryDto>>
        field = MutableStateFlow(emptyList<CategoryDto>())

    val currentDay: StateFlow<LocalDate>
        field = MutableStateFlow(today())

    fun load(day: LocalDate) {
        viewModelScope.launch {
            currentDay.emit(day)
            tasks.emit(TrackerService.getByDate(day))
            categories.emit(TrackerService.listCategories())
        }
    }

    fun start(form: TaskForm) {
        viewModelScope.launch {
            TrackerService.start(form)
            load(today())
        }
    }

    fun resume(task: TaskDto) {
        viewModelScope.launch {
            TrackerService.resume(task.id)
            load(today())
        }
    }

    fun stop(task: TaskDto) {
        viewModelScope.launch {
            TrackerService.stop(task.id)
            load(today())
        }
    }

    fun delete(task: TaskDto) {
        viewModelScope.launch {
            TrackerService.delete(task.id)
            load(today())
        }
    }

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun nextDay() {
        viewModelScope.launch {
            load(currentDay.value.plus(1, DateTimeUnit.DAY))
        }
    }

    fun previousDay() {
        viewModelScope.launch {
            load(currentDay.value.minus(1, DateTimeUnit.DAY))
        }
    }
}