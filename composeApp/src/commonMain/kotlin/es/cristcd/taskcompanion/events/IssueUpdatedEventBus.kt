package es.cristcd.taskcompanion.events

import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.dto.JournalDetailDto
import es.cristcd.taskcompanion.redmine.model.CustomField
import es.cristcd.taskcompanion.redmine.model.Journal
import es.cristcd.taskcompanion.redmine.model.JournalDetail
import es.cristcd.taskcompanion.redmine.toDto
import es.cristcd.taskcompanion.ui.screen.issue.abbreviate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(FlowPreview::class)
object IssueUpdatedEventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val changedIssues = MutableStateFlow<List<ChangedIssue>>(emptyList())

    private val unreadEventCache = mutableMapOf<Long, IssueUpdatedEvent>()

    private val loggedUser by lazy {
        transaction {
            UserPreferences.select(UserPreferences.redmineId).firstOrNull().let { it?.get(UserPreferences.redmineId) }
        }

    }

    val unreadUpdatedEvents = merge(changedIssues.take(1), changedIssues.sample(10.seconds))
        .map { currentChanges ->
            currentChanges.mapNotNull { (redmineId, lastSeenAt, updatedOn) ->
                val eventInCache = unreadEventCache[redmineId]
                if (eventInCache != null && eventInCache.updatedOn == updatedOn) {
                    return@mapNotNull eventInCache
                }

                val currentIssue = RedmineService.getIssue(redmineId)
                val newChanges = currentIssue.journals.filter {
                    it.createdOn != null
                            && it.createdOn > lastSeenAt
                            && it.user.id != loggedUser
                }
                if (newChanges.isEmpty()) {
                    return@mapNotNull null
                }

                val summary = summarizeChanges(newChanges, currentIssue.project.id, currentIssue.customFields)
                val event = IssueUpdatedEvent(redmineId, currentIssue.subject, currentIssue.updatedOn ?: Clock.System.now(), summary)
                unreadEventCache[redmineId] = event
                return@mapNotNull event
            }.sortedByDescending { it.updatedOn }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly, // keeps running independent of UI subscribers
            initialValue = emptyList()
        )

    private suspend fun summarizeChanges(changes: List<Journal>, projectId: Long?, customFields: List<CustomField>): ChangeSummary {
        val notes = changes.filter { it.notes?.isNotBlank() == true }
        val notesSummary = if (notes.size == 1) {
            val change = changes.first()
            "${change.user.name}: ${change.notes?.trim().abbreviate(100)}"
        } else if (notes.size > 1) {
            """
                |[${notes.size - 1} comentarios]
                |${notes.last().user.name}: ${notes.last().notes?.trim().abbreviate(100)}
            """.trimMargin()
        } else {
            ""
        }

        val attributeChanges = mutableMapOf<String, JournalDetail>()
        changes.flatMap { it.details }.forEach {
            val key = if (it.property == "attr" || it.property == "cf") {
                "${it.property}:${it.name}"
            } else {
                it.property
            }
            attributeChanges[key] = it
        }
        val attrChanges = attributeChanges.values.map { it.toDto(projectId, customFields) }

        return ChangeSummary(notesSummary, attrChanges)
    }

    fun notifyChanges(changes: List<ChangedIssue>) {
        if (changes.isEmpty()) return
        changedIssues.update { currentChanges ->
            currentChanges.map { currentChange ->
                val updated = changes.find { it.redmineId == currentChange.redmineId }
                updated ?: currentChange
            } + changes.filter { newChange -> currentChanges.none { it.redmineId == newChange.redmineId } }
        }
    }

    fun clearChange(redmineId: Long) {
        changedIssues.update { currentChanges ->
            currentChanges.filter { it.redmineId != redmineId }
        }
    }


}

data class ChangedIssue(val redmineId: Long, val lastSeenAt: Instant, val updatedOn: Instant)

data class IssueUpdatedEvent(val id: Long, val subject: String, val updatedOn: Instant, val summary: ChangeSummary)
data class ChangeSummary(val comments: String, val attributeChanges: List<JournalDetailDto>)