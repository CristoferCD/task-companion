package es.cristcd.taskcompanion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.TaskDto
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrackerViewmodel : ViewModel() {
    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _categories = MutableStateFlow(emptyList<CategoryDto>())
    val categories = _categories.asStateFlow()

    private val _currentDay = MutableStateFlow(today())
    val currentDay = _currentDay.asStateFlow()

    fun load(day: LocalDate) {
        viewModelScope.launch {
            _currentDay.emit(day)
            _tasks.emit(TrackerService.getByDate(day))
            _categories.emit(TrackerService.listCategories())
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