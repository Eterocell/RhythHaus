package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.nowplaying.NowPlayingPanel
import kotlinx.coroutines.flow.StateFlow

internal data class NowPlayingPanelPresentation(
    val panel: NowPlayingPanel,
    val onPanelChange: (NowPlayingPanel) -> Unit,
)

@Composable
internal fun rememberNowPlayingPanelPresentation(
    appState: LibraryAppState,
): NowPlayingPanelPresentation {
    val destination = appState.activeDestinationId
    val target = appState.nowPlayingPanelBackTarget()
    DisposableEffect(appState, destination, target) {
        val unregister =
            target?.let { panelTarget ->
                appState.registerBackSurface(
                    LibraryBackSurfacePort(
                        destinationId = destination,
                        foremostFeatureTarget = panelTarget,
                    ) { requested ->
                        if (requested == panelTarget &&
                            appState.dismissNowPlayingPanel(panelTarget)) {
                            LibraryBackFeatureRequestResult.Started
                        } else {
                            LibraryBackFeatureRequestResult.Rejected
                        }
                    },
                )
            } ?: {}
        onDispose(unregister)
    }
    val onPanelChange =
        remember(appState) {
            { panel: NowPlayingPanel -> appState.setNowPlayingPanel(panel) }
        }
    return NowPlayingPanelPresentation(appState.nowPlayingPanel, onPanelChange)
}

internal class QueueMutationDispatcher(
    private val state: StateFlow<PlaybackState>,
    private val reorderCommand: suspend (String, Int) -> QueueMutationResult,
    private val removeCommand: suspend (String) -> QueueMutationResult,
    private val clearCommand: suspend () -> QueueMutationResult,
) {
    suspend fun reorder(
        occurrenceId: String,
        targetIndex: Int,
    ): QueueMutationFeedback = mutation {
        reorderCommand(occurrenceId, targetIndex)
    }

    suspend fun remove(occurrenceId: String): QueueMutationFeedback = mutation {
        removeCommand(occurrenceId)
    }

    suspend fun clear(): QueueMutationFeedback = mutation(clearCommand)

    private suspend fun mutation(
        command: suspend () -> QueueMutationResult,
    ): QueueMutationFeedback {
        val result = command()
        return QueueMutationFeedback(
            refreshedState = state.value,
            showQueueChanged = result is QueueMutationResult.Rejected,
        )
    }
}
