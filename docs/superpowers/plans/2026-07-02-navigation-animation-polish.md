# Navigation Animation Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Three polish items: Android predictive back visual progress, fixed bottom bar during route transitions, and custom expand/collapse animation from bottom bar to Now Playing.

**Architecture:** All three items modify `App.kt`'s `LibraryHomeScreen` composable and are tightly coupled, so this plan allows either SDD with sequential tasks or inline execution. The key structural change: reorganize `LibraryHomeScreen` root layout so `AnimatedContent`, `NowPlayingBar`, and the Now Playing expand overlay are siblings in one root `Box`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, `navigationevent-compose` 1.1.x (already a dependency), Compose animation APIs.

## Global Constraints

- Android predictive back shows visual progress during the gesture.
- Bottom bar stays fixed during all route transition animations.
- Bottom bar expands to Now Playing with a growth animation, not fade/slide route push.
- `NavigationEventInfo.Slide` does not exist; use `NavigationEventInfo.None` and read `navState.transitionState` for progress.
- Preserve existing playback, scanner, library, theme, search, settings, back navigation, and left-edge swipe behavior.
- Avoid adding dependencies.
- No `SharedTransitionScope`/`SharedTransitionLayout` adoption.
- No native platform navigation migration.
- No screen content redesign beyond the expand/collapse mechanism.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - Restructure `LibraryHomeScreen` root layout.
  - Add predictive back progressollection.
  - Lift `NowPlayingBar` outside `AnimatedContent`.
  - Add Now Playing expand overlay.
- Modify `openspec/changes/navigation-animation-polish/tasks.md`
  - Mark tasks complete after implementation/verification.
- Modify `progress.md`
  - Record verification evidence and handoff.

---

### Task 1: Predictive back visual progress + fixed bottom bar + expand overlay

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: existing `NavigationEventState`, `NavigationBackHandler`, `LibraryNavigationStack`, `NowPlayingBar`, `NowPlayingScreen`, `AnimatedContent`, `LibraryNavigationTransition`.
- Produces: restructured `LibraryHomeScreen` with predictive back progress, fixed bottom bar, and Now Playing expand overlay.

- [ ] **Step 1: Add predictive back progress extraction**

Collect `navState.transitionState` as Compose state. Extract a `predictiveBackProgress: Float` that is:

```kotlin
val predictiveBackProgress = when (val ts = navState.transitionState) {
    is NavigationEventTransitionState.InProgress -> {
        if (ts.direction == NavigationEventTransitionState.TRANSITIONING_BACK) {
            ts.latestEvent.progress
        } else 0f
    }
    else -> 0f
}
```

Import `androidx.navigationevent.NavigationEventTransitionState`.

- [ ] **Step 2: Apply predictive back offset to AnimatedContent**

During a predictive back gesture (progress > 0f), apply a horizontal offset to the entire `AnimatedContent` content so it slides leftward:

```kotlin
val predictiveBackOffsetDp = predictiveBackProgress * 40.dp  // subtle parallax slide
```

Apply this as ahorizontal offset on the `AnimatedContent` modifier so the content visually tracks the back gesture. When progress is 0f, no offset. When progress reaches 1f, the pop fires and `AnimatedContent` completes the transition with its own animation.

- [ ] **Step 3: Restructure LibraryHomeScreen root layout**

Change the root layout from the current structure to:

```kotlin
Box(modifier = modifier.fillMaxSize()) {
    // 1. Main content area with route animation
    AnimatedContent(
        targetState = navigation.current,
        transitionSpec = { routeContentTransform(lastNavigationTransition) },
        label = "LibraryRouteTransition",
        modifier = Modifier
            .fillMaxSize()
            .offset(x = predictiveBackOffsetDp),  // predictive back tracking
    ) { currentRoute ->
        when (val route = currentRoute) {
            is LibraryRoute.AlbumDetail -> { ... }       // keep existing
            is LibraryRoute.ArtistDetail -> { ... }      // keep existing
            LibraryRoute.NowPlaying -> {
                // Empty — handled by the expand overlay below.
                // Still keep back-handling: don't render content here.
                Box(Modifier.fillMaxSize())
            }
            LibraryRoute.Home,
            LibraryRoute.Settings,
            LibraryRoute.Search,
            LibraryRoute.ClearLibraryDialog,
            -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // existing Home surface + overlays (Settings, Search, Clear Library)
                    ...existing code but WITHOUT NowPlayingBar...
                }
            }
        }
    }

    // 2. Fixed bottom bar (outside AnimatedContent)
    NowPlayingBar(
        track = selectedTrack,
        playbackState = playbackState,
        onPlayPause = { ... },       // keep existing
        onExpand = { if (selectedTrack != null) pushRoute(LibraryRoute.NowPlaying) },
        onSettings = { pushRoute(LibraryRoute.Settings) },
        onSearch = { pushRoute(LibraryRoute.Search) },
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    // 3. Now Playing expand overlay (outside AnimatedContent)
    NowPlayingExpandOverlay(
        track = selectedTrack,
        playbackState = playbackState,
        playbackController = playbackController,
        tagLibReader = tagLibReader,
        currentLibraryTrack = ...,
        isVisible = navigation.current == LibraryRoute.NowPlaying,
        onBack = ::popRoute,
        modifier = Modifier.fillMaxSize(),
    )
}
```

