package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Category : IntIdTable() {
    val name = varchar("name", 255)
    val type = enumeration<CategoryType>("type")
    val color = long("color")
    val redmineProjectId = long("redmineProjectId").nullable().uniqueIndex()
}

enum class CategoryType {
    WORK,
    REST
}