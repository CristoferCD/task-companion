package es.cristcd.taskcompanion.search

import es.cristcd.taskcompanion.persistence.model.Issue
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.search.dto.SearchResultDto
import es.cristcd.taskcompanion.search.dto.toSearchResultDto
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object SearchService {

    fun searchLocally(query: String): List<SearchResultDto> {
        return transaction {
            Issue.join(Status, JoinType.INNER)
                .selectAll()
                .where {
                    (Issue.redmineId.castTo(VarCharColumnType()) like "%$query%").or { Issue.subject like "%$query%" }
                }
                .map { it.toSearchResultDto() }
        }
    }

    suspend fun searchRedmine(query: String): List<SearchResultDto> {
        val results = RedmineService.search(query)
        return results.map {
            SearchResultDto(
                it.id,
                it.title,
            )
        }
    }
}