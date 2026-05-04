package es.cristcd.taskcompanion.search.dto

import es.cristcd.taskcompanion.persistence.model.Issue
import es.cristcd.taskcompanion.persistence.model.Status
import org.jetbrains.exposed.v1.core.ResultRow

data class SearchResultDto(val redmineId: Long, val subject: String, val status: String? = null, val statusColor: Long? = null)

fun ResultRow.toSearchResultDto() = SearchResultDto(
    redmineId = this[Issue.redmineId] ?: error("Search only accepts redmine issues"),
    subject = this[Issue.subject],
    status = this[Status.name],
    statusColor =  this[Status.color]
)