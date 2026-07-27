package com.eterocell.rhythhaus.library.ui

import kotlin.math.roundToInt

sealed interface LibraryRoute {
    data object Home : LibraryRoute

    data class AlbumDetail(val album: String) : LibraryRoute

    data class ArtistDetail(val artist: String) : LibraryRoute

    data object NowPlaying : LibraryRoute

    data object Search : LibraryRoute

    data object PlaylistHub : LibraryRoute

    data class PlaylistDetail(val playlistId: String) : LibraryRoute

    data object Settings : LibraryRoute

    data object SettingsAbout : LibraryRoute

    data object OpenSourceLibraries : LibraryRoute

    data object ClearLibraryDialog : LibraryRoute
}

internal enum class LibraryNavigationTransition {
    None,
    Push,
    Pop,
    Replace,
    Root,
}

internal fun classifyNavigationTransition(
    from: LibraryNavigationStack,
    to: LibraryNavigationStack,
): LibraryNavigationTransition =
    when {
        from.entries == to.entries -> LibraryNavigationTransition.None
        to.current == LibraryRoute.Home && from.current != LibraryRoute.Home ->
            LibraryNavigationTransition.Root
        to.routes.size > from.routes.size -> LibraryNavigationTransition.Push
        to.routes.size < from.routes.size -> LibraryNavigationTransition.Pop
        from.current != to.current -> LibraryNavigationTransition.Replace
        else -> LibraryNavigationTransition.None
    }

fun routeRequiresInWindowContentAnimation(route: LibraryRoute): Boolean = false

fun routePermitsNowPlayingBar(route: LibraryRoute): Boolean =
    when (route) {
        LibraryRoute.Settings,
        LibraryRoute.SettingsAbout,
        LibraryRoute.OpenSourceLibraries,
        -> false

        LibraryRoute.Home,
        is LibraryRoute.AlbumDetail,
        is LibraryRoute.ArtistDetail,
        LibraryRoute.NowPlaying,
        LibraryRoute.Search,
        LibraryRoute.PlaylistHub,
        is LibraryRoute.PlaylistDetail,
        LibraryRoute.ClearLibraryDialog,
        -> true
    }

fun shouldShowNowPlayingBar(
    route: LibraryRoute,
    existingVisibility: Boolean,
): Boolean = existingVisibility && routePermitsNowPlayingBar(route)

sealed interface LibraryBottomBarContent {
    data object Hidden : LibraryBottomBarContent

    data object NowPlaying : LibraryBottomBarContent

    data class Selection(val selectedCount: Int) : LibraryBottomBarContent
}

fun libraryBottomBarContent(
    route: LibraryRoute,
    selectionState: TrackSelectionState,
    isNowPlayingVisible: Boolean,
): LibraryBottomBarContent {
    val selectionMatchesVisibleSurface =
        when (val pageKey = selectionState.pageKey) {
            TrackSelectionPageKey.HomeSongs ->
                route == LibraryRoute.Home ||
                    route is LibraryRoute.AlbumDetail ||
                    route is LibraryRoute.ArtistDetail ||
                    route == LibraryRoute.PlaylistHub ||
                    route is LibraryRoute.PlaylistDetail

            is TrackSelectionPageKey.Album ->
                route == LibraryRoute.AlbumDetail(pageKey.album)

            is TrackSelectionPageKey.Artist ->
                route == LibraryRoute.ArtistDetail(pageKey.artist)

            TrackSelectionPageKey.Search -> route == LibraryRoute.Search

            null -> false
        }
    return when {
        selectionMatchesVisibleSurface &&
            selectionState.selectedTrackIds.isNotEmpty() &&
            routePermitsNowPlayingBar(route) ->
            LibraryBottomBarContent.Selection(
                selectionState.selectedTrackIds.size)

        shouldShowNowPlayingBar(route, isNowPlayingVisible) ->
            LibraryBottomBarContent.NowPlaying

        else -> LibraryBottomBarContent.Hidden
    }
}

data class LibraryBottomBarMeasurement(
    val content: LibraryBottomBarContent,
    val heightPx: Int,
)

fun activeBottomBarClearancePx(
    content: LibraryBottomBarContent,
    measurement: LibraryBottomBarMeasurement?,
): Int =
    if (content == LibraryBottomBarContent.Hidden ||
        measurement?.content != content) {
        0
    } else {
        measurement.heightPx.coerceAtLeast(0)
    }

