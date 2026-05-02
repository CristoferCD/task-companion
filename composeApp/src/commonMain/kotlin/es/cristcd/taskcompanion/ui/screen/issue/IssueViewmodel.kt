@file:OptIn(ExperimentalTime::class)

package es.cristcd.taskcompanion.ui.screen.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.core.CachedResult
import es.cristcd.taskcompanion.core.loadCaching
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.RedmineIssue
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.*
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.json.extract
import java.awt.Desktop
import java.io.File
import java.net.URI
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class IssueViewmodel : ViewModel() {

    val issue: StateFlow<CachedResult<ExtendedIssue>>
        field = MutableStateFlow<CachedResult<ExtendedIssue>>(CachedResult.Loading)

    val watching: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val project: StateFlow<Project?>
        field = MutableStateFlow<Project?>(null)

    val versions: StateFlow<List<Version>>
        field = MutableStateFlow<List<Version>>(emptyList())

    init {
        val userId = getUserId()

        viewModelScope.launch {
            issue.collect {
                val watchers = when (it) {
                    is CachedResult.FromApi -> it.issue.watchers
                    is CachedResult.FromDb -> it.issue.watchers
                    CachedResult.Loading -> emptyList()
                }
                watching.emit(watchers.any { it.id == userId })
            }
        }
    }

    private fun getUserId(): Long? {
        val userId = transaction {
            UserPreferences.select(UserPreferences.redmineId).firstOrNull().let { it?.get(UserPreferences.redmineId) }
        }
        return userId
    }

    @OptIn(ExperimentalTime::class)
    fun loadIssue(id: Long) {
        viewModelScope.launch {
            loadCaching(
                fromDb = {
                    RedmineIssue.selectAll()
                        .where { RedmineIssue.data.extract<Long>(".id") eq id }
                        .firstOrNull()?.let { it[RedmineIssue.data]!! to it[RedmineIssue.updatedAt] }
                },
                fromApi = {
                    val issue = RedmineService.getIssue(id)
                    //FIXME: only online?
                    issue.project.id?.let { loadProjectInfo(it) }
                    val description = issue.description
                    if (issue.allowedStatuses.isEmpty()) {
                        //Api under 5.0 doesn't return statuses
                        val allStatuses = RedmineService.listStatus()
                        issue.copy(description = description, allowedStatuses = allStatuses)
                    } else {
                        issue.copy(description = description)
                    }
                },
                onUpdate = { issue ->
                    val currentId = RedmineIssue.selectAll()
                        .where { RedmineIssue.data.extract<Long>(".id") eq id }
                        .firstOrNull()?.let { it[RedmineIssue.id] }
                    RedmineIssue.upsert {
                        currentId?.let { id -> it[RedmineIssue.id] = id }
                        it[data] = issue
                        it[updatedAt] = Clock.System.now()
                    }
                }
            ).collect { issue.emit(it) }
        }
    }

    private suspend fun loadProjectInfo(projectId: Long) {
        project.emit(RedmineService.getProject(projectId))
        versions.emit(RedmineService.listOpenProjectVersions(projectId))
    }

    fun toggleWatching() {
        viewModelScope.launch {
            val issueId = when (val issueResult = issue.value) {
                is CachedResult.FromDb -> issueResult.issue.id
                is CachedResult.FromApi -> issueResult.issue.id
                else -> null
            }
            val userId = getUserId()

            if (issueId == null || userId == null) {
                return@launch
            }

            val currentlyWatching = watching.value
            if (currentlyWatching) {
                RedmineService.stopWatchingIssue(issueId, userId)
            } else {
                RedmineService.watchIssue(issueId, userId)
            }
            watching.emit(!currentlyWatching)
        }
    }

    fun updateIssueAttribute(form: IssueForm) {
        viewModelScope.launch {
            val issueId = when (val issueResult = issue.value) {
                is CachedResult.FromDb -> issueResult.issue.id
                is CachedResult.FromApi -> issueResult.issue.id
                else -> null
            }

            if (issueId == null) {
                return@launch
            }

            RedmineService.updateIssueAttribute(issueId, form)
            //TODO: don't do a full refresh
            loadIssue(issueId)
        }
    }

    fun startTask() {
        viewModelScope.launch {
            val issue = when (val issueResult = issue.value) {
                is CachedResult.FromDb -> issueResult.issue
                is CachedResult.FromApi -> issueResult.issue
                else -> null
            }

            if (issue == null) {
                return@launch
            }

            val categoryId = transaction {
                Category.select(Category.id)
                    .where { Category.redmineProjectId eq issue.project.id }
                    .singleOrNull()?.let { it[Category.id] }
            }

            if (categoryId == null) {
                return@launch
            }

            TrackerService.start(TaskForm(categoryId.value, issue.id.toString(), issue.subject))
        }
    }

    fun openInBrowser() {
        viewModelScope.launch {
            val issue = when (val issueResult = issue.value) {
                is CachedResult.FromDb -> issueResult.issue
                is CachedResult.FromApi -> issueResult.issue
                else -> null
            }

            if (issue == null) {
                return@launch
            }

            val url = transaction {
                UserPreferences.select(UserPreferences.redmineUrl).singleOrNull()
                    ?.let { it[UserPreferences.redmineUrl] }
            } ?: return@launch


            Desktop.getDesktop().browse(URI("${url}issues/${issue.id}"))
        }
    }

    fun downloadFile(attachment: Attachment) {
        viewModelScope.launch {
            val extensionSeparator = attachment.filename.lastIndexOf('.')
            val extension = attachment.filename.substring(extensionSeparator..<attachment.filename.length)
            val filename = attachment.filename.substring(0..<extensionSeparator)
            val file = File.createTempFile(filename, extension)
            RedmineService.downloadFile(attachment.contentUrl, file)
            Desktop.getDesktop().open(file)
        }
    }
}