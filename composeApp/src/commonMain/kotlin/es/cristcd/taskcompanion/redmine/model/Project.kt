package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class ProjectRoot(val project: Project)

@Serializable
data class ProjectList(val projects: List<Project>)

@Serializable
data class Project @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val name: String,
    val identifier: String,
    val description: String,
    val customFields: List<IdString> = emptyList(),
    val trackers: List<IdString> = emptyList(),
    val issueCategories: List<IdString> = emptyList(),
    val timeEntryActivities: List<IdString> = emptyList(),
    val issueCustomFields: List<IdString> = emptyList(),
    val createdOn: Instant,
    val updatedOn: Instant,
)