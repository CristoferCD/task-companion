package es.cristcd.taskcompanion.redmine.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class IssueList(
    val issues: List<Issue>
)

data class IssueListAnalytics(
    val totalIssues: Int,
    val byStatus: List<IssueAnalyticsCount>,
    val byCategory: List<IssueAnalyticsCount>,
    val byAssigned: List<IssueAnalyticsCount>,
    val totalStoryPoints: Long,
)

data class IssueAnalyticsCount(val id: IdString?, val count: Int, val storyPoints: Long)

@Serializable
data class Issue @OptIn(ExperimentalTime::class) constructor(
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
)

//Move to dto
fun Issue.storyPoints() = ((customFields.firstOrNull { it.name == "Puntos de historia" } as SimpleCustomField?)?.value?.toLongOrNull() ?: 0)

@Serializable
data class IdString(val id: Long?, val name: String?)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("multiple")
sealed class CustomField() {
    abstract val id: Long
    abstract val name: String
}

@Serializable
data class SimpleCustomField(override val id: Long, override val name: String, val value: String?) : CustomField()

@Serializable
@SerialName("true")
data class MultipleCustomField(override val id: Long, override val name: String, val value: List<String>?) : CustomField()