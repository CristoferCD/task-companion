package es.cristcd.taskcompanion.persistence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.json.json

object DashboardLayout: IntIdTable() {
    val row = long("row")
    val title = varchar("title", 255)
    val item = json<DashboardItem>("item", Json)
}

@Serializable
sealed interface DashboardItem {
    @Serializable
    @SerialName("AssignedToMe")
    data object AssignedToMe : DashboardItem
    @Serializable
    @SerialName("Monitored")
    data object Monitored: DashboardItem
    @Serializable
    @SerialName("FollowedVersions")
    data object FollowedVersions: DashboardItem
    @Serializable
    @SerialName("CustomQuery")
    data class CustomQuery(val queryId: Long, val projectId: Long? = null): DashboardItem
}