fun activeBottomBarAlpha(
    content: LibraryBottomBarContent,
    measurement: LibraryBottomBarMeasurement?,
    hiddenFraction: Float,
): Float =
    if (activeBottomBarClearancePx(content, measurement) == 0) {
        0f
    } else {
        1f - hiddenFraction.coerceIn(0f, 1f)
    }

data class LibraryBottomBarPresentation(
    val clearancePx: Int,
    val alpha: Float,
    val isInteractive: Boolean,
)

fun libraryBottomBarPresentation(
    content: LibraryBottomBarContent,
    measurement: LibraryBottomBarMeasurement?,
    hiddenFraction: Float,
): LibraryBottomBarPresentation {
    val clearancePx = activeBottomBarClearancePx(content, measurement)
    val alpha = activeBottomBarAlpha(content, measurement, hiddenFraction)
    return LibraryBottomBarPresentation(
        clearancePx = clearancePx,
        alpha = alpha,
        isInteractive = clearancePx > 0 && hiddenFraction < 1f,
    )
}

fun trackSelectionPageKeyFor(
    route: LibraryRoute,
    browseMode: BrowseMode
): TrackSelectionPageKey? =
    when (route) {
        LibraryRoute.Home ->
            TrackSelectionPageKey.HomeSongs.takeIf {
                browseMode == BrowseMode.Songs
            }
        is LibraryRoute.AlbumDetail -> TrackSelectionPageKey.Album(route.album)
        is LibraryRoute.ArtistDetail ->
            TrackSelectionPageKey.Artist(route.artist)
        LibraryRoute.Search -> TrackSelectionPageKey.Search
        else -> null
    }

/**
 * The presented route plus its shell-created instance token. Route equality alone is not
 * sufficient because an equal route can replace an outgoing destination.
 */
internal data class LibraryDestinationId(
    val route: LibraryRoute,
    val instanceToken: String,
)

/** A concrete Back-capable target within one presented destination instance. */
internal data class LibraryBackTargetId(
    val destinationId: LibraryDestinationId,
    val instanceToken: String,
)

internal data class LibraryRoutePreview(
    val outgoingEntry: LibraryNavigationEntry,
    val incomingEntry: LibraryNavigationEntry,
    val nextNavigation: LibraryNavigationStack,
    val transition: LibraryNavigationTransition,
)

internal sealed interface LibraryBackTarget {
    val id: LibraryBackTargetId

    data class FeatureModal(
        override val id: LibraryBackTargetId,
    ) : LibraryBackTarget

    data class FeatureEdit(
        override val id: LibraryBackTargetId,
    ) : LibraryBackTarget

    data class PageSelection(
        override val id: LibraryBackTargetId,
        val pageKey: TrackSelectionPageKey,
    ) : LibraryBackTarget

    data class NowPlaying(
        override val id: LibraryBackTargetId,
    ) : LibraryBackTarget

    data class Route(
        override val id: LibraryBackTargetId,
        val routePreview: LibraryRoutePreview,
    ) : LibraryBackTarget
}

/**
 * A feature publishes only its own already-chosen foremost action. The state module does not
 * inspect or order feature modal internals.
 */
internal data class LibraryBackSurfacePort(
    val destinationId: LibraryDestinationId,
    val foremostFeatureTarget: LibraryBackTarget?,
    val dispatch: (LibraryBackTarget) -> LibraryBackFeatureRequestResult = {
        LibraryBackFeatureRequestResult.Rejected
    },
)

/** The feature, rather than a Back adapter, authoritatively accepts or rejects its request. */
internal enum class LibraryBackFeatureRequestResult { Started, Rejected }

/** Shell-owned selection state is published as a capability; the state module never owns it. */
internal data class LibraryBackSelectionPort(
    val destinationId: LibraryDestinationId,
    val target: LibraryBackTarget.PageSelection,
    val cancel: () -> Unit,
)

internal data class LibraryBackResolutionInput(
    val activeDestinationId: LibraryDestinationId,
    val backSurfacePorts: List<LibraryBackSurfacePort>,
    val browseMode: BrowseMode,
    val isNowPlayingExpanded: Boolean,
    val navigation: LibraryNavigationStack,
    val isBackSessionPending: Boolean = false,
    val nowPlayingTargetId: LibraryBackTargetId =
        LibraryBackTargetId(activeDestinationId, "now-playing"),
    val routeTargetId: LibraryBackTargetId =
        LibraryBackTargetId(activeDestinationId, "route"),
    val selectionPort: LibraryBackSelectionPort? = null,
)

