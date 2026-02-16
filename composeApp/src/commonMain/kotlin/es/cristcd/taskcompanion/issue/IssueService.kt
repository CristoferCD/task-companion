package es.cristcd.taskcompanion.issue

import es.cristcd.taskcompanion.issue.dto.IssueListItemDto
import es.cristcd.taskcompanion.issue.dto.TagDto
import es.cristcd.taskcompanion.issue.form.NewTagForm
import es.cristcd.taskcompanion.persistence.model.Issue
import es.cristcd.taskcompanion.persistence.model.IssueTag
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.Tag
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.RedmineIssue
import org.jetbrains.exposed.v1.core.dao.id.EntityIDFunctionProvider
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

object IssueService {

    suspend fun listByQuery(queryId: Long, projectId: Long?): List<IssueListItemDto> {
        val redmineIssues = RedmineService.listIssuesByQuery(queryId, projectId).issues
        val redmineIds = redmineIssues.map { it.id }
        val tags = listTagsByIssue(redmineIds)

        updateIssuesFromRedmine(redmineIssues)

        return redmineIssues.map {
            IssueListItemDto(
                id = it.id,
                project = it.project,
                status = it.status,
                priority = it.priority,
                subject = it.subject,
                updatedOn = it.updatedOn,
                fixedVersion = it.fixedVersion,
                recentlyChanged = false, //TODO
                tags[it.id] ?: emptyList(),
            )
        }
    }

    private fun updateIssuesFromRedmine(redmineIssues: List<RedmineIssue>) {
        transaction {
            val statuses = Status.selectAll()
                .where { Status.redmineStatusId inList redmineIssues.mapNotNull { it.status.id }}
                .associate { it[Status.redmineStatusId] to it[Status.id] }

            redmineIssues.forEach { issue ->
                Issue.upsert(
                    keys = arrayOf(Issue.redmineId),
                ) {
                    it[Issue.subject] = issue.subject
                    it[Issue.status] = statuses[issue.status.id]
                    it[Issue.redmineId] = issue.id
                }
            }
        }
    }

    private fun listTagsByIssue(redmineIds: List<Long>): Map<Long, List<TagDto>> {
        return transaction {
            IssueTag
                .innerJoin(Tag)
                .innerJoin(Issue)
                .selectAll()
                .where { Issue.redmineId inList redmineIds }
                .groupBy({ it[Issue.redmineId]!! }) { TagDto(it[Tag.id].value, it[Tag.name], it[Tag.color]) }
        }
    }

    fun listTags(): List<TagDto> {
        return transaction {
            Tag.selectAll().map { TagDto(it[Tag.id].value, it[Tag.name], it[Tag.color]) }
        }
    }

    fun updateTags(redmineId: Long, tags: List<TagDto>) {
        transaction {
            val issueId = Issue.select(Issue.id)
                .where { Issue.redmineId eq redmineId }
                .firstOrNull()?.let { it[Issue.id].value } ?: error("Issue $redmineId not found")
            val currentTags = IssueTag.select(IssueTag.tag).where { IssueTag.issue eq issueId }.map { it[IssueTag.tag].value }
            val addedTags = tags.filter { it.id !in currentTags }
            val removedTags = currentTags.filter { current -> current !in tags.map { it.id } }

            addedTags.forEach { tag ->
                IssueTag.insert {
                    it[IssueTag.issue] = issueId
                    it[IssueTag.tag] = tag.id
                }
            }
            if (removedTags.isNotEmpty()) {
                IssueTag.deleteWhere { IssueTag.tag inList removedTags }
            }
        }
    }

    fun createTag(form: NewTagForm) {
        transaction {
            Tag.upsert {
                form.id?.let { tagId -> it[Tag.id] = EntityIDFunctionProvider.createEntityID(tagId, Tag) }
                it[Tag.name] = form.name
                it[Tag.color] = form.color
            }
        }
    }

    fun deleteTag(tagId: Int) {
        transaction {
            IssueTag.deleteWhere { IssueTag.tag eq tagId }
            Tag.deleteWhere { Tag.id eq tagId }
        }
    }
}