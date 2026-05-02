package es.cristcd.taskcompanion.ui.screen.projectlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectListViewmodel: ViewModel() {
    val projects: StateFlow<List<Project>>
        field = MutableStateFlow<List<Project>>(emptyList())

    fun load() {
        viewModelScope.launch {
            projects.emit(RedmineService.listProjects())
        }
    }
}