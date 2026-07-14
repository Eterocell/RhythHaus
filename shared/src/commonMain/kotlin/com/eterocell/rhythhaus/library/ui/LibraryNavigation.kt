package com.eterocell.rhythhaus.library.ui

sealed interface LibraryRoute {
    data object Home : LibraryRoute
    data class AlbumDetail(val album: String) : LibraryRoute
    data class ArtistDetail(val artist: String) : LibraryRoute
    data object NowPlaying : LibraryRoute
    data object Search : LibraryRoute
    data object Settings : LibraryRoute
    data object SettingsAbout : LibraryRoute
    data object OpenSourceLibraries : LibraryRoute
    data object ClearLibraryDialog : LibraryRoute
}

enum class LibraryNavigationTransition {
    None,
    Push,
    Pop,
    Replace,
    Root,
}

fun classifyNavigationTransition(
    from: LibraryNavigationStack,
    to: LibraryNavigationStack,
): LibraryNavigationTransition = when {
    from.routes == to.routes -> LibraryNavigationTransition.None
    to.current == LibraryRoute.Home && from.current != LibraryRoute.Home -> LibraryNavigationTransition.Root
    to.routes.size > from.routes.size -> LibraryNavigationTransition.Push
    to.routes.size < from.routes.size -> LibraryNavigationTransition.Pop
    from.current != to.current -> LibraryNavigationTransition.Replace
    else -> LibraryNavigationTransition.None
}

fun routeRequiresInWindowContentAnimation(route: LibraryRoute): Boolean = false

enum class LibraryAdaptiveLayoutMode {
    Compact,
    ListDetail,
}

enum class NowPlayingAdaptiveLayoutMode {
    Compact,
    Split,
}

fun libraryAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): LibraryAdaptiveLayoutMode {
    if (widthDp >= 840f) return LibraryAdaptiveLayoutMode.ListDetail
    if (widthDp >= 600f && widthDp > 0f && heightDp / widthDp < 1.2f) return LibraryAdaptiveLayoutMode.ListDetail
    return LibraryAdaptiveLayoutMode.Compact
}

fun nowPlayingAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): NowPlayingAdaptiveLayoutMode = when (libraryAdaptiveLayoutModeFor(widthDp, heightDp)) {
    LibraryAdaptiveLayoutMode.Compact -> NowPlayingAdaptiveLayoutMode.Compact
    LibraryAdaptiveLayoutMode.ListDetail -> NowPlayingAdaptiveLayoutMode.Split
}

internal fun libraryRouteRendersAsActiveOverlay(
    route: LibraryRoute,
    mode: LibraryAdaptiveLayoutMode,
): Boolean = when (mode) {
    LibraryAdaptiveLayoutMode.Compact,
    LibraryAdaptiveLayoutMode.ListDetail,
    -> when (route) {
        LibraryRoute.Search,
        LibraryRoute.Settings,
        LibraryRoute.SettingsAbout,
        LibraryRoute.OpenSourceLibraries,
        -> true

        LibraryRoute.Home,
        is LibraryRoute.AlbumDetail,
        is LibraryRoute.ArtistDetail,
        LibraryRoute.NowPlaying,
        LibraryRoute.ClearLibraryDialog,
        -> false
    }
}

data class LibraryScrollPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
)

fun decideNowPlayingBarVisibilityForLibraryScroll(
    previous: LibraryScrollPosition,
    current: LibraryScrollPosition,
    currentlyVisible: Boolean,
    jitterThresholdPx: Int = 2,
): Boolean {
    val indexDelta = current.firstVisibleItemIndex - previous.firstVisibleItemIndex
    if (indexDelta > 0) return false
    if (indexDelta < 0) return true

    val offsetDelta = current.firstVisibleItemScrollOffset - previous.firstVisibleItemScrollOffset
    if (offsetDelta > jitterThresholdPx) return false
    if (offsetDelta < -jitterThresholdPx) return true
    return currentlyVisible
}

