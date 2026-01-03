package es.cristcd.taskcompanion.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

private val LocalSnackbarController = staticCompositionLocalOf {
    SnackbarController(
        host = SnackbarHostState(),
        scope = CoroutineScope(EmptyCoroutineContext)
    )
}
private val channel = Channel<SnackbarChannelMessage>(capacity = Int.MAX_VALUE)

@Composable
fun SnackbarControllerProvider(content: @Composable (snackbarHost: SnackbarHostState) -> Unit) {
    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackController = remember(scope) { SnackbarController(snackHostState, scope) }

    DisposableEffect(snackController, scope) {
        val job = scope.launch {
            for (payload in channel) {
                snackController.showMessage(
                    message = payload.message,
                    duration = payload.duration,
                    action = payload.action,
                )
            }
        }

        onDispose { job.cancel() }
    }

    CompositionLocalProvider(LocalSnackbarController provides snackController) {
        content(snackHostState)
    }
}

@Immutable
class SnackbarController(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope
) {
    companion object {
        val current
            @Composable
            @ReadOnlyComposable
            get() = LocalSnackbarController.current

        fun showMessage(message: String, action: SnackbarAction? = null, duration: SnackbarDuration = SnackbarDuration.Short) {
            channel.trySend(SnackbarChannelMessage(message, action, duration))
        }
    }

    fun showMessage(message: String, action: SnackbarAction? = null, duration: SnackbarDuration = SnackbarDuration.Short) {
        scope.launch {
            val result = host.showSnackbar(message = message, actionLabel = action?.title, withDismissAction = true, duration = duration)
            if (result == SnackbarResult.ActionPerformed) {
                action?.onActionPress?.invoke()
            }
        }
    }
}

data class SnackbarChannelMessage(
    val message: String,
    val action: SnackbarAction?,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

data class SnackbarAction(val title: String, val onActionPress: () -> Unit)