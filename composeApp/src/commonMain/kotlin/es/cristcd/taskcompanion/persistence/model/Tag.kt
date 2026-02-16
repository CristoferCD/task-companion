package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Tag : IntIdTable() {
    val name = varchar("name", 255).uniqueIndex()
    val color = long("color")
}