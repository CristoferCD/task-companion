package es.cristcd.taskcompanion.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.issue.IssueService
import es.cristcd.taskcompanion.issue.dto.IssueHistoryItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewmodel: ViewModel() {
    val recentIssues: StateFlow<List<IssueHistoryItemDto>>
        field = MutableStateFlow(emptyList())

    fun load() {
        viewModelScope.launch {
            recentIssues.emit(IssueService.getLastAccessedIssues())
        }
    }
}