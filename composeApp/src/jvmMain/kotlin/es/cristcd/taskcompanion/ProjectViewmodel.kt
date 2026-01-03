package es.cristcd.taskcompanion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectViewmodel: ViewModel() {
    private val _version = MutableStateFlow<ProjectResult>(ProjectResult.Loading)
    val version = _version.asStateFlow()



    fun load(projectId: Long) {
        viewModelScope.launch {
            _version.emit(ProjectResult.Loading)
            val project = RedmineService.getProject(projectId)
            val versionList = RedmineService.listProjectVersions(projectId)
            _version.emit(ProjectResult.Ok(project, versionList))
        }
    }
}

sealed interface ProjectResult {
    data object Loading : ProjectResult
    data class Ok(val project: Project, val versions: List<Version>) : ProjectResult
}