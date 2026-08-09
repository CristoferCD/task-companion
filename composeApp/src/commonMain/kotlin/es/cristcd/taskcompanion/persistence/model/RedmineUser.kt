package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

object RedmineUser : LongIdTable() {
    val firstName = varchar("first_name", 50).nullable()
    val lastName = varchar("last_name", 255).nullable()
    val updatedAt = timestamp("updated_at")
}