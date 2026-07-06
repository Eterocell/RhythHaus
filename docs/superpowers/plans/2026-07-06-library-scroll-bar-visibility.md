# Library Scroll Bar Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide `NowPlayingBar` when any scrollable screen that renders a bar scrolls downward, and show it when that screen scrolls upward.

**Architecture:** Add a pure common helper that decides bottom-bar visibility from previous/current LazyColumn scroll positions. Wire that helper into the Home, Search results, and album/artist drill-down list states, keep visibility hoisted in `LibraryHomeScreen`, and render every `NowPlayingBar` path through a bottom enter/exit animation. Keep `NowPlayingBar` presentational and preserve existing playback, navigation, tap-to-expand, and drag-up-to-expand behavior.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, Compose `LazyListState`, Compose animation APIs, kotlin-test common tests.

## Global Constraints

- On any scrollable screen that renders a `NowPlayingBar`, downward scroll/drag into content hides the bar.
- On any scrollable screen that renders a `NowPlayingBar`, upward scroll/drag toward earlier content shows the bar.
- Tiny scroll-position jitter does not toggle visibility.
- The hide/show transition is animated from the bottom rather than abrupt.
- Hidden bar does not intercept pointer input.
- Existing tap-to-expand, drag-up-to-expand, playback controls, Search, Settings, route navigation, predictive/system back, and Now Playing overlay behavior are preserved.
- No new dependencies.
- No native navigation migration.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
  - Add small pure data/decision helpers for Library list scroll positions and bar visibility.
- Modify `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
  - Add common tests for hide/show/jitter/index-boundary behavior.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - Add Home `LazyListState` to the main Library/Home `LazyColumn`.
  - Observe Home, Search results, and album/artist drill-down scroll states and update hoisted `isNowPlayingBarVisible`.
  - Wrap the root-level and drill-down `NowPlayingBar` paths in bottom enter/exit animation.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
  - Add a result-list `LazyListState` and report result scroll position to the owner when results are visible.
- Modify `openspec/changes/library-scroll-bar-visibility/tasks.md`
  - Mark tasks complete after implementation and verification.
- Modify `progress.md`
  - Record handoff evidence, commands, risks, and next owner.

---

### Task 1: Add tested Library scroll visibility decision helper

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: no new external dependencies.
- Produces:
  - `data class LibraryScrollPosition(val firstVisibleItemIndex: Int, val firstVisibleItemScrollOffset: Int)`
  - `fun decideNowPlayingBarVisibilityForLibraryScroll(previous: LibraryScrollPosition, current: LibraryScrollPosition, currentlyVisible: Boolean, jitterThresholdPx: Int = 2): Boolean`

- [ ] **Step 1: Write failing tests**

Append these tests to `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt` inside `class LibraryNavigationTest`:

```kotlin
    @Test
    fun libraryScrollDownWithinSameItemHidesNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)

        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
            ),
        )
    }

    @Test
    fun libraryScrollUpWithinSameItemShowsNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun libraryScrollDownAcrossItemBoundaryHidesNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)

        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
            ),
        )
    }

    @Test
    fun libraryScrollUpAcrossItemBoundaryShowsNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun libraryScrollJitterKeepsCurrentNowPlayingBarVisibility() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 31)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
                jitterThresholdPx = 2,
            ),
        )
        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
                jitterThresholdPx = 2,
            ),
        )
    }
```

- [ ] **Step 2: Run focused test and verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL because `LibraryScrollPosition` and `decideNowPlayingBarVisibilityForLibraryScroll` are not defined.

- [ ] **Step 3: Implement the pure helper**

Add this code near the existing navigation helpers in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`, after `routeRequiresInWindowContentAnimation` and before `data class LibraryNavigationStack`:

```kotlin
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
```

- [ ] **Step 4: Run focused test and verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 5: Self-review Task 1**

