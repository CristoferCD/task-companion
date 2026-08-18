package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object VisibleTableColumns : IntIdTable() {
    val index = integer("index")
    val label = varchar("label", 255).uniqueIndex()
    //Global column ordering, might be a problem if several projects share some columns
}