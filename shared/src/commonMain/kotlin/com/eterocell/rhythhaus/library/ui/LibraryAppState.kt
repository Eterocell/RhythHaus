package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eterocell.rhythhaus.LibrarySnapshot

class LibraryAppState(
    initialSelectedTrackId: String?,
) {
    private var selectedTrackIdState by mutableStateOf(initialSelectedTrackId)
    val selectedTrackId: String?
        get() = selectedTrackIdState

    private var browseModeState by mutableStateOf(BrowseMode.Albums)
    val browseMode: BrowseMode
        get() = browseModeState
    var showNowPlaying by mutableStateOf(false)
        private set
    var isNowPlayingBarVisible by mutableStateOf(true)
        private set
    var navigation by mutableStateOf(LibraryNavigationStack())
        private set
    var lastNavigationTransition by mutableStateOf(LibraryNavigationTransition.None)
        private set

    private var bottomBarVisibilityState by mutableStateOf(LibraryBottomBarVisibilityState())

    fun setSelectedTrackId(trackId: String?) {
        selectedTrackIdState = trackId
    }

    fun syncSelectedTrackWithPlayback(playbackTrackId: String?) {
        selectedTrackIdState = selectedTrackIdForPlaybackChange(selectedTrackId, playbackTrackId)
    }

    fun setBrowseMode(mode: BrowseMode) {
        browseModeState = mode
    }

    fun showNowPlaying() {
        showNowPlaying = true
    }

    fun hideNowPlaying() {
        showNowPlaying = false
    }

    fun pushRoute(route: LibraryRoute) {
        applyNavigation(LibraryNavigationAction.Push(route))
    }

    fun replaceTopRoute(route: LibraryRoute) {
        applyNavigation(LibraryNavigationAction.ReplaceTop(route))
    }

    fun popRoute() {
        applyNavigation(LibraryNavigationAction.Pop)
    }

    fun popToRoot() {
        applyNavigation(LibraryNavigationAction.PopToRoot)
    }

    fun openDetailRoute(route: LibraryRoute, adaptiveLayoutMode: LibraryAdaptiveLayoutMode) {
        if (shouldReplaceWideDetailRoute(adaptiveLayoutMode, navigation.current, route)) {
            replaceTopRoute(route)
        } else {
            pushRoute(route)
        }
    }

    fun completePredictivePop(next: LibraryNavigationStack) {
        lastNavigationTransition = LibraryNavigationTransition.None
        navigation = next
    }

    fun updateNowPlayingBarVisibilityForScroll(currentPosition: LibraryScrollPosition) {
        bottomBarVisibilityState = updateBottomBarVisibilityForScroll(bottomBarVisibilityState, currentPosition)
        isNowPlayingBarVisible = bottomBarVisibilityState.visible
    }

    private fun applyNavigation(action: LibraryNavigationAction) {
        lastNavigationTransition = transitionForNavigationAction(navigation, action)
        navigation = applyNavigationAction(navigation, action)
    }
}

@Composable
fun rememberLibraryAppState(
    snapshot: LibrarySnapshot,
): LibraryAppState = remember(snapshot.nowPlayingTrackId) {
    LibraryAppState(initialSelectedTrackId = snapshot.nowPlayingTrackId)
}