Remove the `NowPlayingBar` from inside the Home/Settings/Search/ClearLibraryDialog branch. The bottom bar is now always rendered as a sibling of `AnimatedContent`.

- [ ] **Step 4: Implement NowPlayingExpandOverlay composable**

Add a private composable `NowPlayingExpandOverlay` that:

```kotlin
@Composable
private fun NowPlayingExpandOverlay(
    track: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    isVisible: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(isVisible) {
        if (isVisible) expandProgress.animateTo(1f, tween(300))
        else expandProgress.animateTo(0f, tween(250))
    }
    if (expandProgress.value > 0.001f) {
        val fraction = expandProgress.value
        Box(modifier = modifier) {
            // Panel that grows from bottom bar position to full screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)  // grows from 0 to full height
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(
                    topStart = (24 * (1f - fraction)).dp,
                    topEnd = (24 * (1f - fraction)).dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp,
                ),
                color = HausColors.current.paper,
            ) {
                if (track != null) {
                    NowPlayingScreen(
                        track = track,
                        playbackState = playbackState,
                        playbackController = playbackController,
                        tagLibReader = tagLibReader,
                        currentLibraryTrack = currentLibraryTrack,
                        onBack = onBack,
                        modifier = Modifier.alpha(fraction),  // fade in content
                    )
                }
            }
        }
    }
}
```

Import `androidx.compose.animation.core.Animatable`, `androidx.compose.animation.core.tween`, `androidx.compose.ui.draw.alpha`.

Key details:
- `fillMaxHeight(fraction)` makes the panel grow from 0 to full height.
- Corner radius shrinks from 24dp (bar shape) to 0dp (full screen).
- `alpha(fraction)` fades Now Playing content in as the panel grows.
- When collapsing (`isVisible = false`), the `Animatable` animates back to 0f. The composable stops rendering when `value <= 0.001f`.
- When the overlay is at full screen, the `NowPlayingScreen`'s own `leftEdgeSwipeBack` and `BackChip` still work for closing.
- The `NowPlayingBar` is behind the overlay; when at full screen, the bar is hidden. When collapsing, the bar becomes visible as the panel shrinks.

- [ ] **Step 5: Remove NowPlaying route rendering from AnimatedContent**

Replace the current `LibraryRoute.NowPlaying` branch inside `AnimatedContent` with a `Box(Modifier.fillMaxSize())` placeholder since the actual content is rendered by the overlay.

- [ ] **Step 6: Remove NowPlayingBar from the Home/Settings/Search/ClearLibrary branch**

The current code renders `NowPlayingBar` inside the Home/Settings/Search/ClearLibraryDialog route branch. Remove it so the bar is only rendered as the fixed sibling described in Step 3.

Also remove `NowPlayingBarContentPadding` spacer or keep it if it's needed for content scrolling. If the spacer was inside the `LazyColumn` content, keep it so content still scrolls above the bar.

- [ ] **Step 7: Compile and fix import/API issues**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Fix any missing imports or API issues. If `ExperimentalAnimationApi` opt-in is no longer needed (since `AnimatedContent` moved to stable in newer Compose), remove it.

- [ ] **Step 8: Run focused navigation tests**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS (navigation stack behavior unchanged).

---

### Task 2: Verification, OpenSpec update, and handoff evidence

**Files:**
- Modify: `openspec/changes/navigation-animation-polish/tasks.md`
- Modify: `openspec/changes/navigation-animations/tasks.md` (if needed for consistency)
- Modify: `progress.md`

- [ ] **Step 1: Validate OpenSpec changes**

```bash
openspec validate navigation-animation-polish --strict
```

Expected: `Change 'navigation-animation-polish' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

- [ ] **Step 3: Run iOS simulator tests**

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

- [ ] **Step 4: Update OpenSpec task checklist**

Mark all tasks complete in `openspec/changes/navigation-animation-polish/tasks.md`.

- [ ] **Step 5: Update progress.md**

Prepend evidence for the polish work covering both the base animation and polish changes.

- [ ] **Step 6: Final diff review**

```bash
git diff --check
git diff --stat
```

- [ ] **Step 7: Stage all changes**

```bash
git add docs/superpowers/specs/2026-07-02-navigation-animations-design.md docs/superpowers/plans/2026-07-02-navigation-animations.md docs/superpowers/specs/2026-07-02-navigation-animation-polish-design.md docs/superpowers/plans/2026-07-02-navigation-animation-polish.md openspec/changes/navigation-animations openspec/changes/navigation-animation-polish shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt progress.md
```

- [ ] **Step 8: Summarize staged diff for user**

Present the staged diff summary to the user for review. Commit only after user approval.

```bash
git commit -m "feat: add navigation animations with predictive back and bottom bar expand"
```