package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class MembershipList(val memberships: List<Membership>, val totalCount: Int, val offset: Int, val limit: Int)

@Serializable
data class Membership(
    val id: Long,
    val user: IdString?
)