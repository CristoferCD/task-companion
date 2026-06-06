package es.cristcd.taskcompanion.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.issue.IssueService
import es.cristcd.taskcompanion.issue.dto.IssueListDto
import es.cristcd.taskcompanion.issue.dto.IssueListItemDto
import es.cristcd.taskcompanion.issue.dto.TagDto
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.DashboardItem
import es.cristcd.taskcompanion.persistence.model.DashboardLayout
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.Query
import es.cristcd.taskcompanion.search.SearchService
import es.cristcd.taskcompanion.search.dto.SearchResultDto
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.form.TaskForm
import es.cristcd.taskcompanion.ui.screen.version.VersionResult
import es.cristcd.taskcompanion.ui.screen.version.calculateAnalytics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DashboardViewmodel : ViewModel() {
    val layoutItems: StateFlow<List<DashboardGroup>>
        field = MutableStateFlow(emptyList())
    private var reloadingJob: Job? = null

    val availableQueries: StateFlow<List<RedmineQueriesByProject>>
        field = MutableStateFlow(emptyList())

    val searchResults: StateFlow<List<SearchResultDto>>
        field = MutableStateFlow(emptyList())
    val searching: StateFlow<Boolean>
        field = MutableStateFlow(false)
    private var searchJob: Job? = null

    fun load() {
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
            DashboardItem.AssignedToMe -> DashboardGroupContent.IssueList(IssueService.listAssignedToMe())
            is DashboardItem.CustomQuery -> DashboardGroupContent.IssueList(IssueService.listByQuery(dashboardItem.queryId, dashboardItem.projectId))
            DashboardItem.Monitored -> DashboardGroupContent.IssueList(IssueService.listMonitored())
            DashboardItem.FollowedVersions -> DashboardGroupContent.VersionList(listFollowedVersions())
        }
    }

    fun reloadGroup(groupId: Int) {
        viewModelScope.launch {
            layoutItems.update { items ->
                items.map { group ->
                    if (group.id == groupId) {
                        group.copy(reloading = true)
                    } else {
                        group
                    }
                }
            }

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

            layoutItems.update { loadedList ->
                loadedList.map {
                    if (it.id == group.id) {
                        group
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun reloadAll() {
        reloadingJob?.cancel()
        reloadingJob = viewModelScope.launch {
            layoutItems.value.forEach {
                reloadGroup(it.id)
            }
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            transaction {
                DashboardLayout.deleteWhere { DashboardLayout.id eq groupId }
            }
        }
    }

    fun updateGroupName(groupId: Int, name: String) {
        transaction {
            DashboardLayout.update({ DashboardLayout.id eq groupId }) {
                it[DashboardLayout.title] = name
            }
        }
        viewModelScope.launch {
            layoutItems.update { items ->
                items.map {
                    if (it.id == groupId) {
                        it.copy(title = name)
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun createGroup(name: String, item: DashboardItem) {
        val id = transaction {
            val maxRow = DashboardLayout.row.max()
            val lastRow = DashboardLayout.select(maxRow).firstOrNull()?.get(maxRow) ?: 1
            DashboardLayout.insertAndGetId {
                it[DashboardLayout.row] = lastRow + 1
                it[DashboardLayout.title] = name
                it[DashboardLayout.item] = item
            }
        }

        viewModelScope.launch {
            layoutItems.update {
                it + DashboardGroup(
                    id.value,
                    name,
                    DashboardGroupContent.Loading(item)
                )
            }

            val content = loadItems(item)
            layoutItems.update { currentList ->
                currentList.map {
                    if (it.id == id.value) {
                        it.copy(content = content)
                    } else {
                        it
                    }
                }
            }
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

    fun updateIssueTags(issue: IssueListItemDto, tags: List<TagDto>) {
        viewModelScope.launch {
            IssueService.updateTags(issue.id, tags)
        }
    }

    fun searchQuery(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.length < 3) {
                searchResults.emit(emptyList())
                return@launch
            }
            searching.emit(true)
            val localResults = SearchService.searchLocally(query)
            searchResults.emit(localResults)

            delay(500.milliseconds)
            val apiResults = SearchService.searchRedmine(query)
            val totalResults = localResults + apiResults.filter { apiValue -> localResults.none { it.redmineId == apiValue.redmineId} }
            searchResults.emit(totalResults)
            searching.emit(false)
        }
    }
}

data class DashboardGroup(val id: Int, val title: String, val content: DashboardGroupContent, val reloading: Boolean = false)
sealed interface DashboardGroupContent {
    data class IssueList(val list: IssueListDto) : DashboardGroupContent
    data class VersionList(val list: List<VersionResult.Ok>) : DashboardGroupContent
    data class Loading(val item: DashboardItem) : DashboardGroupContent
}

data class RedmineQueriesByProject(val projectName: String, val queries: List<Query>)

sealed interface SidebarNavigation {
    data object None: SidebarNavigation
    data object Tracker : SidebarNavigation
    data object Projects: SidebarNavigation

    fun toggle(target: SidebarNavigation): SidebarNavigation {
        return if (this == target) {
            SidebarNavigation.None
        } else {
            target
        }
    }
}