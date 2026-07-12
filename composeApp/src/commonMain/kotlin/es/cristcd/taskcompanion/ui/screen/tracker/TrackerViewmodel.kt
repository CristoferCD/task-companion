package es.cristcd.taskcompanion.ui.screen.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.TaskDto
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class TrackerViewmodel : ViewModel() {

    val categories: StateFlow<List<CategoryDto>>
        field = MutableStateFlow(emptyList<CategoryDto>())

    val currentDay: StateFlow<LocalDate>
        field = MutableStateFlow(today())

    val tasks: StateFlow<List<TaskDto>> = currentDay.flatMapLatest { TrackerService.observeByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    fun load(day: LocalDate) {
        viewModelScope.launch {
            currentDay.emit(day)
//            tasks.emit(TrackerService.getByDate(day))
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
        }
    }

    fun stop(task: TaskDto) {
        viewModelScope.launch {
            TrackerService.stop(task.id)
        }
    }

    fun delete(task: TaskDto) {
        viewModelScope.launch {
            TrackerService.delete(task.id)
        }
    }

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun nextDay() {
        viewModelScope.launch {
            currentDay.update { it.plus(1, DateTimeUnit.DAY) }
        }
    }

    fun previousDay() {
        viewModelScope.launch {
            currentDay.update { it.minus(1, DateTimeUnit.DAY) }
        }
    }
}