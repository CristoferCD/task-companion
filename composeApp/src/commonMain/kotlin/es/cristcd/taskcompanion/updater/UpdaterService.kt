package es.cristcd.taskcompanion.updater

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

object UpdaterService {
    @OptIn(ExperimentalSerializationApi::class)
    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                explicitNulls = false
                namingStrategy = JsonNamingStrategy.SnakeCase
            })
        }
        install(HttpCache) {

        }
        expectSuccess = true
    }

    fun currentVersion(): String? {
        return System.getProperty("jpackage.app-version")
    }

    fun isUpdateAvailable(githubRelease: GithubRelease): Boolean {
        val currentVersion = currentVersion() ?: return false
        val githubSemver = githubRelease.name.replace("v", "").toSemver()
        val currentSemver = currentVersion.toSemver()

        if (currentSemver == null || githubSemver == null) {
            return false
        }
        return currentSemver < githubSemver
    }

    suspend fun latestGithubRelease(): GithubRelease? {
        return try {
            client.get("https://api.github.com/repos/CristoferCD/task-companion/releases/latest")
                .body<GithubRelease>()
        } catch (e: Exception) {
            null
        }
    }
}