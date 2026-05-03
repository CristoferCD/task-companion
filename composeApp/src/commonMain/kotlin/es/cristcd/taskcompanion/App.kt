package es.cristcd.taskcompanion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.onClick
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import es.cristcd.taskcompanion.ui.Screen
import es.cristcd.taskcompanion.ui.common.SnackbarControllerProvider
import es.cristcd.taskcompanion.ui.screen.dashboard.Dashboard
import es.cristcd.taskcompanion.ui.screen.issue.IssueScreen
import es.cristcd.taskcompanion.ui.screen.project.ProjectScreen
import es.cristcd.taskcompanion.ui.screen.settings.Settings
import es.cristcd.taskcompanion.ui.screen.tracker.Tracker
import es.cristcd.taskcompanion.ui.screen.version.VersionScreen
import es.cristcd.taskcompanion.ui.theme.AppTheme
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App(navController: NavHostController = rememberNavController()) {
    AppTheme(darkTheme = false) {
        SnackbarControllerProvider { snackbarHostState ->
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard,
                    modifier = Modifier
                        .safeContentPadding()
                        .fillMaxSize()
                        .onClick(matcher = PointerMatcher.mouse(PointerButton.Back)) { navController.navigateUp() }
                ) {
                    composable<Screen.Dashboard> {
                        Dashboard(navController = navController)
                    }
                    composable<Screen.Issue> {
                        val issue = it.toRoute<Screen.Issue>()
                        IssueScreen(issue.id, navController = navController)
                    }
                    composable<Screen.Version> {
                        val version = it.toRoute<Screen.Version>()
                        VersionScreen(version.id, navController = navController)
                    }
                    composable<Screen.Tracker>(typeMap = mapOf(typeOf<LocalDate>() to serializableType<LocalDate>())) {
                        val tracker = it.toRoute<Screen.Tracker>()
                        Tracker(tracker.day)
                    }
                    composable<Screen.Project> {
                        val project = it.toRoute<Screen.Project>()
                        ProjectScreen(project.id, navController = navController)
                    }
                    composable<Screen.Settings> {
                        Settings(navController = navController)
                    }

                }
            }
        }
    }
}

inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {

    override fun put(bundle: SavedState, key: String, value: T) {
        bundle.write { putString(key, json.encodeToString(value)) }
    }

    override fun get(bundle: SavedState, key: String): T? {
        return json.decodeFromString<T?>(bundle.read { getString(key) })
    }

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)
}