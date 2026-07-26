package es.cristcd.taskcompanion.ui

import es.cristcd.taskcompanion.ui.screen.issueexplore.IssueFilter
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    object Dashboard

    @Serializable
    data class Issue(val id: Long)

    @Serializable
    data class Version(val id: Long)

    @Serializable
    data class Tracker(val day: LocalDate)

    @Serializable
    data class Project(val id: Long)

    @Serializable
    object Settings

    @Serializable
    data class IssueExplore(val filter: IssueFilter)
}