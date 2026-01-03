package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object UserPreferences: IntIdTable() {
    val redmineId = long("redmine_id").nullable()
    val redmineUrl = varchar("redmineUrl", length = 512).nullable()
    val apiKey = varchar("apiKey", 64).nullable()
}