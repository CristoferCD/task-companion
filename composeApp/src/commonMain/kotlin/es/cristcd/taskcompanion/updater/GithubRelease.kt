package es.cristcd.taskcompanion.updater

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GithubRelease(val name: String, val publishedAt: Instant, val htmlUrl: String, val assets: List<ReleaseAsset> = emptyList())

@Serializable
data class ReleaseAsset(val name: String, val contentType: String, val browserDownloadUrl: String)