package es.cristcd.taskcompanion.ui.screen.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.IssueList
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.Version
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
            val recentIssues = RedmineService.listIssuesByProject(projectId)
            version.emit(ProjectResult.Ok(project, versionList, recentIssues))
        }
    }
}

sealed interface ProjectResult {
    data object Loading : ProjectResult
    data class Ok(val project: Project, val versions: List<Version>, val recentIssues: IssueList) : ProjectResult
}