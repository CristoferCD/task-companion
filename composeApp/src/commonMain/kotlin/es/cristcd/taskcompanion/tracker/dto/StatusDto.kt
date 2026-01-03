package es.cristcd.taskcompanion.tracker.dto

import androidx.compose.ui.graphics.Color
import es.cristcd.taskcompanion.persistence.model.Status
import org.jetbrains.exposed.v1.core.ResultRow

data class StatusDto (
    val id: Int,
    val name: String,
    val color: Color,
    val redmineStatusId: Long?,
)

fun ResultRow.toStatusDto(): StatusDto = StatusDto(
    this[Status.id].value,
    this[Status.name],
    Color(this[Status.color]),
    this[Status.redmineStatusId]
)