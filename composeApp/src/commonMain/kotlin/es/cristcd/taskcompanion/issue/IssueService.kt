package es.cristcd.taskcompanion.issue

import es.cristcd.taskcompanion.issue.dto.IssueListDto
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
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object IssueService {

    suspend fun listAssignedToMe(): IssueListDto {
        val redmineIssueList = RedmineService.listIssuesAssignedToMe()
        return IssueListDto(
            loadFromRedmineInfo(redmineIssueList.issues),
            redmineIssueList.totalCount
        )
    }

    suspend fun listMonitored(): IssueListDto {
        val redmineIssueList = RedmineService.listMonitoredIssues()
        return IssueListDto(
            loadFromRedmineInfo(redmineIssueList.issues),
            redmineIssueList.totalCount
        )
    }

    suspend fun listByQuery(queryId: Long, projectId: Long?): IssueListDto {
        val redmineIssueList = RedmineService.listIssuesByQuery(queryId, projectId)
        return IssueListDto(
            loadFromRedmineInfo(redmineIssueList.issues),
            redmineIssueList.totalCount
        )
    }

    private fun loadFromRedmineInfo(redmineIssues: List<RedmineIssue>): List<IssueListItemDto> {
        updateIssuesFromRedmine(redmineIssues)
        val redmineIds = redmineIssues.map { it.id }
        val tagsInDb = listTagsByIssue(redmineIds)
        val mergedTags = updateTagsWithExtracted(tagsInDb, redmineIssues)
        val changedIssues = listChangedIssues(redmineIssues)


        return redmineIssues.map {

            val tagInfo = mergedTags.find { mt -> mt.redmineId == it.id } ?: error("Error extracting tags from title")

            IssueListItemDto(
                id = it.id,
                project = it.project,
                status = it.status,
                priority = it.priority,
                subject = tagInfo.cleanTitle,
                updatedOn = it.updatedOn,
                fixedVersion = it.fixedVersion,
                recentlyChanged = changedIssues.contains(it.id),
                tagInfo.tags,
            )
        }
    }

    private fun updateTagsWithExtracted(tagsInDb: Map<Long, List<TagDto>>, redmineIssues: List<RedmineIssue>): List<ExtractedTitleInfo> {
        return redmineIssues.map { redmineIssue ->
            val (cleanTitle, detectedTags) = extractTagsFromTitle(redmineIssue.subject)
            val existingTags = tagsInDb[redmineIssue.id] ?: emptyList()

            val newTags = detectedTags.filter { existingTags.none { et -> et.name.equals(it.name, ignoreCase = true) } }
            transaction {
                newTags.forEach { tag ->

                    val tagInDbByName = Tag.selectAll()
                        .where { Tag.name.lowerCase() eq tag.name.lowercase() }
                        .singleOrNull()

                    val tagId = tagInDbByName?.get(Tag.id) ?: Tag.insertReturning {
                        it[Tag.name] = tag.name
                        it[Tag.color] = tag.color ?: 0x00000000
                    }.single()[Tag.id]

                    val issueId = Issue.select(Issue.id)
                        .where { Issue.redmineId eq redmineIssue.id }
                        .firstOrNull()?.let { it[Issue.id].value } ?: error("Issue ${redmineIssue.id} not found")

                    IssueTag.insert {
                        it[IssueTag.issue] = issueId
                        it[IssueTag.tag] = tagId
                    }
                }
            }

            ExtractedTitleInfo(redmineIssue.id, cleanTitle, (existingTags + newTags).sortedBy { it.name })
        }
    }

    private data class ExtractedTitleInfo(val redmineId: Long, val cleanTitle: String, val tags: List<TagDto>)

    private fun extractTagsFromTitle(subject: String): Pair<String, List<TagDto>> {
        val tags = mutableListOf<TagDto>()

        var finalText = subject
        do {
            val nextTag = extractSingleTagFromStart(finalText)
            if (nextTag != null) {
                tags.add(TagDto(-1, nextTag.substring(1, nextTag.length - 1)))
                finalText = finalText.substring(nextTag.length, finalText.length).trim()
            }
        } while (nextTag != null)

        return finalText.trim() to tags
    }

    private fun extractSingleTagFromStart(subject: String): String? {
        if (!subject.startsWith('[')) {
            return null
        }

        val endIdx = subject.indexOfFirst { it == ']' }
        if (endIdx == -1) {
            return null
        }

        return subject.substring(0, endIdx + 1)
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
                    it[Issue.updatedOn] = issue.updatedOn
                }
            }
        }
    }

    private fun listChangedIssues(redmineIssues: List<RedmineIssue>): List<Long> {
        return transaction {
            Issue.select(Issue.redmineId, Issue.updatedOn)
                .where {
                    Issue.redmineId inList redmineIssues.map { it.id }
                }
                .filter { dbIssue ->
                    redmineIssues.any { it.id == dbIssue[Issue.redmineId] && it.updatedOn != dbIssue[Issue.updatedOn] }
                }
                .mapNotNull { it[Issue.redmineId] }
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
            Tag.selectAll().orderBy(Tag.name)
                .map { TagDto(it[Tag.id].value, it[Tag.name], it[Tag.color]) }
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