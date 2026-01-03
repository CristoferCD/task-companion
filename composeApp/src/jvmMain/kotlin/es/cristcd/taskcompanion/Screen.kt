package es.cristcd.taskcompanion

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
}