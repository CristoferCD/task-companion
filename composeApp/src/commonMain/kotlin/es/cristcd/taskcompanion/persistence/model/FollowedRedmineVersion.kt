package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object FollowedRedmineVersion : IntIdTable() {
    val redmineVersionId = long("redmine_version_id")
}