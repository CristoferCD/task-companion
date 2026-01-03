package es.cristcd.taskcompanion.persistence.model

import es.cristcd.taskcompanion.redmine.model.ExtendedIssue
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.json
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object RedmineIssue : IntIdTable() {
    val data = json<ExtendedIssue>("data", Json).nullable()
    val updatedAt = timestamp("updated_at")
}