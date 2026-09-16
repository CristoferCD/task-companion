package es.cristcd.taskcompanion.persistence

import es.cristcd.taskcompanion.persistence.model.*
import es.cristcd.taskcompanion.util.getAppDataDir
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.sql.Connection

object PersistenceManager {
    val log = KotlinLogging.logger { }

    fun init() {
        val dbDir = if (System.getenv("app.env") == "dev") {
            "data.db"
        } else {
            val appDataDir = getAppDataDir().apply { mkdirs() }
            "$appDataDir/data.db"
        }
        val db = Database.connect("jdbc:sqlite:$dbDir", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // or Connection.TRANSACTION_READ_UNCOMMITTED
        TransactionManager.defaultDatabase = db

        transaction {
            SchemaUtils.create(
                Category,
                DashboardIssueItem,
                DashboardLayout,
                FollowedRedmineVersion,
                Issue,
                IssueTag,
                RedmineIssue,
                RedmineUser,
                RedmineUserInvalidation,
                Status,
                Tag,
                Task,
                UserPreferences,
                VisibleTableColumns
            )

            MigrationUtils.statementsRequiredForDatabaseMigration(
                Category,
                DashboardIssueItem,
                DashboardLayout,
                FollowedRedmineVersion,
                Issue,
                IssueTag,
                RedmineIssue,
                RedmineUser,
                RedmineUserInvalidation,
                Status,
                Tag,
                Task,
                UserPreferences,
                VisibleTableColumns
            ).forEach {
                log.info { "Executing migration: $it" }
                exec(it)
            }


        }
    }
}