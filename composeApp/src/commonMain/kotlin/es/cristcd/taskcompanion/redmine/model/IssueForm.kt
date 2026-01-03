package es.cristcd.taskcompanion.redmine.model

import kotlinx.serialization.Serializable

@Serializable
data class IssueFormRoot(val issue: IssueForm)

@Serializable
data class IssueForm(
    val fixedVersionId: Long? = null,
    val categoryId: Long? = null,
    val trackerId: Long? = null,
    val statusId: Long? = null,
)