data class LibraryNavigationStack(
    val routes: List<LibraryRoute> = listOf(LibraryRoute.Home),
) {
    val current: LibraryRoute = routes.lastOrNull() ?: LibraryRoute.Home
    val canPop: Boolean = routes.size > 1

    fun push(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        route == current -> this
        else -> copy(routes = normalizedRoutes(routes + route))
    }

    fun replaceTop(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        routes.size <= 1 -> push(route)
        else -> copy(routes = normalizedRoutes(routes.dropLast(1) + route))
    }

    fun pop(): LibraryNavigationStack = if (canPop) {
        copy(routes = routes.dropLast(1))
    } else {
        this
    }

    fun popToRoot(): LibraryNavigationStack = copy(routes = listOf(LibraryRoute.Home))

    private fun normalizedRoutes(candidate: List<LibraryRoute>): List<LibraryRoute> = when {
        candidate.isEmpty() -> listOf(LibraryRoute.Home)
        candidate.first() != LibraryRoute.Home -> listOf(LibraryRoute.Home) + candidate.filterNot { it == LibraryRoute.Home }
        else -> candidate
    }
}

sealed interface LibraryNavigationAction {
    data class Push(val route: LibraryRoute) : LibraryNavigationAction
    data class ReplaceTop(val route: LibraryRoute) : LibraryNavigationAction
    data object Pop : LibraryNavigationAction
    data object PopToRoot : LibraryNavigationAction
}

fun shouldReplaceWideDetailRoute(
    mode: LibraryAdaptiveLayoutMode,
    current: LibraryRoute,
    next: LibraryRoute,
): Boolean = mode == LibraryAdaptiveLayoutMode.ListDetail &&
    current.isDetailRoute() &&
    next.isDetailRoute()

private fun LibraryRoute.isDetailRoute(): Boolean = this is LibraryRoute.AlbumDetail || this is LibraryRoute.ArtistDetail

fun applyNavigationAction(
    stack: LibraryNavigationStack,
    action: LibraryNavigationAction,
): LibraryNavigationStack = when (action) {
    is LibraryNavigationAction.Push -> stack.push(action.route)
    is LibraryNavigationAction.ReplaceTop -> stack.replaceTop(action.route)
    LibraryNavigationAction.Pop -> stack.pop()
    LibraryNavigationAction.PopToRoot -> stack.popToRoot()
}

fun transitionForNavigationAction(
    from: LibraryNavigationStack,
    action: LibraryNavigationAction,
): LibraryNavigationTransition {
    val to = applyNavigationAction(from, action)
    if (from.routes == to.routes) return LibraryNavigationTransition.None
    return when (action) {
        is LibraryNavigationAction.Push,
        is LibraryNavigationAction.ReplaceTop,
        -> classifyNavigationTransition(from = from, to = to)
        LibraryNavigationAction.Pop -> LibraryNavigationTransition.Pop
        LibraryNavigationAction.PopToRoot -> LibraryNavigationTransition.Root
    }
}

fun selectedTrackIdForPlaybackChange(
    currentSelectedTrackId: String?,
    playbackTrackId: String?,
): String? = playbackTrackId ?: currentSelectedTrackId

data class LibraryBottomBarVisibilityState(
    val visible: Boolean = true,
    val previousScrollPosition: LibraryScrollPosition? = null,
)

fun updateBottomBarVisibilityForScroll(
    state: LibraryBottomBarVisibilityState,
    current: LibraryScrollPosition,
): LibraryBottomBarVisibilityState {
    val previous = state.previousScrollPosition ?: return state.copy(previousScrollPosition = current)
    return LibraryBottomBarVisibilityState(
        visible = decideNowPlayingBarVisibilityForLibraryScroll(
            previous = previous,
            current = current,
            currentlyVisible = state.visible,
        ),
        previousScrollPosition = current,
    )
}
