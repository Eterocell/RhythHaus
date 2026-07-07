# Architecture Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the shared Compose app architecture so `App.kt` becomes a thin app entry point while library orchestration and UI move into focused common-code files without changing behavior.

**Architecture:** Introduce tested pure orchestration helpers and a Compose-remembered common `LibraryAppState` coordinator before splitting the UI. Then move route shell, home/detail content, chrome/dialogs, and leaf components out of `App.kt` in small compile-verified slices.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix, NavigationEvent, kotlinx.coroutines, kotlin-test, Gradle configuration cache.

## Global Constraints

- Behavior-preserving only: no visual redesign, scanner rewrite, repository/database rewrite, playback engine rewrite, TagLib/source-access/artwork-cache redesign, platform-native navigation migration, dependency/toolchain changes, or Windows/Linux scope.
- Keep shared app architecture in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.
- Preserve current routes, route animations, predictive/system back behavior, adaptive thresholds, bottom-bar visibility behavior, Now Playing overlay behavior, clear-library behavior, scan/import behavior, playback queue/play-pause behavior, strings, content descriptions, Miuix/blur behavior, and platform seams.
- New pure decisions introduced by the refactor must have common tests where practical.
- App entry point after the refactor must construct/remember dependencies and hand them to a library shell/coordinator; it must not define main route rendering, home/detail list rendering, chrome implementation, dialogs, rows, or cards.
- Do not add dependencies; `shared/build.gradle.kts` already has lifecycle runtime/viewmodel Compose if later needed.

---

## File Structure

**Modify:**
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` — reduce to app dependency construction, theme collection, and call into the extracted library shell.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt` — add pure architecture helpers that belong with route/adaptive/scroll decisions.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt` — add tests for the new pure helpers and state actions.
- `openspec/changes/architecture-refactor/tasks.md` — mark tasks/evidence as work completes.
- `progress.md` — record final handoff and verification evidence.

**Create:**
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt` — common coordinator/state holder and remembered factory.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt` — root library screen shell, adaptive list/detail container, transition host, bottom bar, Now Playing overlay.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt` — route-content and overlay composition.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt` — home list, header, import card, browse picker, home album/artist/song sections.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt` — drill-down album/artist detail screen.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt` — nested-scroll top chrome, system bar top padding helper, scrollbar.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt` — clear-library dialog route.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt` — section label, track row, album mark, album card, artist row, scanning card.

---

### Task 1: Add tested pure architecture decisions

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: existing `LibraryRoute`, `LibraryNavigationStack`, `LibraryNavigationTransition`, `LibraryScrollPosition`, and `decideNowPlayingBarVisibilityForLibraryScroll(...)`.
- Produces:
  - `fun shouldReplaceWideDetailRoute(mode: LibraryAdaptiveLayoutMode, current: LibraryRoute, next: LibraryRoute): Boolean`
  - `fun applyNavigationAction(stack: LibraryNavigationStack, action: LibraryNavigationAction): LibraryNavigationStack`
  - `fun transitionForNavigationAction(from: LibraryNavigationStack, action: LibraryNavigationAction): LibraryNavigationTransition`
  - `fun selectedTrackIdForPlaybackChange(currentSelectedTrackId: String?, playbackTrackId: String?): String?`
  - `data class LibraryBottomBarVisibilityState(val visible: Boolean, val previousScrollPosition: LibraryScrollPosition?)`
  - `fun updateBottomBarVisibilityForScroll(state: LibraryBottomBarVisibilityState, current: LibraryScrollPosition): LibraryBottomBarVisibilityState`
  - `sealed interface LibraryNavigationAction`

- [ ] **Step 1: Write failing tests for pure helpers**

Add these tests to `LibraryNavigationTest.kt`:

```kotlin
    @Test
    fun wideDetailRouteReplacementOnlyAppliesBetweenDetailRoutesInListDetailMode() {
        assertTrue(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.ArtistDetail("B"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.Compact,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.ArtistDetail("B"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.Home,
                next = LibraryRoute.AlbumDetail("A"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.Search,
            ),
        )
    }

    @Test
    fun navigationActionsApplyExistingStackSemantics() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val pushed = applyNavigationAction(LibraryNavigationStack(), LibraryNavigationAction.Push(album))
        assertEquals(listOf(LibraryRoute.Home, album), pushed.routes)

        val replaced = applyNavigationAction(pushed, LibraryNavigationAction.ReplaceTop(LibraryRoute.ArtistDetail("Alice")))
        assertEquals(listOf(LibraryRoute.Home, LibraryRoute.ArtistDetail("Alice")), replaced.routes)

        assertEquals(LibraryRoute.Home, applyNavigationAction(replaced, LibraryNavigationAction.Pop).current)
        assertEquals(listOf(LibraryRoute.Home), applyNavigationAction(replaced, LibraryNavigationAction.PopToRoot).routes)
    }

    @Test
    fun navigationActionTransitionMatchesStackChange() {
        val from = LibraryNavigationStack().push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(
            LibraryNavigationTransition.Push,
            transitionForNavigationAction(from, LibraryNavigationAction.Push(LibraryRoute.Search)),
        )
        assertEquals(
            LibraryNavigationTransition.Pop,
            transitionForNavigationAction(from, LibraryNavigationAction.Pop),
        )
        assertEquals(
            LibraryNavigationTransition.Replace,
            transitionForNavigationAction(from, LibraryNavigationAction.ReplaceTop(LibraryRoute.ArtistDetail("Alice"))),
        )
        assertEquals(
            LibraryNavigationTransition.Root,
            transitionForNavigationAction(from, LibraryNavigationAction.PopToRoot),
        )
    }

    @Test
    fun playbackTrackSelectionOverridesOnlyWhenPlaybackHasTrack() {
        assertEquals("playing", selectedTrackIdForPlaybackChange("selected", "playing"))
        assertEquals("selected", selectedTrackIdForPlaybackChange("selected", null))
        assertEquals(null, selectedTrackIdForPlaybackChange(null, null))
    }

    @Test
    fun bottomBarVisibilityStateStoresPreviousScrollPosition() {
        val initial = LibraryBottomBarVisibilityState(visible = true, previousScrollPosition = null)
        val first = updateBottomBarVisibilityForScroll(
            state = initial,
            current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10),
        )
        assertTrue(first.visible)
        assertEquals(LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10), first.previousScrollPosition)

        val second = updateBottomBarVisibilityForScroll(
            state = first,
            current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30),
        )
        assertFalse(second.visible)
        assertEquals(LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30), second.previousScrollPosition)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL because the new helper symbols do not exist.

