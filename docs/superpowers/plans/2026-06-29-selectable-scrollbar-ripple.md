# Selectable Scrollbar and Ripple Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the album/artist drill-down scrollbar selectable and add visible ripple feedback to main custom clickable controls and list/card surfaces.

**Architecture:** Keep the change in shared Compose common code. Add one reusable Haus ripple clickable modifier, replace visible custom `Modifier.clickable` usage with that helper, and replace the drill-down visual-only scroll indicator with a right-edge scrubber attached to the existing `LazyListState`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, Compose foundation/ui pointer APIs, existing Miuix components. No new dependencies.

## Global Constraints

- Work in `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus`.
- Preserve existing local uncommitted changes in `progress.md` and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`; patch only this task's UI lines.
- Shared Compose only: do not modify `iosApp`, Android-specific, iOS-specific, JVM-specific, dependency, or version catalog files.
- Do not add dependencies or new Material/Material3 UI imports.
- Do not replace Miuix `Button` or `Slider` behavior.
- Do not add ripple to invisible overlay touch blockers using `.clickable(enabled = false, onClick = {})`.
- Keep existing shapes, colors, text, spacing, semantics, content descriptions, list ordering, playback behavior, and navigation behavior unless this plan explicitly changes an interaction modifier.
- Focused verification command: `./gradlew :shared:compileKotlinJvm --configuration-cache`.
- Stage only task implementation files and describe staged diffs before committing.

---

### Task 1: Selectable Scrollbar and Ripple Feedback

**Files:**
- Create: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/iosApp/ContentView.swift`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/iosApp/iOSApp.swift`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/gradle/libs.versions.toml`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/build.gradle.kts`

**Interfaces:**
- Consumes: existing `LazyListState` inside `DrillDownView`, existing custom clickable call sites, existing color constants `HausInk`, `HausMuted`, `HausPanel`, `HausPulse`.
- Produces: `fun Modifier.hausClickable(onClick: () -> Unit): Modifier` for visible custom clickables.
- Produces: `@Composable private fun DrillDownScrollbar(listState: LazyListState, modifier: Modifier = Modifier)` inside `App.kt`, or an equivalent private composable, with a `24.dp` hit area and `6.dp` visible thumb.

- [ ] **Step 1: Re-check workspace and current code**

Run:

```bash
git status --short
git log --oneline -5
sed -n '1,80p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '981,1035p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '1110,1170p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '1190,1220p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '1270,1355p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '1,190p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
sed -n '60,90p;200,260p;276,292p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
sed -n '50,80p;154,190p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt
sed -n '43,70p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt
```

Expected:
- `progress.md` and `App.kt` may already be modified before this task starts.
- Current drill-down scrollbar is a `BoxWithConstraints` with `.width(3.dp)`.
- Visible custom clickables use `.clickable(...)` in the files listed above.
- Search/settings root overlay blockers use `.clickable(enabled = false, onClick = {})` and must stay unchanged.

- [ ] **Step 2: Create the shared Haus ripple clickable helper**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt` with this content:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.hausClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = ripple(
            bounded = true,
            color = HausInk.copy(alpha = 0.14f),
        ),
        onClick = onClick,
    )
}
```

Reasoning:
- Use Compose foundation ripple; do not add dependencies and do not introduce Material/Material3 UI imports.
- This helper is only for visible custom clickables.
- Keep shape clipping at call sites before `hausClickable(...)` so ripples remain bounded by existing rounded/circular shapes.

- [ ] **Step 3: Update `App.kt` imports for scrollbar pointer handling**

In `App.kt`, add only the missing imports needed for the selectable scrollbar:

```kotlin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
```

If any of these imports already exist, do not duplicate them.

- [ ] **Step 4: Replace `App.kt` visible custom clickables with `hausClickable`**

In `App.kt`, replace only visible custom `.clickable(...)` call sites that are in the approved scope.

Replace the developer panel header modifier:

```kotlin
.clickable(onClick = onToggle)
```

with:

```kotlin
.hausClickable(onClick = onToggle)
```

Replace `TrackRow`:

```kotlin
.clickable(onClick = onClick)
```

with:

```kotlin
.hausClickable(onClick = onClick)
```

Replace `DrillDownHeader` back chip:

```kotlin
.clickable(onClick = onBack)
```

with:

```kotlin
.hausClickable(onClick = onBack)
```

Replace `AlbumCard` card modifier:

```kotlin
.clickable(onClick = onClick)
```

with:

```kotlin
.hausClickable(onClick = onClick)
```

Replace `ArtistRow`:

```kotlin
.clickable(onClick = onClick)
```

with:

```kotlin
.hausClickable(onClick = onClick)
```

Do not change any invisible `.clickable(enabled = false, onClick = {})` in this task.

- [ ] **Step 5: Replace the drill-down visual-only scrollbar with a selectable scrubber**

In `DrillDownView`, replace the whole current `// Scroll indicator` block, from:

```kotlin
// Scroll indicator
val scrollFraction by remember(listState) {
    derivedStateOf {
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        val maxFirstVisibleIndex = (total - visible).coerceAtLeast(1)
        (listState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex).coerceIn(0f, 1f)
    }
}
BoxWithConstraints(
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .padding(vertical = 4.dp)
        .width(3.dp)
) {
    val thumbHeight = maxHeight * 0.15f
    val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbHeight)
            .offset(y = thumbOffset)
            .clip(RoundedCornerShape(2.dp))
            .background(HausMuted.copy(alpha = 0.3f)),
    )
}
```

to:

