package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Issue : IntIdTable() {
    val subject = varchar("subject", 1024)
    val status = optReference("status", Status)
    val redmineId = long("redmine_id").nullable().uniqueIndex()
    val updatedOn = timestamp("updated_on").nullable()
}