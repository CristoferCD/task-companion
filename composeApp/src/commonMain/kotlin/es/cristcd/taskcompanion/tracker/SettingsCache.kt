package es.cristcd.taskcompanion.tracker

import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.tracker.dto.StatusDto
import es.cristcd.taskcompanion.tracker.dto.toStatusDto
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object SettingsCache {
    private var redmineStatus: Map<Long, StatusDto>? = null

    fun getStatus(statusId: Long?): StatusDto? {
        if (redmineStatus == null) {
            redmineStatus = loadRedmineStatuses()
        }
        return redmineStatus!![statusId]
    }

    fun invalidateStatusColorCache() {
        redmineStatus = null
    }

    private fun loadRedmineStatuses(): Map<Long, StatusDto> {
        return transaction {
            Status.selectAll()
                .where { Status.redmineStatusId.isNotNull() }
                .associate { it[Status.redmineStatusId]!! to it.toStatusDto() }
        }
    }
}