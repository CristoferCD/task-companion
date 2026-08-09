package es.cristcd.taskcompanion.redmine

import es.cristcd.taskcompanion.persistence.TableObserver
import es.cristcd.taskcompanion.persistence.model.RedmineUser
import es.cristcd.taskcompanion.persistence.model.RedmineUserInvalidation
import es.cristcd.taskcompanion.redmine.model.User
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

object RedmineUserCacheService {

    private val invalidationTableObserver = TableObserver()

    fun getUsername(userId: Long): String? {
        var shouldInvalidate = false
        val username = transaction {
            val user = RedmineUser.selectAll()
                .where { RedmineUser.id eq userId }
                .firstOrNull() ?: return@transaction null

            val itemAge = Clock.System.now() - user[RedmineUser.updatedAt]
            shouldInvalidate = itemAge > 3.days

            listOfNotNull(user[RedmineUser.firstName], user[RedmineUser.lastName]).joinToString(" ")
        }

        if (shouldInvalidate) {
            invalidationTableObserver.writeTransaction {
                RedmineUserInvalidation.insertIgnore {
                    it[RedmineUserInvalidation.userToRefresh] = userId
                }
            }
        }

        return username
    }

    fun update(user: User) {
        transaction {
            RedmineUser.upsert {
                it[id] = user.id
                it[firstName] = user.firstname
                it[lastName] = user.lastname
                it[updatedAt] = Clock.System.now()
            }
            RedmineUserInvalidation.deleteWhere { RedmineUserInvalidation.userToRefresh eq user.id }
        }
    }

    fun usernameInvalidationFlow(): Flow<List<Long>> {
        return invalidationTableObserver.queryAsFlow {
            RedmineUserInvalidation.selectAll()
                .limit(20)
                .orderBy(RedmineUserInvalidation.id, SortOrder.ASC)
                .map { it[RedmineUserInvalidation.userToRefresh].value }
        }
    }

}