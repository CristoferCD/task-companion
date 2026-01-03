package es.cristcd.taskcompanion.tracker

import androidx.compose.ui.graphics.Color
import es.cristcd.taskcompanion.persistence.model.Status
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object SettingsCache {
    val redmineStatusColors: Map<Long, Color> by lazy { loadRedmineStatuses() }

    private fun loadRedmineStatuses(): Map<Long, Color> {
        return transaction {
            Status.select(Status.redmineStatusId, Status.color)
                .where { Status.redmineStatusId.isNotNull() }
                .associate { it[Status.redmineStatusId]!! to Color(it[Status.color]) }
        }
    }
}