```kotlin
DrillDownScrollbar(
    listState = listState,
    modifier = Modifier.align(Alignment.CenterEnd),
)
```

Add this private composable below `DrillDownView` and above `DrillDownHeader`:

```kotlin
@Composable
private fun DrillDownScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollFraction by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val maxFirstVisibleIndex = (total - visible).coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex).coerceIn(0f, 1f)
        }
    }

    fun scrollTo(yPosition: Float, trackHeightPx: Float) {
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        if (total <= visible || trackHeightPx <= 0f) return

        val maxFirstVisibleIndex = (total - visible).coerceAtLeast(0)
        val targetFraction = (yPosition / trackHeightPx).coerceIn(0f, 1f)
        val targetIndex = (targetFraction * maxFirstVisibleIndex).toInt().coerceIn(0, maxFirstVisibleIndex)
        coroutineScope.launch {
            listState.animateScrollToItem(targetIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(24.dp)
            .pointerInput(listState) {
                detectTapGestures { offset ->
                    scrollTo(offset.y, size.height.toFloat())
                }
            }
            .pointerInput(listState) {
                detectVerticalDragGestures { change, _ ->
                    scrollTo(change.position.y, size.height.toFloat())
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbHeight = maxHeight * 0.15f
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .width(6.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(HausMuted.copy(alpha = 0.42f)),
        )
    }
}
```

If the compiler reports that `scrollTo` cannot be called from `pointerInput` because of capture rules, keep the same behavior by making the scroll calculation inside each pointer block. Do not change the public UI behavior.

- [ ] **Step 6: Update `NowPlayingBar.kt` visible custom clickables**

Replace these visible custom clickables with `hausClickable`:

```kotlin
.clickable(onClick = onExpand)
.clickable(onClick = onPlayPause)
.clickable(onClick = onSearch)
.clickable(onClick = onSettings)
```

Use the exact replacements:

```kotlin
.hausClickable(onClick = onExpand)
.hausClickable(onClick = onPlayPause)
.hausClickable(onClick = onSearch)
.hausClickable(onClick = onSettings)
```

Keep existing `clip`, `background`, `shape`, `safeContentPadding`, and sizes unchanged.

- [ ] **Step 7: Update `NowPlayingScreen.kt` visible custom clickables**

Replace the back chip:

```kotlin
.clickable(onClick = onBack)
```

with:

```kotlin
.hausClickable(onClick = onBack)
```

Replace each transport control custom clickable. For the previous button, change:

```kotlin
.clickable {
    val queue = playbackState.queue
    val currentId = playbackState.currentTrack?.id
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
    prevTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
},
```

to:

```kotlin
.hausClickable {
    val queue = playbackState.queue
    val currentId = playbackState.currentTrack?.id
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
    prevTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
},
```

For the play/pause button, change:

```kotlin
.clickable { playbackController.togglePlayPause() },
```

to:

```kotlin
.hausClickable { playbackController.togglePlayPause() },
```

For the next button, change:

```kotlin
.clickable {
    val queue = playbackState.queue
    val currentId = playbackState.currentTrack?.id
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
    nextTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
},
```

to:

```kotlin
.hausClickable {
    val queue = playbackState.queue
    val currentId = playbackState.currentTrack?.id
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
    nextTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
},
```

Replace the developer panel header:

```kotlin
.clickable { expanded = !expanded }
```

with:

```kotlin
.hausClickable { expanded = !expanded }
```

Do not change the Miuix `Slider`.

- [ ] **Step 8: Update `SearchScreen.kt` visible custom clickables**

Replace the visible back chip:

```kotlin
.clickable(onClick = onDismiss)
```

with:

```kotlin
.hausClickable(onClick = onDismiss)
```

Replace the result row surface:

```kotlin
modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
```

with:

```kotlin
modifier = Modifier.fillMaxWidth().hausClickable(onClick = onClick),
```

Do not change the root overlay blocker:

```kotlin
.clickable(enabled = false, onClick = {})
```

- [ ] **Step 9: Update `SettingsScreen.kt` visible custom clickable**

Replace the visible back chip:

```kotlin
.clickable(onClick = onDismiss)
```

with:

```kotlin
.hausClickable(onClick = onDismiss)
```

Do not change the root overlay blocker:

```kotlin
.clickable(enabled = false, onClick = {})
```

Do not change any Miuix `Button` call.

- [ ] **Step 10: Verify no out-of-scope ripple replacements happened**

Run:

```bash
rg -n "clickable\(" shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt
rg -n "hausClickable" shared/src/commonMain/kotlin/com/eterocell/rhythhaus
```

Expected:
- Remaining `clickable(` calls in `SearchScreen.kt` and `SettingsScreen.kt` include the invisible root overlay blockers.
- Any remaining `clickable(` calls outside those blockers are either out of the approved main-visible scope or should be converted before continuing.
- `hausClickable` appears in the helper file and visible custom clickable call sites listed in this task.

- [ ] **Step 11: Compile focused shared UI target**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: Gradle exits 0. Existing Compose dependency mismatch warnings may appear; do not treat existing warnings as failures.

- [ ] **Step 12: Review scope and commit**

Run:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt
git status --short
```

Expected:
- Diffs are limited to the six shared Compose files in this task.
- No dependency files, iOS files, or platform-specific files changed.
- Pre-existing `progress.md` remains unstaged.

Stage only task implementation files, describe the staged diff, then commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt
git diff --cached --stat
git commit -m "feat: add selectable scrollbar and ripple feedback"
```

Do not stage or commit `progress.md` during this task.