- [ ] **Step 3: Implement pure helpers**

Add to `LibraryNavigation.kt` below `LibraryNavigationStack` or near related helpers:

```kotlin
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
): LibraryNavigationTransition = classifyNavigationTransition(
    from = from,
    to = applyNavigationAction(from, action),
)

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
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 5: Commit task**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
git commit -m "test: cover library architecture decisions"
```

---

### Task 2: Introduce shared library app state/coordinator

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:304-408`, `667-782`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes Task 1 helpers.
- Produces `LibraryAppState`, `rememberLibraryAppState(snapshot: LibrarySnapshot, playbackState: PlaybackState): LibraryAppState`.

- [ ] **Step 1: Write coordinator tests for state actions**

Add to `LibraryNavigationTest.kt`:

```kotlin
    @Test
    fun libraryAppStateNavigationActionsRecordTransitions() {
        val state = LibraryAppState(initialSelectedTrackId = null)

        state.pushRoute(LibraryRoute.AlbumDetail("A"))
        assertEquals(LibraryRoute.AlbumDetail("A"), state.navigation.current)
        assertEquals(LibraryNavigationTransition.Push, state.lastNavigationTransition)

        state.replaceTopRoute(LibraryRoute.ArtistDetail("B"))
        assertEquals(LibraryRoute.ArtistDetail("B"), state.navigation.current)
        assertEquals(LibraryNavigationTransition.Replace, state.lastNavigationTransition)

        state.popRoute()
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertEquals(LibraryNavigationTransition.Pop, state.lastNavigationTransition)
    }

    @Test
    fun libraryAppStateTracksNowPlayingAndBottomBarVisibility() {
        val state = LibraryAppState(initialSelectedTrackId = "a")

        state.syncSelectedTrackWithPlayback("b")
        assertEquals("b", state.selectedTrackId)

        state.showNowPlaying()
        assertTrue(state.showNowPlaying)
        state.hideNowPlaying()
        assertFalse(state.showNowPlaying)

        state.updateNowPlayingBarVisibilityForScroll(LibraryScrollPosition(0, 10))
        assertTrue(state.isNowPlayingBarVisible)
        state.updateNowPlayingBarVisibilityForScroll(LibraryScrollPosition(0, 30))
        assertFalse(state.isNowPlayingBarVisible)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL because `LibraryAppState` does not exist.

- [ ] **Step 3: Add `LibraryAppState.kt`**

Create:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class LibraryAppState(
    initialSelectedTrackId: String?,
) {
    var selectedTrackId by mutableStateOf(initialSelectedTrackId)
        private set
    var browseMode by mutableStateOf(BrowseMode.Albums)
        private set
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
        selectedTrackId = trackId
    }

    fun syncSelectedTrackWithPlayback(playbackTrackId: String?) {
        selectedTrackId = selectedTrackIdForPlaybackChange(selectedTrackId, playbackTrackId)
    }

    fun setBrowseMode(mode: BrowseMode) {
        browseMode = mode
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
fun rememberLibraryAppState(snapshot: LibrarySnapshot): LibraryAppState = remember(snapshot.nowPlayingTrackId) {
    LibraryAppState(initialSelectedTrackId = snapshot.nowPlayingTrackId)
}
```

- [ ] **Step 4: Wire `LibraryHomeScreen(...)` to the coordinator without moving UI yet**

In `LibraryHomeScreen(...)`:

- Replace local `selectedTrackId`, `browseMode`, `showNowPlaying`, `isNowPlayingBarVisible`, `previousLibraryScrollPosition`, `navigation`, `lastNavigationTransition`, `updateNavigation`, `pushRoute`, `popRoute`, and `updateNowPlayingBarVisibilityForScroll` with `val appState = rememberLibraryAppState(snapshot)`.
- Keep `homeListState`, `expandProgress`, `screenHeightPx`, predictive-back offset, albums/artists, route content, and UI structure in place.
- Replace reads/calls as follows:
  - `selectedTrackId` -> `appState.selectedTrackId`
  - `selectedTrackId = track.id` -> `appState.setSelectedTrackId(track.id)`
  - `browseMode` -> `appState.browseMode`
  - `browseMode = it` -> `appState.setBrowseMode(it)`
  - `showNowPlaying` -> `appState.showNowPlaying`
  - `showNowPlaying = true` -> `appState.showNowPlaying()`
  - `showNowPlaying = false` -> `appState.hideNowPlaying()`
  - `isNowPlayingBarVisible` -> `appState.isNowPlayingBarVisible`
  - `navigation` -> `appState.navigation`
  - `lastNavigationTransition` -> `appState.lastNavigationTransition`
  - `pushRoute(route)` -> `appState.pushRoute(route)`
  - `popRoute()` -> `appState.popRoute()`
  - `updateNavigation(navigation.replaceTop(route))` -> `appState.replaceTopRoute(route)` or `appState.openDetailRoute(route, adaptiveLayoutMode)` where appropriate
  - `updateNowPlayingBarVisibilityForScroll(...)` -> `appState.updateNowPlayingBarVisibilityForScroll(...)`
  - predictive pop assignment should call `appState.completePredictivePop(next)`.

- [ ] **Step 5: Run focused tests and shared compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both PASS.

- [ ] **Step 6: Commit task**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
git commit -m "refactor: add library app state coordinator"
```

---

### Task 3: Extract route/adaptive shell from `App.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes `LibraryAppState`, existing `LibraryHomeScreen(...)` parameters, route functions, `NowPlayingBar`, `NowPlayingExpandOverlay`, `DrillDownView`, `HomeContent`, and `RouteOverlays`.
- Produces extracted `LibraryHomeScreen(...)` and route shell code outside `App.kt`.

- [ ] **Step 1: Move `LibraryHomeScreen(...)` root shell to `LibraryAppShell.kt`**

Move the `LibraryHomeScreen(...)` function and supporting constants/functions that are shell-specific from `App.kt` into `LibraryAppShell.kt`:

- `LibraryHomeScreen(...)`
- `AdaptiveDetailPlaceholder()`
- `NowPlayingExpandOverlay(...)`
- `NavigationAnimationMillis`
- `NavigationSlideDistancePx`
- `routeContentTransform(...)`
- `routeSlideContentTransform(...)`
- `routeFadeContentTransform(...)`

Keep signatures unchanged except for internal references to Task 2 `appState`.

- [ ] **Step 2: Move nested route lambdas to `LibraryRoutes.kt` as composables**

Create composables with explicit parameters instead of nested functions:

```kotlin
@Composable
internal fun LibraryRouteOverlays(
    route: LibraryRoute,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onDismiss: () -> Unit,
    onShowClearLibrary: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
)
```

```kotlin
@Composable
internal fun LibraryRouteContent(
    route: LibraryRoute,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    selectedTrackId: String?,
    isNowPlayingBarVisible: Boolean,
    onBack: () -> Unit,
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
    onPlayPauseFromTracks: (List<Track>, Track) -> Unit,
    onExpandNowPlaying: (Track) -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    homeContent: @Composable ((LibraryRoute) -> Unit) -> Unit,
)
```

The implementation can refine the parameter list, but all dependencies must be explicit and behavior must remain unchanged.

- [ ] **Step 3: Keep existing route behavior unchanged**

During extraction, preserve these exact behaviors:

- missing album/artist route triggers `LaunchedEffect(route) { onBack() }` and renders empty `Box`;
- album and artist detail track selection starts from first track;
- detail `onPlayPause` sets queue to that detail track list if needed, then toggles play/pause;
- Now Playing route remains overlay-only/comment equivalent;
- Settings/Search/ClearLibraryDialog overlay routing remains unchanged;
- wide list/detail shell uses `openDetailRoute` replacement behavior from Task 2.

- [ ] **Step 4: Run focused tests and shared compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both PASS.

- [ ] **Step 5: Commit task**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt
git commit -m "refactor: extract library route shell"
```

---

### Task 4: Extract home and detail content

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt`

**Interfaces:**
- Produces:
  - `LibraryHomeContent(...)`
  - `DrillDownView(...)` moved unchanged in behavior.

- [ ] **Step 1: Move home content into `LibraryHomeContent.kt`**

Move `HomeContent` logic from the current shell into a top-level internal composable:

```kotlin
@Composable
internal fun LibraryHomeContent(
    snapshot: LibrarySnapshot,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    browseMode: BrowseMode,
    homeListState: LazyListState,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    selectedTrackId: String?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    homeBackdrop: LayerBackdrop?,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onClearLibrary: () -> Unit,
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
)
```

Preserve the existing content order:

1. `HeaderSection(snapshot)`
2. empty-library `ImportAudioCard(...)`
3. active `ScanningCard(...)`
4. `SectionLabel(library_queue)`
5. `BrowseModePicker(...)`
6. albums/artists/songs list content
7. bottom spacer `NowPlayingBarContentPadding`

- [ ] **Step 2: Move detail content into `LibraryDetailContent.kt`**

Move `DrillDownView(...)` without changing its signature unless a moved dependency must become explicit. Preserve:

- `leftEdgeSwipeBack(onBack)` root;
- `rememberSystemBarTopPadding()` use;
- full-size backdrop recording outside chrome/bottom bar;
- `LazyColumn` content padding and spacing;
- `DrillDownScrollbar(...)` and `NestedScrollBlurChrome(...)` overlay siblings;
- bottom `NowPlayingBar(...)` behavior and `AnimatedVisibility` enter/exit.

- [ ] **Step 3: Run focused tests and shared compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both PASS.

- [ ] **Step 4: Commit task**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt
git commit -m "refactor: extract library home and detail content"
```

---

### Task 5: Extract chrome, dialogs, and presentational rows/cards

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify extracted files from Tasks 3-4 as needed.

**Interfaces:**
- Produces moved internal UI components:
  - `AnimatedClearLibraryDialogRoute(...)`
  - `NestedScrollBlurChrome(...)`
  - `DrillDownScrollbar(...)`
  - `rememberSystemBarTopPadding()`
  - `HeaderSection(...)`
  - `ImportAudioCard(...)`
  - `SectionLabel(...)`
  - `TrackRow(...)`
  - `AlbumMark(...)`
  - `DrillDownHeader(...)`
  - `BrowseModePicker(...)`
  - `AlbumCard(...)`
  - `ArtistRow(...)`
  - `ScanningCard(...)`
  - `EqualizerStrip(...)` if still only used by old `NowPlayingCard(...)`; otherwise remove old unreachable card code if compile proves it is unused and no tests reference it.

- [ ] **Step 1: Move chrome into `LibraryChrome.kt`**

Move:

```kotlin
private val NestedScrollChromeToolbarHeight = 56.dp

@Composable
internal fun rememberSystemBarTopPadding(): Dp { ... }

@Composable
internal fun NestedScrollBlurChrome(...) { ... }

@Composable
internal fun DrillDownScrollbar(...) { ... }
```

Use `internal` visibility for moved functions needed across extracted files.

- [ ] **Step 2: Move dialog into `LibraryDialogs.kt`**

Move `AnimatedClearLibraryDialogRoute(...)` unchanged except visibility becomes `internal`.

- [ ] **Step 3: Move rows/cards into `LibraryRows.kt`**

Move presentational leaf composables. Keep modifiers, strings, content descriptions, colors, spacing, and callbacks unchanged.

If `NowPlayingCard(...)`, `playbackStatusLabel(...)`, and `EqualizerStrip(...)` are no longer referenced after current architecture, verify with content search. If they are unused, remove them from `App.kt` in this task and run compile. Do not remove any reachable behavior.

- [ ] **Step 4: Reduce `App.kt` imports and verify it is thin**

After extraction, `App.kt` should primarily contain:

- package/imports needed for `App()` and theme wrapper if not moved;
- `App()` dependency construction and `RhythHausTheme { LibraryHomeScreen(...) }` call;
- `RhythHausTheme(...)` only if not moved to a theme file.

Run this inspection command and record the line count:

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt')
print(sum(1 for _ in p.open()), p)
PY
```

Expected: `App.kt` is substantially smaller than the initial 1,850 lines and no longer defines route/home/detail/chrome/dialog/row/card internals.

- [ ] **Step 5: Run focused tests and shared compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both PASS.

- [ ] **Step 6: Commit task**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt
git commit -m "refactor: split library chrome and components"
```

---

### Task 6: Final verification and evidence

**Files:**
- Modify: `openspec/changes/architecture-refactor/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes completed Tasks 1-5.
- Produces verified evidence and final handoff.

- [ ] **Step 1: Validate OpenSpec change**

Run:

```bash
openspec validate architecture-refactor --strict
```

Expected: `Change 'architecture-refactor' is valid`.

- [ ] **Step 2: Run focused/common tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run primary JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: BUILD SUCCESSFUL. If the known Android artwork deprecation warning appears, record it as existing warning only.

- [ ] **Step 4: Run iOS toolchain and simulator verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: xcodebuild prints installed Xcode version and iOS simulator tests return BUILD SUCCESSFUL. If `xcodebuild` is unavailable, record the exact blocker and do not claim iOS validation passed.

- [ ] **Step 5: Run diff hygiene and architecture checks**

Run:

```bash
git diff --check
python3 - <<'PY'
from pathlib import Path
for p in sorted(Path('shared/src/commonMain/kotlin/com/eterocell/rhythhaus').glob('Library*.kt')) + [Path('shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt')]:
    if p.exists():
        print(f'{sum(1 for _ in p.open()):5d} {p}')
PY
```

Expected: `git diff --check` has no output, and line counts show `App.kt` is no longer the large owner of library UI internals.

- [ ] **Step 6: Update OpenSpec tasks with evidence**

In `openspec/changes/architecture-refactor/tasks.md`, mark each task complete and add one evidence line with the actual command results and commit hashes.

- [ ] **Step 7: Update `progress.md` handoff**

Append:

```text
## Handoff - 2026-07-07 architecture refactor

Route: openspec+superpowers
Owner: implementation
Input: architecture-refactor spec/plan
Output: behavior-preserving shared architecture refactor: App.kt thin entry point, library app state/coordinator, focused library UI files
Verification:
- `openspec validate architecture-refactor --strict`: <pass/fail exact output>
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: <pass/fail exact output>
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: <pass/fail exact output>
- `/usr/bin/xcrun xcodebuild -version`: <pass/fail exact output>
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: <pass/fail exact output>
- `git diff --check`: <pass/fail exact output>
Changed files:
- <list final changed files and why>
Next owner: user for manual visual smoke validation of library navigation/scanning/playback screens
Blockers: <none or exact blocker>
Commit: <final commit hash/message or pending>
```

- [ ] **Step 8: Commit final evidence**

Describe staged diffs before committing. Then run:

```bash
git add docs/superpowers/specs/2026-07-07-architecture-refactor-design.md docs/superpowers/plans/2026-07-07-architecture-refactor.md openspec/changes/architecture-refactor progress.md
git commit -m "docs: record architecture refactor evidence"
```

Expected: final evidence commit created.
