# Swipe Gestures for Now Playing Sheet — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add interactive vertical swipe gestures to open/close the Now Playing detail sheet.

**Architecture:** A new `Modifier.verticalSheetGesture()` modifier (sibling to `SwipeBackGesture.kt`) shared between the overlay and mini-player bar. One `Animatable<Float>` (0=closed, 1=open) drives both gesture tracking and the overlay's visual height.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform

## Global Constraints

- Works on Android, iOS, and macOS/desktop JVM
- Follows existing pattern: `SwipeBackGesture.kt` for modifier structure
- Thresholds: swipe-down close at fraction < 0.7, swipe-up open at fraction > 0.3
- Rubber-banding at half-speed beyond [0, 1]
- Tap on bar still opens the sheet (existing `hausClickable` stays)
- Left-edge swipe back still works (existing modifier stays)
- No verticalScroll in NowPlayingScreen (removed)

---

### Task 1: Create VerticalSheetGesture.kt

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/VerticalSheetGesture.kt`

**Interfaces:**
- Consumes: nothing (standalone new file)
- Produces: `Modifier.verticalSheetGesture(expandProgress: Animatable<Float, AnimationVector1D>, isActive: Boolean, onSwipeExpand: () -> Unit, onSwipeCollapse: () -> Unit): Modifier`

- [ ] **Step 1: Create the file with imports and function signature**

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

private const val CloseThreshold = 0.7f
private const val OpenThreshold = 0.3f
private const val RubberBandFactor = 0.5f

fun Modifier.verticalSheetGesture(
    expandProgress: Animatable<Float, AnimationVector1D>,
    isActive: Boolean,
    onSwipeExpand: () -> Unit,
    onSwipeCollapse: () -> Unit,
): Modifier = pointerInput(isActive) {
    if (!isActive) return@pointerInput
    val scope = rememberCoroutineScope()
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = {
            totalDrag = 0f
        },
        onVerticalDrag = { _, dragAmount ->
            scope.launch {
                totalDrag += dragAmount
                val screenHeight = size.height.toFloat()
                if (screenHeight <= 0f) return@launch
                val current = expandProgress.value
                val delta = -(dragAmount / screenHeight)
                var target = current + delta
                if (target < 0f) {
                    target = current + delta * RubberBandFactor
                } else if (target > 1f) {
                    target = current + delta * RubberBandFactor
                }
                expandProgress.snapTo(target.coerceIn(-0.05f, 1.05f))
            }
        },
        onDragEnd = {
            scope.launch {
                val target = expandProgress.value
                if (target >= CloseThreshold) {
                    expandProgress.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                } else {
                    expandProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    onSwipeCollapse()
                }
            }
        },
        onDragCancel = {
            scope.launch {
                val current = expandProgress.value
                val target = if (current >= CloseThreshold) 1f else 0f
                expandProgress.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                if (target == 0f) onSwipeCollapse()
            }
        },
    )
}
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew :shared:jvmTest --configuration-cache`
Expected: compilation error-free (the file is unused at this point, but should compile)

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/VerticalSheetGesture.kt
git commit -m "feat: add VerticalSheetGesture modifier for interactive sheet drag"
```

---

### Task 2: Remove verticalScroll from NowPlayingScreen.kt

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt:65-69`

**Interfaces:**
- Consumes: existing `NowPlayingScreen` composable signature
- Produces: same signature, no verticalScroll

- [ ] **Step 1: Remove verticalScroll from the Column modifier**

In `NowPlayingScreen.kt`, change the Column modifier from:
```kotlin
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
```

To:
```kotlin
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 20.dp),
```

Also remove unused imports:
- Remove `import androidx.compose.foundation.rememberScrollState`
- Remove `import androidx.compose.foundation.verticalScroll`

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :shared:jvmTest --configuration-cache`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
git commit -m "refactor: remove unused verticalScroll from NowPlayingScreen"
```

---

