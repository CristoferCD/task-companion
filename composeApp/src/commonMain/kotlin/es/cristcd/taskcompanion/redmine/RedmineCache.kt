package es.cristcd.taskcompanion.redmine

import es.cristcd.taskcompanion.redmine.model.IssuePriority
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.sample
import kotlin.time.Duration.Companion.minutes

object RedmineCache {

    private var priorities: Map<Long, IssuePriority> = emptyMap()

    private var usernames: MutableMap<Long, String> = mutableMapOf()

    private var versionNames: MutableMap<Long, String> = mutableMapOf()

    private val loadingPrioritiesMutex = Mutex()
    suspend operator fun get(priority: RedmineEntity.Priority): IssuePriority? {
        priorities[priority.id]?.let { return it }

        loadingPrioritiesMutex.withLock {
            priorities = RedmineService.listPriorities().associateBy { it.id }
        }
        return priorities[priority.id]
    }

    private val loadingUsernamesMutex = Mutex()
    suspend operator fun get(user: RedmineEntity.User): String {
        usernames[user.id]?.let { return it }

        loadingUsernamesMutex.withLock {
            //Check if loaded while waiting for lock
            usernames[user.id]?.let { return it }
            val cacheHit = RedmineUserCacheService.getUsername(user.id)
            if (cacheHit != null) {
                usernames[user.id] = cacheHit
            } else {
                val apiHit = RedmineService.getUser(user.id)
                if (apiHit != null) {
                    usernames[user.id] = listOfNotNull(apiHit.firstname, apiHit.lastname).joinToString(" ")
                    RedmineUserCacheService.update(apiHit)
                }
            }
        }

        //To avoid reevaluating the membership list for a user not found, insert the id in the cache
        return usernames.getOrPut(user.id) { user.id.toString() }
    }

    private val loadingVersionsMutex = Mutex()
    suspend operator fun get(version: RedmineEntity.Version): String? {
        versionNames[version.id]?.let { return it }

        loadingVersionsMutex.withLock {
            versionNames[version.id]?.let { return it }

            val versions = RedmineService.listProjectVersions(version.projectId)
            versions.forEach { version ->
                versionNames[version.id] = version.name
            }
        }
        return versionNames[version.id]
    }

    @OptIn(FlowPreview::class)
    suspend fun registerUsernameRefresh() {
        RedmineUserCacheService.usernameInvalidationFlow().sample(5.minutes).collect { ids ->
            ids.forEach { userId ->
                val apiHit = RedmineService.getUser(userId)
                if (apiHit != null) {
                    usernames[userId] = listOfNotNull(apiHit.firstname, apiHit.lastname).joinToString(" ")
                    RedmineUserCacheService.update(apiHit)
                }
            }
        }
    }
}

sealed class RedmineEntity {
    abstract val id: Long

    data class User(override val id: Long, val projectId: Long) : RedmineEntity()
    data class Priority(override val id: Long) : RedmineEntity()
    data class Version(override val id: Long, val projectId: Long) : RedmineEntity()
}