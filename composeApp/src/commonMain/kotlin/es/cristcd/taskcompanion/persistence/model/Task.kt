package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Task : IntIdTable() {
    val code = varchar("code", 50)
    val category = reference("category_id", Category)
    val description = varchar("description", 2000)
    val start = timestamp("start")
    val end = timestamp("end").nullable()
    val redmineId = long("redmine_id").nullable()
}