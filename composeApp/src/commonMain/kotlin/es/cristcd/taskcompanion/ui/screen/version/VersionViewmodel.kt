package es.cristcd.taskcompanion.ui.screen.version

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.IdString
import es.cristcd.taskcompanion.redmine.model.RedmineIssue
import es.cristcd.taskcompanion.redmine.model.IssueAnalyticsCount
import es.cristcd.taskcompanion.redmine.model.IssueList
import es.cristcd.taskcompanion.redmine.model.IssueListAnalytics
import es.cristcd.taskcompanion.redmine.model.Version
import es.cristcd.taskcompanion.redmine.model.storyPoints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class VersionViewmodel : ViewModel() {
    val version: StateFlow<VersionResult>
        field = MutableStateFlow<VersionResult>(VersionResult.Loading)

    fun loadVersion(id: Long) {
        viewModelScope.launch {
            version.emit(VersionResult.Loading)
            val redmineVersion = RedmineService.getVersion(id)
            val issues = RedmineService.listIssues(id)
            val analytics = calculateAnalytics(issues)
            val following = isFollowingVersion(redmineVersion.id)
            version.emit(VersionResult.Ok(redmineVersion, following, issues, analytics))
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
    data class Ok(val version: Version, val following: Boolean, val issueList: IssueList, val analytics: IssueListAnalytics) : VersionResult
}

