package es.cristcd.taskcompanion.redmine

import es.cristcd.taskcompanion.redmine.model.IssuePriority
import kotlinx.coroutines.*

object RedmineCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var priorities: Map<Long, IssuePriority> = emptyMap()
    private var loadingPriorities: Job? = null

    private var usernames: MutableMap<Long, String> = mutableMapOf()
    private var loadingUsernames: Job? = null

    private var versionNames: MutableMap<Long, String> = mutableMapOf()
    private var loadingVersions: Job? = null

    suspend operator fun get(priority: RedmineEntity.Priority): IssuePriority? {
        loadingPriorities?.join()
        val cacheHit = priorities[priority.id]
        if (cacheHit != null) {
            return cacheHit
        }
        loadingPriorities = scope.launch {
            priorities = RedmineService.listPriorities().associateBy { it.id }
        }
        loadingPriorities?.join()
        return priorities[priority.id]
    }

    suspend operator fun get(user: RedmineEntity.User): String? {
        loadingUsernames?.join()
        val cacheHit = usernames[user.id]
        if (cacheHit != null) {
            return cacheHit
        }

        loadingUsernames = scope.launch {
            var offset = 0
            val limit = 100
            do {
                val memberships = RedmineService.listUserMemberships(user.projectId, offset, limit)
                val projectUsernames = memberships.memberships.mapNotNull { it.user }.filter { it.id != null }
                projectUsernames.forEach { projectUsername ->
                    usernames[projectUsername.id!!] = projectUsername.name ?: ""
                }
                offset += limit
            } while (usernames[user.id] == null && (offset + limit) < memberships.totalCount)
        }
        loadingUsernames?.join()
        return usernames[user.id]
    }

    suspend operator fun get(version: RedmineEntity.Version): String? {
        loadingVersions?.join()
        val cacheHit = versionNames[version.id]
        if (cacheHit != null) {
            return cacheHit
        }

        loadingVersions = scope.launch {
            val versions = RedmineService.listProjectVersions(version.projectId)
            versions.forEach { version ->
                versionNames[version.id] = version.name
            }
        }
        loadingVersions?.join()
        return versionNames[version.id]
    }
}

sealed class RedmineEntity {
    abstract val id: Long

    data class User(override val id: Long, val projectId: Long) : RedmineEntity()
    data class Priority(override val id: Long) : RedmineEntity()
    data class Version(override val id: Long, val projectId: Long) : RedmineEntity()
}