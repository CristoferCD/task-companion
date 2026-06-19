package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class IssuePrioritiesList(val issuePriorities: List<IssuePriority>) {
}

@Serializable
data class IssuePriority(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val active: Boolean,
)