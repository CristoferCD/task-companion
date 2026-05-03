package es.cristcd.taskcompanion.ui.screen.issue

import es.cristcd.taskcompanion.redmine.model.Attachment
import es.cristcd.taskcompanion.redmine.model.IssueForm

sealed interface IssueAction {
    data object ToggleWatching : IssueAction
    data class UpdateAttribute(val form: IssueForm) : IssueAction
    data object StartTask : IssueAction
    data object OpenInBrowser : IssueAction
    data class DownloadFile(val attachment: Attachment) : IssueAction
}