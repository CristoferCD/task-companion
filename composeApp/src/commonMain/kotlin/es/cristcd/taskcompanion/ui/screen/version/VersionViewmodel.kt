package es.cristcd.taskcompanion.ui.screen.version

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class VersionViewmodel : ViewModel() {
    val version: StateFlow<VersionResult>
        field = MutableStateFlow<VersionResult>(VersionResult.Loading)

    fun loadVersion(versionId: Long) {
        viewModelScope.launch {
            version.emit(VersionResult.Loading)
            val redmineVersion = RedmineService.getVersion(versionId)
            val issuePager = Pager(
                config = PagingConfig(
                    pageSize = 50,
                ),
                pagingSourceFactory = { IssueListPagingSource(versionId) }
            ).flow.cachedIn(viewModelScope)
            val analytics = IssueListAnalytics(0, emptyList(), emptyList(), emptyList(), 0) //calculateAnalytics(issues)
            val following = isFollowingVersion(redmineVersion.id)
            version.emit(VersionResult.Ok(redmineVersion, following, issuePager, analytics))
        }
    }

    private fun isFollowingVersion(versionId: Long): Boolean {
        return transaction {
            FollowedRedmineVersion.selectAll().where { FollowedRedmineVersion.redmineVersionId eq versionId }.count() > 0
        }
    }

    fun toggleFollowVersion() {
        viewModelScope.launch {
            val currentVersion = when(val versionResult = version.value) {
                is VersionResult.Ok -> versionResult
                else -> null
            } ?: return@launch

            val currentlyFollowing = currentVersion.following
            if (currentlyFollowing) {
                transaction {
                    FollowedRedmineVersion.deleteWhere { FollowedRedmineVersion.redmineVersionId eq currentVersion.version.id }
                }
            } else {
                transaction {
                    FollowedRedmineVersion.insert {
                        it[FollowedRedmineVersion.redmineVersionId] = currentVersion.version.id
                    }
                }
            }

            version.emit(currentVersion.copy(following = !currentlyFollowing))
        }
    }
}

//TODO: move to service
fun calculateAnalytics(issueList: IssueList): IssueListAnalytics {
    return IssueListAnalytics(
        issueList.issues.size,
        issueList.issues.getAnalyticsBy { it.status },
        issueList.issues.getAnalyticsBy { it.category },
        issueList.issues.getAnalyticsBy { it.assignedTo },
        issueList.issues.sumOf { it.storyPoints() },
    )
}

private fun List<RedmineIssue>.getAnalyticsBy(key: (RedmineIssue) -> IdString?) =
    groupingBy(key).aggregate { _, accumulator: IssueAnalyticsCount?, element, _ ->
        val sp = element.storyPoints()
        accumulator?.copy(count = accumulator.count + 1, storyPoints = accumulator.storyPoints + sp)
            ?: IssueAnalyticsCount(key(element), 1, sp)
    }.values.toList()


sealed interface VersionResult {
    data object Loading : VersionResult
    data class Ok(val version: Version, val following: Boolean, val issueList: Flow<PagingData<RedmineIssue>>, val analytics: IssueListAnalytics) : VersionResult
}

private class IssueListPagingSource(val versionId: Long) : PagingSource<Int, RedmineIssue>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RedmineIssue> {
        val offset = params.key ?: 0
        val items = RedmineService.listIssuesByVersion(versionId, offset, params.loadSize)

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