internal sealed interface LibraryBackResolution {
    data object Unhandled : LibraryBackResolution

    data object Suppressed : LibraryBackResolution

    data class Started(
        val target: LibraryBackTarget,
    ) : LibraryBackResolution
}

/** Resolves only the active destination from authoritative inputs; it performs no mutation. */
internal fun resolveLibraryBack(input: LibraryBackResolutionInput): LibraryBackResolution {
    if (input.isBackSessionPending) return LibraryBackResolution.Suppressed

    val activeDestination = input.activeDestinationId
    val featureTarget =
        input.backSurfacePorts
            .singleOrNull { it.destinationId == activeDestination }
            ?.foremostFeatureTarget
            ?.takeIf { it.id.destinationId == activeDestination }
    if (featureTarget is LibraryBackTarget.FeatureModal ||
        featureTarget is LibraryBackTarget.FeatureEdit) {
        return LibraryBackResolution.Started(featureTarget)
    }

    val selectionTarget = input.selectionPort?.target
    if (selectionTarget != null && input.selectionPort.destinationId == activeDestination &&
        selectionTarget.id.destinationId == activeDestination &&
        selectionTarget.pageKey == trackSelectionPageKeyFor(activeDestination.route, input.browseMode)) {
        return LibraryBackResolution.Started(
            selectionTarget,
        )
    }

    if (input.isNowPlayingExpanded &&
        input.nowPlayingTargetId.destinationId == activeDestination) {
        return LibraryBackResolution.Started(
            LibraryBackTarget.NowPlaying(input.nowPlayingTargetId))
    }

    if (input.navigation.canPop && input.routeTargetId.destinationId == activeDestination) {
        val routePreview =
            LibraryRoutePreview(
                outgoingEntry = input.navigation.currentEntry,
                incomingEntry = input.navigation.entries[input.navigation.entries.lastIndex - 1],
                nextNavigation = input.navigation.pop(),
                transition =
                    transitionForNavigationAction(
                        input.navigation, LibraryNavigationAction.Pop),
            )
        return LibraryBackResolution.Started(
            LibraryBackTarget.Route(
                LibraryBackTargetId(
                    activeDestination,
                    "route-${routePreview.outgoingEntry.destinationId.instanceToken}-${routePreview.incomingEntry.destinationId.instanceToken}",
                ),
                routePreview,
            ))
    }

    return LibraryBackResolution.Unhandled
}

fun nowPlayingBarOffsetPx(hiddenFraction: Float, measuredHeightPx: Int): Int =
    (hiddenFraction.coerceIn(0f, 1f) * measuredHeightPx).roundToInt()

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
    if (widthDp >= 600f && widthDp > 0f && heightDp / widthDp < 1.2f)
        return LibraryAdaptiveLayoutMode.ListDetail
    return LibraryAdaptiveLayoutMode.Compact
}

fun nowPlayingAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): NowPlayingAdaptiveLayoutMode =
    when (libraryAdaptiveLayoutModeFor(widthDp, heightDp)) {
        LibraryAdaptiveLayoutMode.Compact ->
            NowPlayingAdaptiveLayoutMode.Compact
        LibraryAdaptiveLayoutMode.ListDetail ->
            NowPlayingAdaptiveLayoutMode.Split
    }

