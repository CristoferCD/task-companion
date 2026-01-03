package es.cristcd.taskcompanion.tracker

import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.Task
import es.cristcd.taskcompanion.tracker.dto.*
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.datetime.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object TrackerService {
    fun getByDate(date: LocalDate): List<TaskDto> {
        val startDay = date.atStartOfDayIn(TimeZone.currentSystemDefault())
        val endDay = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault())
        return transaction {
            (Task innerJoin Category).selectAll().where { (Task.start greaterEq startDay) and (Task.start less endDay) }
                .orderBy(Task.start to SortOrder.ASC)
                .map { it.toTaskDto() }
        }
    }

    fun getFinishedByDate(date: LocalDate): List<TaskDto> {
        val startDay = date.atStartOfDayIn(TimeZone.currentSystemDefault())
        return transaction {
            Task.selectAll().where { (Task.start greaterEq startDay) and (Task.end.isNull()) }
                .orderBy(Task.start to SortOrder.ASC)
                .map { it.toTaskDto() }
        }
    }

    fun start(form: TaskForm) {
        transaction {
            val category = Category.select(Category.id).where { Category.id eq form.categoryId }.single().let { it[Category.id] }
            finalizeLastTask()
            Task.insert {
                it[Task.code] = form.code
                it[Task.category] = category
                it[Task.description] = form.description
                it[Task.start] = Clock.System.now()
            }
        }
    }

    fun resume(taskId: Int) {
        transaction {
            val task = Task.selectAll().where { Task.id eq taskId }.single()
            finalizeLastTask()
            Task.insert {
                it[Task.code] = task[Task.code]
                it[Task.category] = task[Task.category]
                it[Task.description] = task[Task.description]
                it[Task.start] = Clock.System.now()
            }
        }
    }

    fun stop(taskId: Int) {
        transaction {
            Task.update(where = { Task.id eq taskId }) {
                it[Task.end] = Clock.System.now()
            }
        }
    }

    fun delete(taskId: Int) {
        transaction {
            Task.deleteWhere { Task.id eq taskId }
        }
    }

    fun finalizeLastTask() {
        transaction {
            Task.select(Task.id).where { Task.end.isNull() }
                .firstOrNull()?.let { lastTask ->
                    Task.update({ Task.id eq lastTask[Task.id] }) {
                        it[Task.end] = Clock.System.now()
                    }
                }
        }
    }

    fun listCategories(): List<CategoryDto> {
        return transaction {
            Category.selectAll().map { it.toCategoryDto() }
        }
    }

    fun listStatuses(): List<StatusDto> {
        return transaction {
            Status.selectAll().map { it.toStatusDto() }
        }
    }

}