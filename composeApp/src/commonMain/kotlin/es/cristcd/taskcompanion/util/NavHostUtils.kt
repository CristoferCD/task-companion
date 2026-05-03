package es.cristcd.taskcompanion.util

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

fun NavHostController.popBackStackIfResumed() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}