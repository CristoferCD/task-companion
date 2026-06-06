package es.cristcd.taskcompanion.events

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object ShortcutEventBus {
    private val eventChannel = Channel<ShortcutEvent>(Channel.RENDEZVOUS)
    val events = eventChannel.receiveAsFlow()

    fun send(event: ShortcutEvent) {
        //If there are no receivers registered, just drops the event
        eventChannel.trySend(event)
    }
}

sealed interface ShortcutEvent {
    object Refresh : ShortcutEvent
}

