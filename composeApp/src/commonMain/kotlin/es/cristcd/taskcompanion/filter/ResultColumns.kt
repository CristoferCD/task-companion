package es.cristcd.taskcompanion.filter

import es.cristcd.taskcompanion.redmine.model.RedmineIssue

data class ColumnDefinition(val label: String, val visibleIndex: Int? = null, val content: (RedmineIssue) -> String)
