package es.cristcd.taskcompanion.redmine

import es.cristcd.taskcompanion.redmine.dto.ExtendedIssueDto
import es.cristcd.taskcompanion.redmine.dto.JournalDetailDto
import es.cristcd.taskcompanion.redmine.dto.JournalDto
import es.cristcd.taskcompanion.redmine.model.CustomField
import es.cristcd.taskcompanion.redmine.model.ExtendedIssue
import es.cristcd.taskcompanion.redmine.model.Journal
import es.cristcd.taskcompanion.redmine.model.JournalDetail
import es.cristcd.taskcompanion.tracker.SettingsCache

suspend fun ExtendedIssue.toDto(): ExtendedIssueDto {
    return ExtendedIssueDto(
        id = id,
        project = project,
        tracker = tracker,
        status = status,
        priority = priority,
        author = author,
        subject = subject,
        description = description,
        startDate = startDate,
        dueDate = dueDate,
        doneRatio = doneRatio,
        isPrivate = isPrivate,
        estimatedHours = estimatedHours,
        customFields = customFields,
        createdOn = createdOn,
        updatedOn = updatedOn,
        closedOn = closedOn,
        assignedTo = assignedTo,
        fixedVersion = fixedVersion,
        category = category,
        relations = relations,
        journals = journals.map { it.toDto(project.id, customFields) },
        watchers = watchers,
        allowedStatuses = allowedStatuses,
        attachments = attachments,
    )
}

private suspend fun Journal.toDto(projectId: Long?, customFields: List<CustomField>): JournalDto {
    return JournalDto(
        id = id,
        user = user,
        notes = notes,
        createdOn = createdOn,
        privateNotes = privateNotes,
        details = details.map { it.toDto(projectId, customFields) },
    )
}

private suspend fun JournalDetail.toDto(projectId: Long?, customFields: List<CustomField>): JournalDetailDto {
    return when(property) {
        "attr" -> {
            when (name) {
                "priority_id" -> {
                    val oldPriority = oldValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.Priority(it)] }
                    val newPriority = newValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.Priority(it)] }
                    JournalDetailDto.AttributeChange("Priority", oldValue = oldPriority?.name, newValue = newPriority?.name)
                }
                "assigned_to_id" -> {
                    if (projectId == null) {
                        return JournalDetailDto.Unknown(property = property, name = name, oldValue = oldValue, newValue = newValue)
                    }
                    val oldUser = oldValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.User(it, projectId)] }
                    val newUser = newValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.User(it, projectId)] }
                    JournalDetailDto.AttributeChange("Assigned to", oldValue = oldUser, newValue = newUser)
                }
                "status_id" -> {
                    val oldStatus = oldValue?.toLongOrNull()?.let { SettingsCache.getStatus(it) }
                    val newStatus = newValue?.toLongOrNull()?.let { SettingsCache.getStatus(it) }
                    JournalDetailDto.StatusChange("Status", oldValue = oldStatus, newValue = newStatus)
                }
                "fixed_version_id" -> {
                    if (projectId == null) {
                        return JournalDetailDto.Unknown(property = property, name = name, oldValue = oldValue, newValue = newValue)
                    }
                    val oldVersion = oldValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.Version(it, projectId)] }
                    val newVersion = newValue?.toLongOrNull()?.let { RedmineCache[RedmineEntity.Version(it, projectId)] }
                    JournalDetailDto.AttributeChange("Version", oldValue = oldVersion, newValue = newVersion)
                }
                else -> {
                    JournalDetailDto.Unknown(property = property, name = name, oldValue = oldValue, newValue = newValue)
                }
            }
        }
        "cf" -> {
            val field = customFields.find { it.id == (this.name.toLongOrNull() ?: 0) }
            if (field != null) {
                JournalDetailDto.AttributeChange(name = field.name, oldValue = oldValue, newValue = newValue)
            } else {
                JournalDetailDto.Unknown(property = property, name = name, oldValue = oldValue, newValue = newValue)
            }
        }
        else -> {
            JournalDetailDto.Unknown(property = property, name = name, oldValue = oldValue, newValue = newValue)
        }
    }
}