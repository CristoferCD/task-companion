package es.cristcd.taskcompanion.updater

import androidx.compose.material3.SnackbarDuration
import es.cristcd.taskcompanion.AppController
import es.cristcd.taskcompanion.ui.common.SnackbarAction
import es.cristcd.taskcompanion.ui.common.SnackbarController
import es.cristcd.taskcompanion.util.Platform
import es.cristcd.taskcompanion.util.getAppDataDir
import es.cristcd.taskcompanion.util.getCurrentPlatform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.awt.Desktop
import java.io.File

object UpdaterService {
    @OptIn(ExperimentalSerializationApi::class)
    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
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

    suspend fun downloadRelease(release: GithubRelease) {
        val requiredContentType = when(getCurrentPlatform()) {
            Platform.WINDOWS -> ReleaseContentType.WINDOWS_MSI
            Platform.MACOS -> ReleaseContentType.MAC_DMG
            else -> null
        }
        val artifact = requiredContentType?.let { contentType -> release.assets.find { it.contentType == contentType.value } }
        if (artifact != null) {
            client.prepareGet(artifact.browserDownloadUrl).execute { httpResponse ->
                val contentDisposition = httpResponse.headers[HttpHeaders.ContentDisposition]
                val filename = contentDisposition?.let { ContentDisposition.parse(it).parameter("filename") } ?: "task-companion.tmp"
                val output = File(getAppDataDir(), filename)
                val channel: ByteReadChannel = httpResponse.body()
                channel.copyAndClose(output.writeChannel())
                SnackbarController.showMessage("Close app and install update?", action = SnackbarAction("Yes") {
                    Desktop.getDesktop().open(output)
                    AppController.exitApplication()
                }, duration = SnackbarDuration.Indefinite)
            }
        }
    }

    fun cleanOldDownloads() {
        getAppDataDir().walk().filter { it.extension in listOf("msi", "dmg") }.forEach {
            it.delete()
        }
    }
}