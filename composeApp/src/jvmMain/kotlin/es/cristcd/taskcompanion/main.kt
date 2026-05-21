package es.cristcd.taskcompanion

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toLong
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import es.cristcd.taskcompanion.persistence.PersistenceManager
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.ui.common.SnackbarController
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
            var density by remember { mutableStateOf(Density(1f, 1f)) }
            LaunchedEffect(Unit) {
                PersistenceManager.init()
                transaction {
                    UserPreferences.select(UserPreferences.scalePercent, UserPreferences.fontScalePercent)
                        .firstOrNull()?.let { prefs ->
                            val scale = prefs[UserPreferences.scalePercent]?.let { it / 100f} ?: 1f
                            val fontScale = prefs[UserPreferences.fontScalePercent]?.let { it / 100f} ?: 1f
                            density = Density(scale, fontScale)
                        }
                }
            }
            CompositionLocalProvider(LocalDensity provides density) {
                App()
            }
        }
    }
}