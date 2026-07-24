package com.eterocell.rhythhaus.library.ui

sealed interface TrackSelectionPageKey {
    data object HomeSongs : TrackSelectionPageKey
    data class Album(val album: String) : TrackSelectionPageKey
    data class Artist(val artist: String) : TrackSelectionPageKey
    data object Search : TrackSelectionPageKey
}

data class TrackSelectionState(
    val pageKey: TrackSelectionPageKey? = null,
    val selectedTrackIds: Set<String> = emptySet(),
)

sealed interface TrackSelectionAction {
    data class Start(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class Select(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class Toggle(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class ReconcileVisible(val pageKey: TrackSelectionPageKey, val visibleTrackIds: List<String>) : TrackSelectionAction
    data class RouteChanged(val pageKey: TrackSelectionPageKey?) : TrackSelectionAction
    data object Cancel : TrackSelectionAction
    data object Completed : TrackSelectionAction
}

fun reduceTrackSelection(state: TrackSelectionState, action: TrackSelectionAction): TrackSelectionState {
    val normalized = state.normalized()

    return when (action) {
        is TrackSelectionAction.Start -> {
            if (action.trackId.isBlank()) {
                normalized
            } else {
                TrackSelectionState(action.pageKey, setOf(action.trackId))
            }
        }

        is TrackSelectionAction.Select -> {
            if (action.trackId.isBlank() || normalized.pageKey != action.pageKey) {
                normalized
            } else {
                normalized.copy(selectedTrackIds = normalized.selectedTrackIds + action.trackId)
            }
        }

        is TrackSelectionAction.Toggle -> {
            if (action.trackId.isBlank()) {
                normalized
            } else if (normalized.pageKey == null) {
                TrackSelectionState(action.pageKey, setOf(action.trackId))
            } else if (normalized.pageKey != action.pageKey) {
                normalized
            } else {
                normalized.copy(
                    selectedTrackIds = if (action.trackId in normalized.selectedTrackIds) {
                        normalized.selectedTrackIds - action.trackId
                    } else {
                        normalized.selectedTrackIds + action.trackId
                    },
                ).normalized()
            }
        }

        is TrackSelectionAction.ReconcileVisible -> {
            if (normalized.pageKey != action.pageKey) {
                normalized
            } else {
                normalized.copy(
                    selectedTrackIds = normalized.selectedTrackIds
                        .asSequence()
                        .filter(String::isNotBlank)
                        .filter { it in action.visibleTrackIds }
                        .toSet(),
                ).normalized()
            }
        }

        is TrackSelectionAction.RouteChanged -> TrackSelectionState()

        TrackSelectionAction.Cancel,
        TrackSelectionAction.Completed,
        -> TrackSelectionState()
    }
}

fun orderedSelectedTrackIds(
    state: TrackSelectionState,
    pageKey: TrackSelectionPageKey,
    visibleTrackIds: List<String>,
): List<String> {
    if (state.pageKey != pageKey) {
        return emptyList()
    }

    val selected = state.selectedTrackIds
    return buildList {
        visibleTrackIds.forEach { trackId ->
            if (trackId.isNotBlank() && trackId in selected && trackId !in this) {
                add(trackId)
            }
        }
    }
}

private fun TrackSelectionState.normalized(): TrackSelectionState = if (selectedTrackIds.isEmpty()) TrackSelectionState() else this
