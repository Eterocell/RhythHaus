package com.eterocell.rhythhaus

sealed interface LibraryRoute {
    data object Home : LibraryRoute
    data class AlbumDetail(val album: String) : LibraryRoute
    data class ArtistDetail(val artist: String) : LibraryRoute
    data object NowPlaying : LibraryRoute
    data object Search : LibraryRoute
    data object Settings : LibraryRoute
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

fun routeRequiresInWindowContentAnimation(route: LibraryRoute): Boolean = route == LibraryRoute.ClearLibraryDialog

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

data class LibraryScrollPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
)

data class NestedScrollChromeState(
    val progress: Float,
    val headerOffsetPx: Float,
)

fun nestedScrollChromeStateFor(
    position: LibraryScrollPosition,
    activationDistancePx: Float = 96f,
    maxHeaderOffsetPx: Float = 0f,
): NestedScrollChromeState {
    val progress = when {
        position.firstVisibleItemIndex > 0 -> 1f
        activationDistancePx <= 0f -> 1f
        else -> (position.firstVisibleItemScrollOffset / activationDistancePx).coerceIn(0f, 1f)
    }
    return NestedScrollChromeState(
        progress = progress,
        headerOffsetPx = if (progress == 0f) 0f else -maxHeaderOffsetPx * progress,
    )
}

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
