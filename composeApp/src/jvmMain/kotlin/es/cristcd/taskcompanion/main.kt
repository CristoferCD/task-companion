package es.cristcd.taskcompanion

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import es.cristcd.taskcompanion.persistence.PersistenceManager
import es.cristcd.taskcompanion.ui.common.SnackbarController
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { "Uncaught exception in thread ${Thread.currentThread().name}" }
        SnackbarController.showMessage("Error: ${e.localizedMessage}", duration = SnackbarDuration.Indefinite)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TaskCompanion",
        ) {
            LaunchedEffect(Unit) {
                PersistenceManager.init()
            }
            App()
        }
    }
}