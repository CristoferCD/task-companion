package es.cristcd.taskcompanion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.core.CachedResult
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.dto.IssueListItemDto
import es.cristcd.taskcompanion.redmine.model.IssueList
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DashboardViewmodel : ViewModel() {
    private var lastUpdated: Instant? = null

    private val _issuesAssigned = MutableStateFlow(emptyList<IssueListItemDto>())
    val issuesAssigned = _issuesAssigned.asStateFlow()

    private val _issuesMonitored = MutableStateFlow(emptyList<IssueListItemDto>())
    val issuesMonitored = _issuesMonitored.asStateFlow()

    private val _versions = MutableStateFlow<List<VersionResult.Ok>>(emptyList())
    val versions = _versions.asStateFlow()

    fun load() {
        if (lastUpdated != null && (Clock.System.now() - lastUpdated!!) < 10.minutes) return
        lastUpdated = Clock.System.now()
        println("${Clock.System.now()} reloaded dashboard")

        viewModelScope.launch {
            loadAssignedToMe()
            loadMonitored()
            loadFollowedVersions()
        }
    }

    fun reloadAssigned() {
        viewModelScope.launch {
            loadAssignedToMe()
        }
    }

    private suspend fun loadAssignedToMe() {
        val currentIssues = _issuesAssigned.value
        val newIssues = RedmineService.listIssuesAssignedToMe().issues.map {
            IssueListItemDto(
                id = it.id,
                project = it.project,
                status = it.status,
                priority = it.priority,
                subject = it.subject,
                updatedOn = it.updatedOn,
                fixedVersion = it.fixedVersion,
                recentlyChanged = currentIssues.find { current -> current.id == it.id }?.updatedOn?.let { oldUpdate -> oldUpdate != it.updatedOn } ?: false
            )
        }
        _issuesAssigned.emit(newIssues)
    }

    fun reloadMonitored() {
        viewModelScope.launch {
            loadMonitored()
        }
    }

    private suspend fun loadMonitored() {
        val currentIssues = _issuesMonitored.value
        val newIssues = RedmineService.listMonitoredIssues().issues.map {
            IssueListItemDto(
                id = it.id,
                project = it.project,
                status = it.status,
                priority = it.priority,
                subject = it.subject,
                updatedOn = it.updatedOn,
                fixedVersion = it.fixedVersion,
                recentlyChanged = currentIssues.find { current -> current.id == it.id }?.updatedOn?.let { oldUpdate -> oldUpdate != it.updatedOn } ?: false
            )
        }
        _issuesMonitored.emit(newIssues)
    }

    private suspend fun loadFollowedVersions() {
        val followedVersions = transaction {
            FollowedRedmineVersion.selectAll().map { it[FollowedRedmineVersion.redmineVersionId] }
        }

        _versions.emit(followedVersions.map {
            val version = RedmineService.getVersion(it)
            val issues = RedmineService.listIssues(it)
            val analytics = calculateAnalytics(issues)
            VersionResult.Ok(version, true, issues, analytics)
        })
    }

    fun reloadVersions() {
        viewModelScope.launch {
            loadFollowedVersions()
        }
    }

    fun startTask(issue: IssueListItemDto) {
        //TODO: duplicate
        viewModelScope.launch {
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
}

sealed interface SidebarNavigation {
    data object None: SidebarNavigation
    data object Tracker : SidebarNavigation
    data object Projects: SidebarNavigation
}