Check:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
```

Confirm:
- Tests cover same-item down/up, item-boundary down/up, and jitter no-op.
- Helper has no Compose dependency and no side effects.
- Helper defaults match spec and can be reused by `App.kt`.

---

### Task 2: Wire Home Library scroll state to animated NowPlayingBar visibility

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes:
  - `LibraryScrollPosition`
  - `decideNowPlayingBarVisibilityForLibraryScroll(previous, current, currentlyVisible, jitterThresholdPx)`
  - Existing `NowPlayingBar`, `NowPlayingBarContentPadding`, `showNowPlaying`, `expandProgress`, `screenHeightPx`.
- Produces:
  - Home `LazyColumn` controls `isNowPlayingBarVisible` from scroll direction.
  - Root-level `NowPlayingBar` appears/disappears through a bottom animation and does not intercept input when hidden.

- [ ] **Step 1: Add animation imports**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, add imports if absent:

```kotlin
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
```

`AnimatedVisibility` is already imported. If `EnterTransition` or `ExitTransition` is unnecessary after implementation, remove unused imports before verification.

- [ ] **Step 2: Add Home list and bar visibility state**

Inside `LibraryHomeScreen`, near existing state declarations after `var showNowPlaying by remember { mutableStateOf(false) }`, add:

```kotlin
    var isNowPlayingBarVisible by remember { mutableStateOf(true) }
    var previousLibraryScrollPosition by remember { mutableStateOf<LibraryScrollPosition?>(null) }
    val homeListState = rememberLazyListState()
```

- [ ] **Step 3: Observe Home list scroll direction**

After `val previousRoute = ...` and before `@Composable fun RouteContent`, add:

```kotlin
    LaunchedEffect(homeListState.firstVisibleItemIndex, homeListState.firstVisibleItemScrollOffset) {
        val currentPosition = LibraryScrollPosition(
            firstVisibleItemIndex = homeListState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = homeListState.firstVisibleItemScrollOffset,
        )
        val previousPosition = previousLibraryScrollPosition
        if (previousPosition != null) {
            isNowPlayingBarVisible = decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previousPosition,
                current = currentPosition,
                currentlyVisible = isNowPlayingBarVisible,
            )
        }
        previousLibraryScrollPosition = currentPosition
    }
