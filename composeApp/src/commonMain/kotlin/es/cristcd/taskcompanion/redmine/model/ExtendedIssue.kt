package es.cristcd.taskcompanion.redmine.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class ExtendedIssueRoot(val issue: ExtendedIssue)

@Serializable
data class ExtendedIssue @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val project: IdString,
    val tracker: IdString,
    val status: IdString,
    val priority: IdString,
    val author: IdString,
    val subject: String,
    val description: String,
    val startDate: LocalDate?,
    val dueDate: LocalDate?,
    val doneRatio: Int = 0,
    val isPrivate: Boolean = false,
    val estimatedHours: Double?,
    val customFields: List<CustomField> = emptyList(),
    val createdOn: Instant?,
    val updatedOn: Instant?,
    val closedOn: Instant?,
    val assignedTo: IdString?,
    val fixedVersion: IdString?,
    val category: IdString?,
    val relations: List<Relation> = emptyList(),
    val journals: List<Journal> = emptyList(),
    val watchers: List<IdString> = emptyList(),
    val allowedStatuses: List<IssueStatus> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)

@Serializable
data class Relation(
    val id: Long,
    val issueId: Long,
    val issueToId: Long,
    val relationType: RelationType,
    val delay: String?
)

@Serializable
enum class RelationType {
    @SerialName("relates") RELATES,
    @SerialName("duplicates") DUPLICATES,
    @SerialName("duplicated") DUPLICATED,
    @SerialName("blocks") BLOCKS,
    @SerialName("blocked") BLOCKED,
    @SerialName("precedes") PRECEDES,
    @SerialName("follows") FOLLOWS,
    @SerialName("copied_to") COPIED_TO,
    @SerialName("copied_from") COPIED_FROM
}

@Serializable
data class Attachment(
    val id: Long,
    val filename: String,
    val filesize: Long,
    val contentType: String,
    val description: String,
    val contentUrl: String,
    val author: IdString,
    val createdOn: Instant,
    val thumbnailUrl: String? = null
)

@Serializable
data class Journal @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val user: IdString,
    val notes: String?,
    val createdOn: Instant?,
    val privateNotes: Boolean,
    val details: List<JournalDetail> = emptyList(),
)

@Serializable
data class JournalDetail (
    val property: String,
    val name: String,
    val oldValue: String?,
    val newValue: String?,
)