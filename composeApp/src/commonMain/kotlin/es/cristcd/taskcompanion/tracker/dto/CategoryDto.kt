package es.cristcd.taskcompanion.tracker.dto

import androidx.compose.ui.graphics.Color
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.CategoryType
import org.jetbrains.exposed.v1.core.ResultRow

data class CategoryDto(
    val id: Int,
    val name: String,
    val type: CategoryType,
    val color: Color,
    val redmineProjectId: Long?,
)

fun ResultRow.toCategoryDto(): CategoryDto = CategoryDto(
    this[Category.id].value,
    this[Category.name],
    this[Category.type],
    Color(this[Category.color]),
    this[Category.redmineProjectId]
)
