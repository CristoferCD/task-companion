package es.cristcd.taskcompanion.ui.screen.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import es.cristcd.taskcompanion.filter.ColumnDefinition
import es.cristcd.taskcompanion.filter.VisibleColumnsService
import es.cristcd.taskcompanion.filter.form.ColumnSelectionForm
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.RedmineIssue
import es.cristcd.taskcompanion.redmine.model.Version
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProjectViewmodel: ViewModel() {
    val project: StateFlow<ProjectResult>
        field = MutableStateFlow<ProjectResult>(ProjectResult.Loading)

    val query: StateFlow<String>
        field = MutableStateFlow("")


    fun load(projectId: Long) {
        viewModelScope.launch {
            project.emit(ProjectResult.Loading)
            val redmineProject = RedmineService.getProject(projectId)
            val versionList = RedmineService.listProjectVersions(projectId)
            val issuePager = query.debounce(300.milliseconds).flatMapLatest { searchQuery ->
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                    ),
                    pagingSourceFactory = { IssueListPagingSource(projectId, searchQuery) }
                ).flow.cachedIn(viewModelScope)
            }
            val visibleColumns = VisibleColumnsService.loadVisibleColumns(redmineProject)

            project.emit(ProjectResult.Ok(redmineProject, versionList, issuePager, visibleColumns))
        }
    }

    fun updateQuery(queryValue: String) {
        viewModelScope.launch {
            query.emit(queryValue)
        }
    }

    fun updateColumnSelection(updatedColumn: ColumnDefinition) {
        viewModelScope.launch {
            project.update {
                when (it) {
                    is ProjectResult.Ok -> {
                        val currentSelection = it.resultColumns
                        val updatedSelection = currentSelection.map { col ->
                            if (col.label == updatedColumn.label) {
                                updatedColumn
                            } else {
                                col
                            }
                        }
                        saveColumnSelection(updatedSelection)
                        it.copy(resultColumns = updatedSelection)
                    }
                    else -> it
                }
            }
        }
    }

    private var savingColumnSelectionJob : Job? = null
    private fun saveColumnSelection(columnSelection: List<ColumnDefinition>) {
        savingColumnSelectionJob?.cancel()
        savingColumnSelectionJob = viewModelScope.launch {
            delay(1.seconds)
            val form = columnSelection.map { ColumnSelectionForm(it.visibleIndex, it.label) }
            VisibleColumnsService.updatePreferences(form)
        }
    }
}

sealed interface ProjectResult {
    data object Loading : ProjectResult
    data class Ok(val project: Project, val versions: List<Version>, val recentIssues: Flow<PagingData<RedmineIssue>>, val resultColumns: List<ColumnDefinition>) : ProjectResult
}

private class IssueListPagingSource(val projectId: Long, val query: String) : PagingSource<Int, RedmineIssue>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RedmineIssue> {
        val offset = params.key ?: 0
        val items = RedmineService.listIssuesByProject(projectId, query, offset, params.loadSize)

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