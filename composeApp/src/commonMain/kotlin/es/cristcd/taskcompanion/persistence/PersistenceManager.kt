package es.cristcd.taskcompanion.persistence

import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.DashboardIssueItem
import es.cristcd.taskcompanion.persistence.model.DashboardLayout
import es.cristcd.taskcompanion.persistence.model.FollowedRedmineVersion
import es.cristcd.taskcompanion.persistence.model.RedmineIssue
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.Task
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

object PersistenceManager {
    fun init() {
        val db = Database.connect("jdbc:sqlite:data.db", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // or Connection.TRANSACTION_READ_UNCOMMITTED
        TransactionManager.defaultDatabase = db

        transaction {
            SchemaUtils.create(
                Category,
                DashboardIssueItem,
                DashboardLayout,
                FollowedRedmineVersion,
                RedmineIssue,
                Status,
                Task,
                UserPreferences
            )
        }
    }
}