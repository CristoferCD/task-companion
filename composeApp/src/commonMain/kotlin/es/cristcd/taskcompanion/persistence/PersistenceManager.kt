package es.cristcd.taskcompanion.persistence

import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.DashboardIssueItem
import es.cristcd.taskcompanion.persistence.model.DashboardLayout
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.persistence.model.Issue
import es.cristcd.taskcompanion.persistence.model.IssueTag
import es.cristcd.taskcompanion.persistence.model.RedmineIssue
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.Tag
import es.cristcd.taskcompanion.persistence.model.Task
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.sql.Connection

object PersistenceManager {
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
                Status,
                Tag,
                Task,
                UserPreferences
            )
        }
    }

    private fun getAppDataDir(): File {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> File(System.getenv("LOCALAPPDATA"), ".TaskCompanion")
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/.TaskCompanion")
            else -> File(System.getProperty("user.home"), ".TaskCompanion")
        }
    }

}