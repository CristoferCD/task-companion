package es.cristcd.taskcompanion.redmine.dto

import es.cristcd.taskcompanion.redmine.model.*
import es.cristcd.taskcompanion.tracker.dto.StatusDto
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ExtendedIssueDto @OptIn(ExperimentalTime::class) constructor(
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
    val journals: List<JournalDto> = emptyList(),
    val watchers: List<IdString> = emptyList(),
    val allowedStatuses: List<IssueStatus> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)

data class JournalDto @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val user: IdString,
    val notes: String?,
    val createdOn: Instant?,
    val privateNotes: Boolean,
    val details: List<JournalDetailDto> = emptyList(),
)

sealed interface JournalDetailDto {
    data class AttributeChange(val name: String, val oldValue: String?, val newValue: String?) : JournalDetailDto
    data class StatusChange(val name: String, val oldValue: StatusDto?, val newValue: StatusDto?) : JournalDetailDto
    data class Unknown(val property: String, val name: String, val oldValue: String?, val newValue: String?) : JournalDetailDto
}