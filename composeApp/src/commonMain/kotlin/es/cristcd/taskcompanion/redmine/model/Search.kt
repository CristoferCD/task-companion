package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRoot(val results: List<SearchResult>)

@Serializable
data class SearchResult(
    val id: Long,
    val title: String,
)