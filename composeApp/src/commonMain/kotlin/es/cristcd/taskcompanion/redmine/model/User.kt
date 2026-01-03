package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class UserRoot(val user: User)

@Serializable
data class User(
    val id: Long,
    val login: String,
    val firstname: String,
    val lastname: String,
    val mail: String,
)