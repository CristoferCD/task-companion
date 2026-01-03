package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class IssueStatusList(
    val issueStatuses: List<IssueStatus>
)

@Serializable
data class IssueStatus(
    val id: Long,
    val name: String,
    val isClosed: Boolean,
    val description: String?,
)