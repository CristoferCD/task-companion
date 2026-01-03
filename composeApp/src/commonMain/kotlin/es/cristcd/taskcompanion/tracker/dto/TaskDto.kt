package es.cristcd.taskcompanion.tracker.dto

import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.Task
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class TaskDto(
    val id: Int,
    val code: String,
    val description: String,
    val start: Instant,
    val end: Instant?,
    val redmineId: Long?,
    val categoryId: Int,
    val categoryName: String,
    val color: Long,
)

@OptIn(ExperimentalTime::class)
fun ResultRow.toTaskDto() = TaskDto(
    this[Task.id].value,
    this[Task.code],
    this[Task.description],
    this[Task.start],
    this[Task.end],
    this[Task.redmineId],
    this[Category.id].value,
    this[Category.name],
    this[Category.color],
)
