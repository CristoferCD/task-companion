package es.cristcd.taskcompanion.ui.screen.issueexplore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import es.cristcd.taskcompanion.filter.ColumnDefinition
import es.cristcd.taskcompanion.filter.VisibleColumnsService
import es.cristcd.taskcompanion.filter.form.ColumnSelectionForm
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.RedmineIssue
import es.cristcd.taskcompanion.ui.screen.dashboard.RedmineQueriesByProject
import es.cristcd.taskcompanion.ui.screen.version.VersionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class IssueExploreViewmodel: ViewModel() {

    val exploreState: StateFlow<ExploreResult>
        field = MutableStateFlow<ExploreResult>(ExploreResult.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(filter: IssueFilter) {
        viewModelScope.launch {
            exploreState.emit(ExploreResult.Loading)
            val issuePager = exploreState.filterIsInstance<ExploreResult.Ok>().flatMapMerge { result ->
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                    ),
                    pagingSourceFactory = { IssueListPagingSource(result.filter) }
                ).flow.cachedIn(viewModelScope)
            }


            val allQueries = RedmineService.listAllQueries()
            val projects = RedmineService.listProjects()
            val queriesByProject = allQueries.groupBy { it.projectId }.map { (projectId, queries) ->
                val foundProject = projects.find { it.id == projectId }
                RedmineQueriesByProject(foundProject?.name ?: "Todos los proyectos", queries)
            }
            val filterOptions = FilterOptions(queriesByProject)
            val visibleColumns = VisibleColumnsService.loadVisibleColumns(null)

            exploreState.emit(ExploreResult.Ok(issuePager, filter, filterOptions, visibleColumns))
        }
    }

    fun updateColumnSelection(updatedColumn: ColumnDefinition) {
        viewModelScope.launch {
            exploreState.update {
                when (it) {
                    is ExploreResult.Ok -> {
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

    private var filterChangeJob: Job? = null
    fun onFilterChange(filter: IssueFilter) {
        filterChangeJob?.cancel()
        filterChangeJob = viewModelScope.launch {
            delay(500.milliseconds)
            exploreState.update {
                when (it) {
                    is ExploreResult.Ok -> { it.copy(filter = filter)}
                    else -> it
                }
            }
        }
    }
}

sealed interface ExploreResult {
    data object Loading: ExploreResult
    data class Ok(val issues: Flow<PagingData<RedmineIssue>>, val filter: IssueFilter, val filterOptions: FilterOptions,  val resultColumns: List<ColumnDefinition>): ExploreResult
}

data class FilterOptions(val queriesByProject: List<RedmineQueriesByProject>)

private class IssueListPagingSource(val filter: IssueFilter) : PagingSource<Int, RedmineIssue>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RedmineIssue> {
        val offset = params.key ?: 0
        return if (filter.customQueryId != null) {
            val items = RedmineService.listIssuesByFilter(filter, offset, params.loadSize)


            LoadResult.Page(
                data = items.issues,
                prevKey = if (offset == 0) null else offset - items.limit.toInt(),
                nextKey = if (items.issues.isEmpty()) null else offset + items.limit.toInt()
            )
        } else {
            LoadResult.Invalid()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, RedmineIssue>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

}
