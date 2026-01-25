package es.cristcd.taskcompanion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.DashboardItem
import es.cristcd.taskcompanion.persistence.model.DashboardLayout
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.dto.IssueListItemDto
import es.cristcd.taskcompanion.redmine.model.Issue
import es.cristcd.taskcompanion.redmine.model.Query
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.form.TaskForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DashboardViewmodel : ViewModel() {
    private var lastUpdated: Instant? = null

    val layoutItems: StateFlow<List<DashboardGroup>>
        field = MutableStateFlow(emptyList())

    val availableQueries: StateFlow<List<RedmineQueriesByProject>>
        field = MutableStateFlow(emptyList())

    fun load() {
        if (lastUpdated != null && (Clock.System.now() - lastUpdated!!) < 10.minutes) return
        lastUpdated = Clock.System.now()
        println("${Clock.System.now()} reloaded dashboard")

        viewModelScope.launch {
            loadLayout()
        }
    }

    private suspend fun listFollowedVersions(): List<VersionResult.Ok> {
        val followedVersions = transaction {
            FollowedRedmineVersion.selectAll().map { it[FollowedRedmineVersion.redmineVersionId] }
        }

        return followedVersions.map {
            val version = RedmineService.getVersion(it)
            val issues = RedmineService.listIssues(it)
            val analytics = calculateAnalytics(issues)
            VersionResult.Ok(version, true, issues, analytics)
        }
    }

    private suspend fun loadLayout() {
        val resultSet = transaction {
            DashboardLayout.selectAll()
                .sortedBy { it[DashboardLayout.row] }
        }

        val groupedItems = resultSet.map { row ->
            DashboardGroup(
                row[DashboardLayout.id].value,
                row[DashboardLayout.title],
                loadItems(row[DashboardLayout.item])
            )
        }

        layoutItems.emit(groupedItems)
    }

    private suspend fun loadItems(dashboardItem: DashboardItem): DashboardGroupContent {
        return when(dashboardItem) {
            DashboardItem.AssignedToMe -> DashboardGroupContent.IssueList(RedmineService.listIssuesAssignedToMe().issues.mapToDto())
            is DashboardItem.CustomQuery -> DashboardGroupContent.IssueList(RedmineService.listIssuesByQuery(dashboardItem.queryId, dashboardItem.projectId).issues.mapToDto())
            DashboardItem.Monitored -> DashboardGroupContent.IssueList(RedmineService.listMonitoredIssues().issues.mapToDto())
            DashboardItem.FollowedVersions -> DashboardGroupContent.VersionList(listFollowedVersions())
        }
    }

    fun reloadGroup(groupId: Int) {
        viewModelScope.launch {
            val layoutItem = transaction {
                DashboardLayout.selectAll()
                    .where { DashboardLayout.id eq groupId }
                    .firstOrNull()
            } ?: error("Group not found")

            val group = DashboardGroup(
                layoutItem[DashboardLayout.id].value,
                layoutItem[DashboardLayout.title],
                loadItems(layoutItem[DashboardLayout.item])
            )

            val loadedList = layoutItems.value
            layoutItems.emit(loadedList.map {
                if (it.id == group.id) {
                    group
                } else {
                    it
                }
            })
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            transaction {
                DashboardLayout.deleteWhere { DashboardLayout.id eq groupId }
            }
            loadLayout()
        }
    }

    fun updateGroupName(groupId: Int, name: String) {
        transaction {
            DashboardLayout.update({ DashboardLayout.id eq groupId }) {
                it[DashboardLayout.title] = name
            }
        }
    }

    fun createGroup(name: String, item: DashboardItem) {
        transaction {
            val maxRow = DashboardLayout.row.max()
            val lastRow = DashboardLayout.select(maxRow).firstOrNull()?.get(maxRow) ?: 1
            DashboardLayout.insert {
                it[DashboardLayout.row] = lastRow + 1
                it[DashboardLayout.title] = name
                it[DashboardLayout.item] = item
            }
        }

        viewModelScope.launch {
            loadLayout()
        }
    }

    fun loadRedmineQueries() {
        viewModelScope.launch {
            val allQueries = RedmineService.listAllQueries()
            val projects = RedmineService.listProjects()

            val queriesByProject = allQueries.groupBy { it.projectId }.map { (projectId, queries) ->
                val foundProject = projects.find { it.id == projectId }
                RedmineQueriesByProject(foundProject?.name ?: "Todos los proyectos", queries)
            }
            availableQueries.emit(queriesByProject)
        }
    }

    private fun List<Issue>.mapToDto(currentIssues: List<IssueListItemDto> = emptyList()): List<IssueListItemDto> {
        return map {
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

data class DashboardGroup(val id: Int, val title: String, val content: DashboardGroupContent)
sealed interface DashboardGroupContent {
    data class IssueList(val list: List<IssueListItemDto>) : DashboardGroupContent
    data class VersionList(val list: List<VersionResult.Ok>) : DashboardGroupContent
}

data class RedmineQueriesByProject(val projectName: String, val queries: List<Query>)

sealed interface SidebarNavigation {
    data object None: SidebarNavigation
    data object Tracker : SidebarNavigation
    data object Projects: SidebarNavigation
}