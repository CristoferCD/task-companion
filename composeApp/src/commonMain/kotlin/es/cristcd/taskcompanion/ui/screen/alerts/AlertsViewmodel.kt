package es.cristcd.taskcompanion.ui.screen.alerts

import androidx.lifecycle.ViewModel
import es.cristcd.taskcompanion.events.IssueUpdatedEvent
import es.cristcd.taskcompanion.events.IssueUpdatedEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class AlertsViewmodel: ViewModel() {

    private val clearedIssues = MutableStateFlow(emptySet<String>())

    val changedIssues = combine(IssueUpdatedEventBus.unreadUpdatedEvents, clearedIssues) { events, read ->
        events.filterNot { "${it.id}${it.updatedOn.epochSeconds}" in read }
    }

    fun clearAlert(issue: IssueUpdatedEvent) {
        clearedIssues.update { it + "${issue.id}${issue.updatedOn.epochSeconds}" }
        IssueUpdatedEventBus.clearChange(issue.id)
    }

    fun clearAll(issues: List<IssueUpdatedEvent>) {
        clearedIssues.update { cleared -> cleared + issues.map { "${it.id}${it.updatedOn.epochSeconds}" } }
        issues.forEach {
            IssueUpdatedEventBus.clearChange(it.id)
        }
    }

}