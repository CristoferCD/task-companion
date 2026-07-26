package es.cristcd.taskcompanion.ui.screen.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.RedmineIssue
import es.cristcd.taskcompanion.redmine.model.Version
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectViewmodel: ViewModel() {
    val version: StateFlow<ProjectResult>
        field = MutableStateFlow<ProjectResult>(ProjectResult.Loading)



    fun load(projectId: Long) {
        viewModelScope.launch {
            version.emit(ProjectResult.Loading)
            val project = RedmineService.getProject(projectId)
            val versionList = RedmineService.listProjectVersions(projectId)
            val issuePager = Pager(
                config = PagingConfig(
                    pageSize = 50,
                ),
                pagingSourceFactory = { IssueListPagingSource(projectId) }
            ).flow.cachedIn(viewModelScope)

            version.emit(ProjectResult.Ok(project, versionList, issuePager))
        }
    }
}

sealed interface ProjectResult {
    data object Loading : ProjectResult
    data class Ok(val project: Project, val versions: List<Version>, val recentIssues: Flow<PagingData<RedmineIssue>>) : ProjectResult
}

private class IssueListPagingSource(val projectId: Long) : PagingSource<Int, RedmineIssue>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RedmineIssue> {
        val offset = params.key ?: 0
        val items = RedmineService.listIssuesByProject(projectId, offset, params.loadSize)

        return LoadResult.Page(
            data = items.issues,
            prevKey = if (offset == 0) null else offset - items.limit.toInt(),
            nextKey = if (items.issues.isEmpty()) null else offset + items.limit.toInt()
        )
    }

    override fun getRefreshKey(state: PagingState<Int, RedmineIssue>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

}