package es.cristcd.taskcompanion.issue.dto

import es.cristcd.taskcompanion.persistence.model.Issue
import es.cristcd.taskcompanion.persistence.model.RedmineIssue
import es.cristcd.taskcompanion.persistence.model.Status
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.time.Instant

data class IssueHistoryItemDto(
    val redmineId: Long,
    val subject: String,
    val status: String? = null,
    val statusColor: Long? = null,
    val lastVisited: Instant
)

fun ResultRow.toHistoryItemDto() = IssueHistoryItemDto(
    redmineId = this[Issue.redmineId] ?: error("History only accepts redmine issues"),
    subject = this[Issue.subject],
    status = this[Status.name],
    statusColor =  this[Status.color],
    lastVisited = this[RedmineIssue.updatedAt],
)