```

This intentionally ignores the first observed position because there is no previous movement yet.

- [ ] **Step 4: Attach state to the main Library/Home LazyColumn only**

In the `LibraryRoute.Home, LibraryRoute.Settings, LibraryRoute.Search, LibraryRoute.ClearLibraryDialog ->` branch, find the main `LazyColumn(` that renders `HeaderSection`, `ImportAudioCard`, `BrowseModePicker`, Albums/Artists/Songs, and `NowPlayingBarContentPadding`.

Change it from:

```kotlin
                    LazyColumn(
                        modifier = Modifier
```

to:

```kotlin
                    LazyColumn(
                        state = homeListState,
                        modifier = Modifier
```

Do not attach `homeListState` to `DrillDownView`; this feature is scoped to the main Library/Home list.

- [ ] **Step 5: Wrap the root NowPlayingBar in AnimatedVisibility**

In the root `Box`, replace the direct root-level `NowPlayingBar(...)` call beginning at the comment `// Fixed bottom bar (outside AnimatedContent)` with:

```kotlin
    // Fixed bottom bar (outside AnimatedContent)
    AnimatedVisibility(
        visible = isNowPlayingBarVisible && !showNowPlaying,
        enter = slideInVertically(initialOffsetY = { it }) + expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        NowPlayingBar(
            track = selectedTrack,
            playbackState = playbackState,
            onPlayPause = {
                selectedTrack?.let { track ->
                    val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                    if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                        playbackController.setQueue(playableTracks, track.id)
                    }
                    playbackController.togglePlayPause()
                }
            },
            onExpand = { if (selectedTrack != null) showNowPlaying = true },
            onSettings = { pushRoute(LibraryRoute.Settings) },
            onSearch = { pushRoute(LibraryRoute.Search) },
            expandProgress = expandProgress,
            isExpanded = showNowPlaying,
            screenHeightPx = screenHeightPx,
        )
    }
```

Notes:
- `AnimatedVisibility` removes hidden content from hit testing after exit, satisfying the non-interception requirement.
- `!showNowPlaying` avoids rendering the mini-player underneath the full Now Playing overlay while it is expanded.
- Keep the existing `NowPlayingExpandOverlay(...)` call unchanged.

- [ ] **Step 6: Compile and fix API/import issues**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: PASS. If there are unused imports from Step 1, remove them. If `expandVertically`/`shrinkVertically` are visually or API-wise unnecessary, use only `slideInVertically + fadeIn` and `slideOutVertically + fadeOut`, but keep bottom animation semantics.

- [ ] **Step 7: Run focused tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 8: Self-review Task 2**

Check:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
```

Confirm:
- `homeListState` is attached only to the main Library/Home `LazyColumn`.
- The bar is wrapped in `AnimatedVisibility` and hidden content cannot intercept input after exit.
- Existing `NowPlayingBar` callbacks are unchanged.
- `NowPlayingExpandOverlay` is still rendered and still receives `showNowPlaying`.
- No route-stack behavior changed.

---

### Task 3: Verification, OpenSpec update, and handoff evidence

**Files:**
- Modify: `openspec/changes/library-scroll-bar-visibility/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: Task 1 and Task 2 implementation.
- Produces: validated OpenSpec task state and project handoff evidence.

- [ ] **Step 1: Validate OpenSpec change**

Run:

```bash
openspec validate library-scroll-bar-visibility --strict
```

Expected: `Change 'library-scroll-bar-visibility' is valid`.

- [ ] **Step 2: Run focused tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 3: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: PASS. If a known flaky test fails, rerun the exact failing test once, then rerun the broad command and record both outputs.

- [ ] **Step 4: Run iOS verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: `xcodebuild` reports installed Xcode and iOS simulator tests pass. If `xcodebuild` is unavailable, record the exact blocker and do not claim iOS validation passed.

- [ ] **Step 5: Run whitespace diff check**

Run:

```bash
git diff --check
```

Expected: no output and exit code 0.

- [ ] **Step 6: Update OpenSpec tasks**

Edit `openspec/changes/library-scroll-bar-visibility/tasks.md` so all implemented and verified checklist items are checked. Include concise command evidence under Task 3 bullets if needed, matching the style of neighboring OpenSpec changes.

- [ ] **Step 7: Update progress.md**

Append a handoff block to `progress.md` with:

```text
## Handoff - 2026-07-06 library scroll bar visibility

Route: openspec+superpowers
Owner: implementation
Scope: Scroll direction controls `NowPlayingBar` visibility on Home, Search results, and album/artist track-list screens.
Implementation:
- Added pure common Library scroll-position helper and tests for hide/show/jitter decisions.
- Wired Home LazyColumn scroll direction to hoisted NowPlayingBar visibility.
- Rendered root NowPlayingBar through bottom enter/exit animation while preserving existing bar callbacks and Now Playing overlay.
Verification:
- openspec validate library-scroll-bar-visibility --strict: <pass/fail exact output>
- ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache: <pass/fail exact output>
- ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache: <pass/fail exact output>
- /usr/bin/xcrun xcodebuild -version: <version or blocker>
- ./gradlew :shared:iosSimulatorArm64Test --configuration-cache: <pass/fail/blocker exact output>
- git diff --check: <pass/fail exact output>
Acceptance:
- Requirement matched: <yes/no>
- Scope controlled: <yes/no>
- Edge cases/risk reviewed: <notes>
Changed files:
- docs/superpowers/specs/2026-07-06-library-scroll-bar-visibility-design.md
- docs/superpowers/plans/2026-07-06-library-scroll-bar-visibility.md
- openspec/changes/library-scroll-bar-visibility/proposal.md
- openspec/changes/library-scroll-bar-visibility/design.md
- openspec/changes/library-scroll-bar-visibility/specs/library-navigation/spec.md
- openspec/changes/library-scroll-bar-visibility/tasks.md
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt
- shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
- progress.md
Next owner: user/OpenSpec for manual visual validation and archive when satisfied.
Blockers: <none or exact blocker>
Commit: <semantic commit hash/message, or pending if not committed yet>
```

- [ ] **Step 8: Final self-review**

Run:

```bash
git status --short
git diff --stat
git diff -- docs/superpowers/specs/2026-07-06-library-scroll-bar-visibility-design.md docs/superpowers/plans/2026-07-06-library-scroll-bar-visibility.md openspec/changes/library-scroll-bar-visibility shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt progress.md
```

Confirm:
- Diff matches the approved spec.
- No unrelated files changed.
- No dependency/toolchain changes.
- No secrets or generated binary artifacts included.


---

## Scope correction: all screens with NowPlayingBar

After the initial Home-only implementation, the requirement was corrected: the behavior must also apply to track lists and all other screens that have a `NowPlayingBar`.

Additional implementation steps:

- [x] Add a shared `updateNowPlayingBarVisibilityForScroll` updater in `LibraryHomeScreen`.
- [x] Convert Home scroll observation to call the shared updater.
- [x] Pass the shared updater into `SearchScreen` and report Search result-list scroll positions.
- [x] Pass the shared updater and current visibility into album/artist `DrillDownView`.
- [x] Observe `DrillDownView` list scroll positions and feed them into the shared updater.
- [x] Wrap the drill-down `NowPlayingBar` in the same bottom `AnimatedVisibility` used for the root fixed bar.
- [x] Re-run compile/tests/OpenSpec validation after widening scope.
