@file:OptIn(ExperimentalTime::class)

package es.cristcd.taskcompanion.redmine.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class VersionRoot(val version: Version)

@Serializable
data class VersionList(val versions: List<Version>)

@Serializable
data class Version(
    val id: Long,
    val project: IdString,
    val name: String,
    val description: String?,
    val status: String,
    val dueDate: LocalDate?,
    val sharing: String,
    val wikiPageTitle: String?,
    val estimatedHours: Double = 0.0,
    val spentHours: Double = 0.0,
    val createdOn: Instant,
    val updatedOn: Instant,
)