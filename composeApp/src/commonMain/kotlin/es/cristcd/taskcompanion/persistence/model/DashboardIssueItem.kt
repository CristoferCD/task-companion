package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object DashboardIssueItem : IntIdTable() {
    val project = varchar("project", 2000)
    val fixedVersion = varchar("fixed_version", 2000);
    val status = varchar("status", 2000)
    val priority = varchar("priority", 2000)
    val subject = varchar("subject", 2000)
    val updatedOn = timestamp("updated_on").nullable()
    val assignedToMe = bool("assigned_to_me")
    val monitored = bool("monitored")
}