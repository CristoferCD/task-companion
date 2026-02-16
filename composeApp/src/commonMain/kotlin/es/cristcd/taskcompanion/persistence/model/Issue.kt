package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Issue : IntIdTable() {
    val subject = varchar("subject", 1024)
    val status = optReference("status", Status)
    val redmineId = long("redmine_id").nullable().uniqueIndex()
}