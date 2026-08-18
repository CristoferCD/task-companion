package es.cristcd.taskcompanion.filter.dto

import es.cristcd.taskcompanion.persistence.model.VisibleTableColumns
import org.jetbrains.exposed.v1.core.ResultRow

data class ColumnSelectionDto(val index : Int, val label : String)

fun ResultRow.toColumnSelectionDto() = ColumnSelectionDto(
    index = this[VisibleTableColumns.index],
    label = this[VisibleTableColumns.label]
)
