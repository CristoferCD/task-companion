package es.cristcd.taskcompanion.redmine.dto

import es.cristcd.taskcompanion.redmine.model.IdString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class IssueListItemDto @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val project: IdString,
    val status: IdString,
    val priority: IdString,
    val subject: String,
    val updatedOn: Instant?,
    val fixedVersion: IdString?,
    val recentlyChanged: Boolean = false,
)