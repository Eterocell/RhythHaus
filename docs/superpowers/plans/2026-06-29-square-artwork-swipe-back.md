# Square Artwork and Swipe Back Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make shared Compose album artwork square and add left-edge swipe-to-back behavior to shared Compose detail screens.

**Architecture:** Keep the change in shared Compose UI. Add a small reusable gesture modifier/helper in common code, then apply it to the screen-level containers that already own `onBack`. Change rectangular artwork containers to `aspectRatio(1f)` while preserving existing compact square/circle artwork.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, Miuix components already used by the project, no new dependencies.

## Global Constraints

- Work in `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus` unless explicitly told otherwise.
- Shared Compose only: do not implement native SwiftUI navigation or change `iosApp` files.
- Do not add dependencies or Material/Material3 migration work.
- Preserve existing local uncommitted changes in `progress.md` and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`; patch only the UI lines needed by this plan.
- Keep existing compact artwork shapes unchanged: `NowPlayingBar` 40 dp rounded square, `AlbumMark` 54 dp rounded square, and `ArtistRow` circle.
- Use `ContentScale.Crop` for decoded artwork.
- Run `./gradlew :shared:compileKotlinJvm --configuration-cache` after implementation.
- Review `git diff` before committing and describe staged diffs before committing.

---

### Task 1: Shared Compose Square Artwork and Swipe Back

**Files:**
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/iosApp/ContentView.swift`
- Do not modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/iosApp/iOSApp.swift`

**Interfaces:**
- Consumes: existing `onBack: () -> Unit` callbacks in `DrillDownView` and `NowPlayingScreen`.
- Produces: a private shared Compose helper/modifier callable from both files, with behavior equivalent to `Modifier.leftEdgeSwipeBack(onBack)` or a package-private function if implemented in `App.kt` and reused from `NowPlayingScreen.kt`.

- [ ] **Step 1: Re-check workspace state and current source**

Run:

```bash
git status --short
sed -n '1,120p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
sed -n '90,125p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
sed -n '1275,1305p' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
```

Expected:
- `progress.md` and `App.kt` may already be modified before this task starts.
- `NowPlayingScreen.kt` should show a `fillMaxWidth().height(300.dp)` artwork box.
- `App.kt` should show `AlbumCard` artwork with `fillMaxWidth().height(120.dp)`.
- `App.kt` may show an older inline now-playing artwork image with `fillMaxWidth().height(220.dp)`.

- [ ] **Step 2: Add Compose imports needed for square aspect ratio and gestures**

In `App.kt`, add only missing imports required by the implementation. The expected imports are:

```kotlin
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.pointer.pointerInput
```

`App.kt` already imports `Offset` from `androidx.compose.ui.geometry.Offset`; reuse it.

In `NowPlayingScreen.kt`, add only missing imports required by the implementation. The expected imports are:

```kotlin
import androidx.compose.foundation.layout.aspectRatio
```

If the gesture helper is moved to a new separate common file, add the gesture imports there instead and keep imports minimal in `App.kt`/`NowPlayingScreen.kt`.

- [ ] **Step 3: Add the shared swipe-back helper**

Add a small helper in shared common code. Prefer a private top-level helper in `App.kt` only if `NowPlayingScreen.kt` can call it without visibility issues; otherwise create a package-private top-level extension in a new common file such as `SwipeBackGesture.kt`.

The helper should be equivalent to:

```kotlin
private const val SwipeBackEdgeWidthPx = 56f
private const val SwipeBackTriggerDistancePx = 96f

private fun Modifier.leftEdgeSwipeBack(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    var startedAtLeftEdge = false
    var accumulatedDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { offset ->
            startedAtLeftEdge = offset.x <= SwipeBackEdgeWidthPx
            accumulatedDrag = 0f
        },
        onHorizontalDrag = { _, dragAmount ->
            if (startedAtLeftEdge) {
                accumulatedDrag = (accumulatedDrag + dragAmount).coerceAtLeast(0f)
            }
        },
        onDragEnd = {
            if (startedAtLeftEdge && accumulatedDrag >= SwipeBackTriggerDistancePx) {
                onBack()
            }
            startedAtLeftEdge = false
            accumulatedDrag = 0f
        },
        onDragCancel = {
            startedAtLeftEdge = false
            accumulatedDrag = 0f
        },
    )
}
```

If the compiler rejects any `detectHorizontalDragGestures` parameter names for the Compose version, inspect the dependency source or adapt to the available signature while preserving the same behavior.

- [ ] **Step 4: Apply swipe-back at screen-level containers**

In `DrillDownView` in `App.kt`, change the outer branch container from:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
```

to:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .leftEdgeSwipeBack(onBack),
) {
```

This should wrap the album and artist drill-down screens. Do not attach the helper inside individual list rows.

In `NowPlayingScreen.kt`, change the `Surface` modifier from:

```kotlin
Surface(modifier = modifier.fillMaxSize(), color = HausPaper) {
```

to:

```kotlin
Surface(
    modifier = modifier
        .fillMaxSize()
        .leftEdgeSwipeBack(onBack),
    color = HausPaper,
) {
```

Keep existing visible back buttons unchanged.

- [ ] **Step 5: Make rectangular album artwork square**

In `AlbumCard` in `App.kt`, change the artwork box modifier from:

```kotlin
.fillMaxWidth()
.height(120.dp)
```

to:

```kotlin
.fillMaxWidth()
.aspectRatio(1f)
```

Do not change `NowPlayingBar`, `AlbumMark`, or `ArtistRow` shapes.

- [ ] **Step 6: Make full Now Playing artwork square**

In `NowPlayingScreen.kt`, change the large artwork box modifier from:

```kotlin
.fillMaxWidth()
.height(300.dp)
```

to:

```kotlin
.fillMaxWidth()
.aspectRatio(1f)
```

Keep `contentScale = ContentScale.Crop` unchanged.

- [ ] **Step 7: Make older inline now-playing artwork square if still present**

If `App.kt` still contains the older inline now-playing card artwork image:

```kotlin
.fillMaxWidth()
.height(220.dp)
.clip(RoundedCornerShape(20.dp))
```

change it to:

```kotlin
.fillMaxWidth()
.aspectRatio(1f)
.clip(RoundedCornerShape(20.dp))
```

If this code path has been removed by prior local edits, record that it was not present and do not re-add it.

- [ ] **Step 8: Compile focused shared UI target**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: Gradle exits 0. If compilation fails because the gesture API signature differs, adapt only the helper and rerun this command.

- [ ] **Step 9: Review scope and iOS non-change**

Run:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt iosApp/iosApp/ContentView.swift iosApp/iosApp/iOSApp.swift
git status --short
```

Expected:
- Diffs are limited to shared Compose files needed by this task.
- No diff appears for `iosApp/iosApp/ContentView.swift` or `iosApp/iosApp/iOSApp.swift`.
- Pre-existing `progress.md` remains unrelated and unstaged unless already staged before this task.

- [ ] **Step 10: Commit the implementation**

Before committing, describe the staged diff. Stage only files changed for this task, then commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
# add SwipeBackGesture.kt too if created
git diff --cached --stat
git commit -m "feat: add square artwork and swipe back"
```

Expected: one implementation commit. Do not stage or commit `progress.md` unless the controller explicitly asks after implementation verification.
