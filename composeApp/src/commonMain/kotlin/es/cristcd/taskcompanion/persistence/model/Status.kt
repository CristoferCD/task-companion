package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Status : IntIdTable() {
    val name = varchar("name", 255)
    val color = long("color")
    val redmineStatusId = long("redmineStatusId").nullable().uniqueIndex()
}