### Task 3: Wire gesture into App.kt (overlay, bar, LibraryHomeScreen)

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`

**Interfaces:**
- Consumes: `VerticalSheetGesture.kt` (from Task 1), modified `NowPlayingScreen.kt` (from Task 2)
- Produces: gesture-integrated overlay and bar

- [ ] **Step 1: Modify NowPlayingExpandOverlay — accept shared expandProgress**

Change the signature to add `expandProgress` parameter. In `NowPlayingExpandOverlay`:

```kotlin
@Composable
private fun NowPlayingExpandOverlay(
    track: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    isVisible: Boolean,
    expandProgress: Animatable<Float, AnimationVector1D>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

Remove the private `Animatable` and `LaunchedEffect`:
```kotlin
    // REMOVE these two lines:
    // val expandProgress = remember { Animatable(0f) }
    // LaunchedEffect(isVisible) { ... }
```

Add the gesture modifier to the `Surface` inside the overlay's `Box`, after `.align(Alignment.BottomCenter)`:

```kotlin
        Box(modifier = modifier) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .align(Alignment.BottomCenter)
                    .verticalSheetGesture(
                        expandProgress = expandProgress,
                        isActive = true,
                        onSwipeExpand = {},
                        onSwipeCollapse = onBack,
                    ),
```

- [ ] **Step 2: Modify NowPlayingBar — accept shared expandProgress**

Change the `NowPlayingBar` signature to add `expandProgress` parameter:

```kotlin
@Composable
fun NowPlayingBar(
    track: Track?,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
```

Add the gesture modifier to the `Surface` modifier chain (after `.hausClickable(...)`):

```kotlin
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .hausClickable(onClick = { if (mode == BottomBarMode.TrackLoaded) onExpand() })
            .verticalSheetGesture(
                expandProgress = expandProgress,
                isActive = !isExpanded && mode == BottomBarMode.TrackLoaded,
                onSwipeExpand = onExpand,
                onSwipeCollapse = {},
            ),
```

Note: the `isActive` parameter uses `!isExpanded` so the gesture is only active when the sheet is closed. Trade-off: the `isActive` lambda re-reads `isExpanded` from the parent composable on each recomposition inside the `pointerInput` key. When `isExpanded` changes, the `pointerInput` block restarts. This is correct — we want the gesture to stop when the sheet opens, and the animation inside the composable's `LaunchedEffect` takes over.

- [ ] **Step 3: Modify LibraryHomeScreen — own and manage shared expandProgress**

In `LibraryHomeScreen`, add the shared `Animatable`:

```kotlin
    // Add near the other remember declarations (around line 281)
    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(navigation.current == LibraryRoute.NowPlaying) {
        val target = if (navigation.current == LibraryRoute.NowPlaying) 1f else 0f
        expandProgress.animateTo(target, tween(300))
    }
```

Update the `NowPlayingExpandOverlay` call (line 582) to pass `expandProgress`:

```kotlin
    NowPlayingExpandOverlay(
        track = selectedTrack,
        playbackState = playbackState,
        playbackController = playbackController,
        tagLibReader = tagLibReader,
        currentLibraryTrack = libraryTracks.firstOrNull { it.id == selectedTrack?.id },
        isVisible = navigation.current == LibraryRoute.NowPlaying,
        expandProgress = expandProgress,
        onBack = ::popRoute,
        modifier = Modifier.fillMaxSize(),
    )
```

Update the `NowPlayingBar` call (line 563) to pass `expandProgress` and `isExpanded`:

```kotlin
    NowPlayingBar(
        track = selectedTrack,
        playbackState = playbackState,
        onPlayPause = { ... },
        onExpand = { if (selectedTrack != null) pushRoute(LibraryRoute.NowPlaying) },
        onSettings = { pushRoute(LibraryRoute.Settings) },
        onSearch = { pushRoute(LibraryRoute.Search) },
        expandProgress = expandProgress,
        isExpanded = navigation.current == LibraryRoute.NowPlaying,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
```

Add the import for `Animatable` at the top of App.kt if not already present:
```kotlin
import androidx.compose.animation.core.Animatable
```
(It should already be imported since `NowPlayingExpandOverlay` uses it; verify.)

- [ ] **Step 4: Verify compilation across all targets**

Run: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
Expected: all PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
git commit -m "feat: wire interactive swipe gestures into NowPlaying sheet"
```

---

### Task 4: Build verification and iOS check

**Files:**
- None changed — verification only

- [ ] **Step 1: Run full verification**

```bash
./init.sh
```
Expected: all targets compile and tests pass

- [ ] **Step 2: Run iOS-specific verification**

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```
Expected: PASS

- [ ] **Step 3: Verify no new compilation warnings**

Run: `./gradlew :shared:jvmTest 2>&1 | grep -i "warning"`
Expected: no new warnings related to this change

- [ ] **Step 4: Commit verification report**

```bash
git add -A
git commit -m "chore: verification report for swipe gestures feature"
```
