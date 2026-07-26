package es.cristcd.taskcompanion.ui.screen.issueexplore

import kotlinx.serialization.Serializable

@Serializable
data class IssueFilter(val customQueryId: Long? = null, val projectId: Long? = null, val subject: String? = null)