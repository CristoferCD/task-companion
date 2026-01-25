package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class QueriesRoot(val queries: List<Query>)

@Serializable
data class Query(
    val id: Long,
    val name: String,
    val isPublic: Boolean,
    val projectId: Long? = null,
)