package es.cristcd.taskcompanion.issue.dto

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
    val tags: List<TagDto> = emptyList(),
)

data class TagDto(
    val id: Int,
    val name: String,
    val color: Long,
)