internal fun libraryRouteRendersAsActiveOverlay(
    route: LibraryRoute,
    mode: LibraryAdaptiveLayoutMode,
): Boolean =
    when (mode) {
        LibraryAdaptiveLayoutMode.Compact,
        LibraryAdaptiveLayoutMode.ListDetail,
        ->
            when (route) {
                LibraryRoute.Search,
                LibraryRoute.Settings,
                LibraryRoute.SettingsAbout,
                LibraryRoute.OpenSourceLibraries,
                -> true

                LibraryRoute.Home,
                is LibraryRoute.AlbumDetail,
                is LibraryRoute.ArtistDetail,
                LibraryRoute.PlaylistHub,
                is LibraryRoute.PlaylistDetail,
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
    val indexDelta =
        current.firstVisibleItemIndex - previous.firstVisibleItemIndex
    if (indexDelta > 0) return false
    if (indexDelta < 0) return true

    val offsetDelta =
        current.firstVisibleItemScrollOffset -
            previous.firstVisibleItemScrollOffset
    if (offsetDelta > jitterThresholdPx) return false
    if (offsetDelta < -jitterThresholdPx) return true
    return currentlyVisible
}

internal data class LibraryNavigationEntry(
    val route: LibraryRoute,
    val destinationId: LibraryDestinationId =
        LibraryDestinationId(route, "navigation-${nextNavigationEntryToken()}"),
)

private var navigationEntryToken = 0L
private fun nextNavigationEntryToken(): Long = ++navigationEntryToken

internal data class LibraryNavigationStack(
    val entries: List<LibraryNavigationEntry> = listOf(LibraryNavigationEntry(LibraryRoute.Home)),
) {
    val routes: List<LibraryRoute> get() = entries.map(LibraryNavigationEntry::route)
    val currentEntry: LibraryNavigationEntry =
        entries.lastOrNull() ?: LibraryNavigationEntry(LibraryRoute.Home)
    val current: LibraryRoute get() = currentEntry.route
    val canPop: Boolean get() = entries.size > 1

    fun push(route: LibraryRoute): LibraryNavigationStack =
        when {
            route == LibraryRoute.Home -> popToRoot()
            route == current -> this
            else -> copy(entries = normalizedEntries(entries + LibraryNavigationEntry(route)))
        }

    fun replaceTop(route: LibraryRoute): LibraryNavigationStack =
        when {
            route == LibraryRoute.Home -> popToRoot()
            entries.size <= 1 -> push(route)
            else -> copy(entries = normalizedEntries(entries.dropLast(1) + LibraryNavigationEntry(route)))
        }

    fun pop(): LibraryNavigationStack =
        if (canPop) {
            copy(entries = entries.dropLast(1))
        } else {
            this
        }

    fun popToRoot(): LibraryNavigationStack =
        copy(entries = listOf(entries.firstOrNull()?.takeIf { it.route == LibraryRoute.Home }
            ?: LibraryNavigationEntry(LibraryRoute.Home)))

    private fun normalizedEntries(
        candidate: List<LibraryNavigationEntry>
    ): List<LibraryNavigationEntry> =
        when {
            candidate.isEmpty() -> listOf(LibraryNavigationEntry(LibraryRoute.Home))
            candidate.first().route != LibraryRoute.Home ->
                listOf(LibraryNavigationEntry(LibraryRoute.Home)) +
                    candidate.filterNot { it.route == LibraryRoute.Home }
            else -> candidate
        }
}

internal sealed interface LibraryNavigationAction {
    data class Push(val route: LibraryRoute) : LibraryNavigationAction

    data class ReplaceTop(val route: LibraryRoute) : LibraryNavigationAction

    data object Pop : LibraryNavigationAction

    data object PopToRoot : LibraryNavigationAction
}

fun shouldReplaceWideDetailRoute(
    mode: LibraryAdaptiveLayoutMode,
    current: LibraryRoute,
    next: LibraryRoute,
): Boolean =
    mode == LibraryAdaptiveLayoutMode.ListDetail &&
        current.isDetailRoute() &&
        next.isDetailRoute()

private fun LibraryRoute.isDetailRoute(): Boolean =
    this is LibraryRoute.AlbumDetail ||
        this is LibraryRoute.ArtistDetail ||
        this is LibraryRoute.PlaylistDetail

internal fun applyNavigationAction(
    stack: LibraryNavigationStack,
    action: LibraryNavigationAction,
): LibraryNavigationStack =
    when (action) {
        is LibraryNavigationAction.Push -> stack.push(action.route)
        is LibraryNavigationAction.ReplaceTop -> stack.replaceTop(action.route)
        LibraryNavigationAction.Pop -> stack.pop()
        LibraryNavigationAction.PopToRoot -> stack.popToRoot()
    }

internal fun transitionForNavigationAction(
    from: LibraryNavigationStack,
    action: LibraryNavigationAction,
): LibraryNavigationTransition {
    val to = applyNavigationAction(from, action)
    if (from.entries == to.entries) return LibraryNavigationTransition.None
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
    val previous =
        state.previousScrollPosition
            ?: return state.copy(previousScrollPosition = current)
    return LibraryBottomBarVisibilityState(
        visible =
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = state.visible,
            ),
        previousScrollPosition = current,
    )
}
