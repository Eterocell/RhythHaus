package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import com.eterocell.rhythhaus.LibrarySnapshot

internal class LibraryAppState(
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

    private var nowPlayingAppearanceToken by mutableStateOf(0L)

    var isNowPlayingBarVisible by mutableStateOf(true)
        private set

    var navigation by mutableStateOf(LibraryNavigationStack())
        private set

    var lastNavigationTransition by
        mutableStateOf(LibraryNavigationTransition.None)
        private set

    private var bottomBarVisibilityState by
        mutableStateOf(LibraryBottomBarVisibilityState())

    val activeDestinationId: LibraryDestinationId
        get() = navigation.currentEntry.destinationId

    private var acceptedBackSurface: RegisteredBackSurface? by
        mutableStateOf(null)
    private var nextBackSurfaceRegistrationToken = 0L
    private var activeSelectionPort: LibraryBackSelectionPort? by
        mutableStateOf(null)

    var pendingBackSession: LibraryBackSession? by mutableStateOf(null)
        private set

    /**
     * Accepts one feature-owned foremost action for the currently presented
     * destination only. A disposer may clear only the registration that created
     * it.
     */
    internal fun registerBackSurface(port: LibraryBackSurfacePort): () -> Unit {
        val target = port.foremostFeatureTarget
        if (port.destinationId != activeDestinationId ||
            target?.id?.destinationId != null &&
                target.id.destinationId != port.destinationId) {
            return {}
        }
        val registration =
            RegisteredBackSurface(
                token = ++nextBackSurfaceRegistrationToken,
                port = port,
                target = target,
            )
        acceptedBackSurface = registration
        reconcileBackSession()
        return {
            if (acceptedBackSurface === registration) {
                acceptedBackSurface = null
                reconcileBackSession()
            }
        }
    }

    /**
     * The shell publishes the current page's selection capability. The state
     * object never owns selection data or captures a historical cancellation
     * callback in a Back session.
     */
    internal fun publishSelectionPort(port: LibraryBackSelectionPort?) {
        val authoritativePort = port?.takeIf(::isSelectionPortAuthoritative)
        if (activeSelectionPort != authoritativePort) {
            activeSelectionPort = authoritativePort
            reconcileBackSession()
        }
    }

    internal fun beginBack(
        selectionPort: LibraryBackSelectionPort? = null
    ): LibraryBackBeginResult {
        if (selectionPort != null) publishSelectionPort(selectionPort)
        if (pendingBackSession != null) return LibraryBackBeginResult.Suppressed
        val destination = activeDestinationId
        val surface =
            acceptedBackSurface
                ?.takeIf {
                    it.port.destinationId == destination
                }
                ?.port
        val resolution =
            resolveLibraryBack(
                LibraryBackResolutionInput(
                    activeDestinationId = destination,
                    backSurfacePorts = listOfNotNull(surface),
                    browseMode = browseMode,
                    isNowPlayingExpanded = showNowPlaying,
                    navigation = navigation,
                    selectionPort = selectionPort ?: activeSelectionPort,
                    nowPlayingTargetId =
                        LibraryBackTargetId(
                            destination,
                            "now-playing-$nowPlayingAppearanceToken"),
                ),
            )
        val target =
            (resolution as? LibraryBackResolution.Started)?.target
                ?: return LibraryBackBeginResult.Unhandled
        val session =
            LibraryBackSession(
                target = target,
                routePreview =
                    (target as? LibraryBackTarget.Route)?.routePreview,
                completeAction = { completeBackSession(it) },
                cancelAction = { cancelBackSession(it) },
                rejectAction = { rejectBackSession(it) },
            )
        pendingBackSession = session
        return LibraryBackBeginResult.Started(session)
    }

    internal fun canBeginBack(
        selectionPort: LibraryBackSelectionPort? = null
    ): Boolean {
        val destination = activeDestinationId
        val surface =
            acceptedBackSurface
                ?.takeIf { it.port.destinationId == destination }
                ?.port
        return resolveLibraryBack(
            LibraryBackResolutionInput(
                activeDestinationId = destination,
                backSurfacePorts = listOfNotNull(surface),
                browseMode = browseMode,
                isNowPlayingExpanded = showNowPlaying,
                navigation = navigation,
                selectionPort = selectionPort ?: activeSelectionPort,
                nowPlayingTargetId =
                    LibraryBackTargetId(
                        destination, "now-playing-$nowPlayingAppearanceToken"),
            ),
        ) is
            LibraryBackResolution.Started
    }

    /**
     * Clears suppression only when the exact latched target is no longer
     * authoritative.
     */
    internal fun reconcileBackSession(
        selectionPort: LibraryBackSelectionPort? = null
    ) {
        val session = pendingBackSession ?: return
        if (!isTargetAuthoritative(
            session.target, selectionPort ?: activeSelectionPort)) {
            pendingBackSession = null
        }
    }

    fun setSelectedTrackId(trackId: String?) {
        selectedTrackIdState = trackId
    }

    fun syncSelectedTrackWithPlayback(playbackTrackId: String?) {
        selectedTrackIdState =
            selectedTrackIdForPlaybackChange(selectedTrackId, playbackTrackId)
    }

    fun setBrowseMode(mode: BrowseMode) {
        browseModeState = mode
        if (activeSelectionPort?.let(::isSelectionPortAuthoritative) != true) {
            activeSelectionPort = null
        }
        reconcileBackSession()
    }

    internal fun recoverStalePlaylistDetail(
        message: String,
        showMessage: (String) -> Unit
    ) {
        showMessage(message)
        replaceTopRoute(LibraryRoute.PlaylistHub)
    }

    fun showNowPlaying() {
        if (!showNowPlaying) nowPlayingAppearanceToken += 1
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

    /**
     * Applies an authoritative successful deletion only to the exact
     * playlist-detail entry that initiated it. This is destination
     * invalidation, never a Back request or session dispatch.
     */
    internal fun completeDisplayedPlaylistDeletion(
        confirmedSnapshot: PlaylistSnapshot,
        playlistId: String,
        origin: LibraryNavigationEntry,
    ) {
        if (confirmedSnapshot.playlist(playlistId) != null ||
            navigation.currentEntry != origin ||
            origin.route != LibraryRoute.PlaylistDetail(playlistId)) {
            return
        }
        val invalidatedDestination = origin.destinationId
        if (acceptedBackSurface?.port?.destinationId ==
            invalidatedDestination) {
            acceptedBackSurface = null
        }
        if (activeSelectionPort?.destinationId == invalidatedDestination) {
            activeSelectionPort = null
        }
        if (pendingBackSession?.target?.id?.destinationId ==
            invalidatedDestination) {
            pendingBackSession = null
        }
        val predecessor =
            navigation.entries.getOrNull(navigation.entries.lastIndex - 1)
        applyNavigation(
            if (predecessor?.route == LibraryRoute.PlaylistHub) {
                LibraryNavigationAction.Pop
            } else {
                LibraryNavigationAction.ReplaceTop(LibraryRoute.PlaylistHub)
            },
        )
    }

    fun openDetailRoute(
        route: LibraryRoute,
        adaptiveLayoutMode: LibraryAdaptiveLayoutMode
    ) {
        if (shouldReplaceWideDetailRoute(
            adaptiveLayoutMode, navigation.current, route)) {
            replaceTopRoute(route)
        } else {
            pushRoute(route)
        }
    }

    fun completePredictivePop(next: LibraryNavigationStack) {
        lastNavigationTransition = LibraryNavigationTransition.None
        navigation = next
        reconcileBackSession()
    }

    fun updateNowPlayingBarVisibilityForScroll(
        currentPosition: LibraryScrollPosition
    ) {
        bottomBarVisibilityState =
            updateBottomBarVisibilityForScroll(
                bottomBarVisibilityState, currentPosition)
        isNowPlayingBarVisible = bottomBarVisibilityState.visible
    }

    private fun applyNavigation(action: LibraryNavigationAction) {
        lastNavigationTransition =
            transitionForNavigationAction(navigation, action)
        navigation = applyNavigationAction(navigation, action)
        reconcileBackSession()
    }

    private fun completeBackSession(session: LibraryBackSession) {
        val selectionPort = activeSelectionPort
        if (pendingBackSession !== session || !session.markCompleted()) return
        if (!isTargetAuthoritative(session.target, selectionPort)) {
            pendingBackSession = null
            return
        }
        when (val target = session.target) {
            is LibraryBackTarget.FeatureModal,
            is LibraryBackTarget.FeatureEdit,
            ->
                when (acceptedBackSurface?.port?.dispatch?.invoke(target)) {
                    LibraryBackFeatureRequestResult.Started -> Unit
                    else -> rejectBackSession(session)
                }

            is LibraryBackTarget.PageSelection ->
                selectionPort?.cancel?.invoke()
            is LibraryBackTarget.NowPlaying -> {
                hideNowPlaying()
                reconcileBackSession()
            }
            is LibraryBackTarget.Route -> {
                if (navigation.currentEntry ==
                    target.routePreview.outgoingEntry) {
                    completePredictivePop(target.routePreview.nextNavigation)
                } else {
                    pendingBackSession = null
                }
            }
        }
    }

    private fun cancelBackSession(session: LibraryBackSession) {
        if (pendingBackSession === session && !session.isCompleted) {
            pendingBackSession = null
        }
    }

    private fun rejectBackSession(session: LibraryBackSession) {
        if (pendingBackSession === session) pendingBackSession = null
    }

    private fun isTargetAuthoritative(
        target: LibraryBackTarget,
        selectionPort: LibraryBackSelectionPort?,
    ): Boolean {
        val destination = activeDestinationId
        if (target.id.destinationId != destination) return false
        return when (target) {
            is LibraryBackTarget.FeatureModal,
            is LibraryBackTarget.FeatureEdit,
            ->
                acceptedBackSurface?.let { registration ->
                    registration.port.destinationId == destination &&
                        registration.target == target
                } == true

            is LibraryBackTarget.PageSelection ->
                selectionPort?.destinationId == destination &&
                    selectionPort.target == target &&
                    target.pageKey ==
                        trackSelectionPageKeyFor(navigation.current, browseMode)

            is LibraryBackTarget.NowPlaying ->
                showNowPlaying &&
                    target.id.instanceToken ==
                        "now-playing-$nowPlayingAppearanceToken"
            is LibraryBackTarget.Route ->
                navigation.canPop &&
                    target.routePreview.outgoingEntry == navigation.currentEntry
        }
    }

    private fun isSelectionPortAuthoritative(
        port: LibraryBackSelectionPort
    ): Boolean =
        port.destinationId == activeDestinationId &&
            port.target.id.destinationId == activeDestinationId &&
            port.target.pageKey ==
                trackSelectionPageKeyFor(navigation.current, browseMode)

    private data class RegisteredBackSurface(
        val token: Long,
        val port: LibraryBackSurfacePort,
        /**
         * The exact destination-scoped presentation identity this effect owns.
         */
        val target: LibraryBackTarget?,
    )
}

/**
 * Keeps one begin result for an actual predictive gesture. A system completion
 * without a preceding predictive begin is an ordinary Back request, not a
 * fallback for a latched gesture.
 */
internal class LibraryBackGestureLifecycle {
    private var predictiveBeginResult: LibraryBackBeginResult? = null

    fun beginPredictive(
        begin: () -> LibraryBackBeginResult
    ): LibraryBackBeginResult {
        if (predictiveBeginResult == null) predictiveBeginResult = begin()
        return checkNotNull(predictiveBeginResult)
    }

    fun latchedResult(): LibraryBackBeginResult? = predictiveBeginResult

    fun cancelPredictive() {
        (predictiveBeginResult as? LibraryBackBeginResult.Started)
            ?.session
            ?.cancel()
        predictiveBeginResult = null
    }

    fun completeSystemBack(beginOrdinary: () -> LibraryBackBeginResult) {
        val result = predictiveBeginResult ?: beginOrdinary()
        (result as? LibraryBackBeginResult.Started)?.session?.complete()
        predictiveBeginResult = null
    }
}

/**
 * The one core navigation-event handler for the Library subtree. It latches
 * synchronously in the dispatcher callback, before any Compose effect can
 * observe transition state.
 */
internal class LibraryNavigationEventBackHandler(
    private val dispatcher: NavigationEventDispatcher,
    private var beginBack: () -> LibraryBackBeginResult,
) :
    NavigationEventHandler<NavigationEventInfo>(
        NavigationEventInfo.None, isBackEnabled = true) {
    private val lifecycle = LibraryBackGestureLifecycle()

    var predictiveProgress by mutableFloatStateOf(0f)
        private set

    init {
        dispatcher.addHandler(this)
    }

    fun update(beginBack: () -> LibraryBackBeginResult, enabled: Boolean) {
        this.beginBack = beginBack
        isBackEnabled = enabled
    }

    override fun onBackStarted(event: NavigationEvent) {
        lifecycle.beginPredictive(beginBack)
        predictiveProgress = routeProgress(event)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        predictiveProgress = routeProgress(event)
    }

    override fun onBackCompleted() {
        lifecycle.completeSystemBack(beginBack)
        predictiveProgress = 0f
    }

    override fun onBackCancelled() {
        lifecycle.cancelPredictive()
        predictiveProgress = 0f
    }

    fun routePreview(): LibraryRoutePreview? =
        (lifecycle.latchedResult() as? LibraryBackBeginResult.Started)
            ?.session
            ?.routePreview

    fun dispose() = remove()

    private fun routeProgress(event: NavigationEvent): Float =
        if (routePreview() != null) event.progress else 0f
}

@Composable
internal fun rememberLibraryNavigationEventBackHandler(
    dispatcher: NavigationEventDispatcher,
    beginBack: () -> LibraryBackBeginResult,
    enabled: Boolean,
): LibraryNavigationEventBackHandler {
    val handler =
        remember(dispatcher) {
            LibraryNavigationEventBackHandler(dispatcher, beginBack)
        }
    handler.update(beginBack, enabled)
    DisposableEffect(handler) { onDispose(handler::dispose) }
    return handler
}

internal sealed interface LibraryBackBeginResult {
    data object Unhandled : LibraryBackBeginResult

    data object Suppressed : LibraryBackBeginResult

    data class Started(val session: LibraryBackSession) : LibraryBackBeginResult
}

internal enum class LibraryBackAdapterResult {
    Handled,
    Suppressed,
    Unhandled,
}

/**
 * Thin common protocol for ordinary and system Back adapters. A platform
 * adapter retains its own unhandled default while every handled request begins
 * and completes the same latched session.
 */
internal fun performLibraryBack(
    state: LibraryAppState,
    selectionPort: LibraryBackSelectionPort?,
    onUnhandled: () -> Unit,
): LibraryBackAdapterResult =
    when (val result = state.beginBack(selectionPort)) {
        is LibraryBackBeginResult.Started -> {
            result.session.complete()
            LibraryBackAdapterResult.Handled
        }

        LibraryBackBeginResult.Suppressed -> LibraryBackAdapterResult.Suppressed
        LibraryBackBeginResult.Unhandled -> {
            onUnhandled()
            LibraryBackAdapterResult.Unhandled
        }
    }

internal class LibraryBackSession
internal constructor(
    val target: LibraryBackTarget,
    val routePreview: LibraryRoutePreview?,
    private val completeAction: (LibraryBackSession) -> Unit,
    private val cancelAction: (LibraryBackSession) -> Unit,
    private val rejectAction: (LibraryBackSession) -> Unit,
) {
    internal var isCompleted: Boolean = false
        private set

    internal fun markCompleted(): Boolean =
        !isCompleted.also { isCompleted = true }

    fun complete() = completeAction(this)

    fun cancel() = cancelAction(this)

    fun reject() = rejectAction(this)
}

@Composable
internal fun rememberLibraryAppState(
    snapshot: LibrarySnapshot,
): LibraryAppState =
    remember(snapshot.nowPlayingTrackId) {
        LibraryAppState(initialSelectedTrackId = snapshot.nowPlayingTrackId)
    }
