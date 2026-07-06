# Liquid Glass Backdrop Chrome Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace RhythHaus nested-scroll top chrome and bottom `NowPlayingBar` panel surfaces with Kyant0 Backdrop liquid-glass effects while preserving current layout and behavior.

**Architecture:** Add Backdrop through the version catalog and isolate raw Backdrop API usage in a small `LiquidGlassChrome.kt` helper. Route content records a `LayerBackdrop`; top chrome and bottom bar draw glass above that recorded content using the same effect recipe and fallback surface color.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, Kyant0 Backdrop 2.0.0, Kyant0 Shapes 1.2.0, Miuix, OpenSpec.

## Global Constraints

- Use `io.github.kyant0:backdrop:2.0.0` and `io.github.kyant0:shapes:1.2.0` through `gradle/libs.versions.toml`.
- Do not reintroduce Haze APIs or add a Haze dependency.
- Apply Backdrop glass only to the visible top chrome and bottom bar rounded container; do not redesign controls, spacing, route transitions, playback behavior, scanner behavior, or navigation semantics.
- Top chrome must remain bounded to `statusBarHeight + 56.dp` and must not stretch to full-screen height.
- Preserve existing `NowPlayingBar` behavior: empty-library mode, playback controls, Search/Settings controls, tap-to-expand, drag-up expand, navigation-bar padding, hide/show-on-scroll.
- Record content backdrops below overlays; do not record the glass overlays into themselves.
- Verify with OpenSpec validation and supported-platform Gradle/Xcode commands, or record exact blockers.

---

## File Structure

- Modify `gradle/libs.versions.toml`: add `backdrop = "2.0.0"`, `kyant-shapes = "1.2.0"`, `kyant-backdrop`, and `kyant-shapes` aliases.
- Modify `shared/build.gradle.kts`: add Backdrop and Shapes to `commonMain.dependencies`.
- Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`: local wrapper for recording and drawing RhythHaus glass surfaces.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: record Library/Home and DrillDown content backdrops and pass them into `NestedScrollBlurChrome` and root/drill-down `NowPlayingBar` calls.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`: accept optional/default `LayerBackdrop` and use the local glass wrapper for the rounded card container.
- Modify `openspec/changes/liquid-glass-backdrop-chrome/tasks.md`: mark tasks complete with verification evidence.
- Modify `progress.md`: record route, files, verification, blockers, and next owner.

---

### Task 1: Dependencies and Liquid Glass Wrapper

**Files:**
- Modify: `gradle/libs.versions.toml:1-70`
- Modify: `shared/build.gradle.kts:100-119`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`

**Interfaces:**
- Produces: `@Composable fun rememberRhythHausBackdrop(): LayerBackdrop`
- Produces: `fun Modifier.recordRhythHausBackdrop(backdrop: LayerBackdrop): Modifier`
- Produces: `fun Modifier.rhythHausLiquidGlass(backdrop: LayerBackdrop, shape: Shape, fallbackColor: Color, blurRadius: Dp = 8.dp, refractionHeight: Dp = 16.dp, refractionAmount: Dp = 24.dp): Modifier`
- Consumes: Kyant0 `LayerBackdrop`, `rememberLayerBackdrop`, `layerBackdrop`, `drawBackdrop`, `vibrancy`, `blur`, `lens`.

- [ ] **Step 1: Add version-catalog aliases**

Edit `gradle/libs.versions.toml`:

```toml
[versions]
backdrop = "2.0.0"
kyant-shapes = "1.2.0"
```

Add under `[libraries]`:

```toml
kyant-backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }
kyant-shapes = { module = "io.github.kyant0:shapes", version.ref = "kyant-shapes" }
```

Keep existing entries sorted in the file’s current style; do not alter unrelated versions.

- [ ] **Step 2: Add common dependencies**

Edit `shared/build.gradle.kts` in `commonMain.dependencies`:

```kotlin
implementation(libs.kyant.backdrop)
implementation(libs.kyant.shapes)
```

Place near other Compose/UI dependencies, leaving existing dependencies unchanged.

- [ ] **Step 3: Create `LiquidGlassChrome.kt`**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
internal fun rememberRhythHausBackdrop(): LayerBackdrop = rememberLayerBackdrop()

internal fun Modifier.recordRhythHausBackdrop(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

internal fun Modifier.rhythHausLiquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 24.dp,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(refractionHeight.toPx(), refractionAmount.toPx())
    },
    onDrawSurface = {
        drawRect(fallbackColor)
    },
)
```

If the compiler reports unused `drawIntoCanvas`, remove that import; do not add any other behavior.

- [ ] **Step 4: Run dependency compile check**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`. If dependency resolution fails, record the exact artifact/version error in the report and stop for coordinator decision.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt
git commit -m "feat: add liquid glass backdrop wrapper"
```

---

### Task 2: Library/Home Top Chrome Backdrop

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:491-608`

**Interfaces:**
- Consumes from Task 1: `rememberRhythHausBackdrop`, `recordRhythHausBackdrop`, `rhythHausLiquidGlass`.
- Produces: `NestedScrollBlurChrome` receives a `LayerBackdrop` and draws Backdrop glass for Library/Home top chrome.

- [ ] **Step 1: Add imports**

In `App.kt`, add:

```kotlin
import com.kyant.backdrop.backdrops.LayerBackdrop
```

Remove no existing imports unless the compiler marks them unused after the implementation.

- [ ] **Step 2: Create a Home backdrop recorder**

Inside the `LibraryRoute.Home, LibraryRoute.Settings, LibraryRoute.Search, LibraryRoute.ClearLibraryDialog ->` branch, before `Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper)`, add:

```kotlin
val homeBackdrop = rememberRhythHausBackdrop()
```

Change the `Surface` modifier from:

```kotlin
Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
```

to:

```kotlin
Surface(
    modifier = Modifier
        .fillMaxSize()
        .recordRhythHausBackdrop(homeBackdrop),
    color = HausColors.current.paper,
) {
```

This records the content/background below the chrome and root bottom bar.

- [ ] **Step 3: Pass the backdrop to Home top chrome**

Change the Home `NestedScrollBlurChrome` call to:

```kotlin
NestedScrollBlurChrome(
    state = homeScrollChromeState,
    title = stringResource(Res.string.library),
    backdrop = homeBackdrop,
    statusBarHeight = homeStatusBarHeight,
    modifier = Modifier.align(Alignment.TopCenter),
)
```

- [ ] **Step 4: Update `NestedScrollBlurChrome` signature**

Change the function signature to include a required backdrop parameter:

```kotlin
private fun NestedScrollBlurChrome(
    state: NestedScrollChromeState,
    title: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    statusBarHeight: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
)
```

- [ ] **Step 5: Replace the chrome scrim background with Backdrop glass**

In `NestedScrollBlurChrome`, replace the outer `Box` modifier background chain:

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .requiredHeight(chromeHeight)
        .zIndex(3f)
        .background(HausColors.current.paper.copy(alpha = 0.26f + 0.66f * progress)),
) {
```

with:

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .requiredHeight(chromeHeight)
        .zIndex(3f)
        .rhythHausLiquidGlass(
            backdrop = backdrop,
            shape = RoundedCornerShape(0.dp),
            fallbackColor = HausColors.current.paper.copy(alpha = 0.34f + 0.42f * progress),
            blurRadius = (6 + 10 * progress).dp,
            refractionHeight = (8 + 8 * progress).dp,
            refractionAmount = (12 + 12 * progress).dp,
        ),
) {
```

Keep the existing inner gradient, title row, and divider unchanged.

- [ ] **Step 6: Run focused compile**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: apply backdrop glass to library chrome"
```

---

### Task 3: DrillDown Top Chrome Backdrop

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:1251-1324`

**Interfaces:**
- Consumes from Task 2: `NestedScrollBlurChrome(backdrop = ...)`.
- Produces: Album/artist drill-down content records a backdrop and uses Backdrop glass top chrome.

- [ ] **Step 1: Create a drill-down backdrop recorder**

Inside `DrillDownView`, after `val drillDownStatusBarHeight = ...`, add:

```kotlin
val drillDownBackdrop = rememberRhythHausBackdrop()
```

Change:

```kotlin
Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
```

to:

```kotlin
Surface(
    modifier = Modifier
        .fillMaxSize()
        .recordRhythHausBackdrop(drillDownBackdrop),
    color = HausColors.current.paper,
) {
```

- [ ] **Step 2: Pass the backdrop to drill-down top chrome**

Change the drill-down `NestedScrollBlurChrome` call to:

```kotlin
NestedScrollBlurChrome(
    state = scrollChromeState,
    title = title,
    backdrop = drillDownBackdrop,
    statusBarHeight = drillDownStatusBarHeight,
    modifier = Modifier.align(Alignment.TopCenter),
)
```

- [ ] **Step 3: Run navigation tests and compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both commands report `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: apply backdrop glass to drilldown chrome"
```

---

### Task 4: NowPlayingBar Backdrop Glass

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt:1-230`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:674-699,1326-1344`

**Interfaces:**
- Consumes: `LayerBackdrop`, `rhythHausLiquidGlass`.
- Produces: `NowPlayingBar(..., backdrop: LayerBackdrop? = null, ...)` renders Backdrop glass when a backdrop is supplied and falls back to the current panel color otherwise.

- [ ] **Step 1: Add NowPlayingBar imports**

In `NowPlayingBar.kt`, add:

```kotlin
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.backdrops.LayerBackdrop
```

- [ ] **Step 2: Add optional backdrop parameter**

Change `NowPlayingBar` signature to include:

```kotlin
backdrop: LayerBackdrop? = null,
```

Place it before `modifier: Modifier = Modifier` so existing call sites remain source-compatible if they omit it.

- [ ] **Step 3: Extract the rounded shape and glass modifier**

Before the `Surface(` call inside `NowPlayingBar`, add:

```kotlin
val barShape: Shape = RoundedCornerShape(20.dp)
val barModifier = modifier
    .fillMaxWidth()
    .navigationBarsPadding()
    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    .clip(barShape)
    .then(
        if (backdrop != null) {
            Modifier.rhythHausLiquidGlass(
                backdrop = backdrop,
                shape = barShape,
                fallbackColor = HausColors.current.panel.copy(alpha = 0.72f),
                blurRadius = 10.dp,
                refractionHeight = 16.dp,
                refractionAmount = 24.dp,
            )
        } else {
            Modifier.background(HausColors.current.panel)
        },
    )
    .hausClickable(onClick = { if (mode == BottomBarMode.TrackLoaded) onExpand() })
    .verticalSheetGesture(
        expandProgress = expandProgress,
        isActive = !isExpanded && mode == BottomBarMode.TrackLoaded,
        scope = rememberCoroutineScope(),
        onSwipeExpand = onExpand,
        onSwipeCollapse = {},
        threshold = 0.3f,
        referenceHeight = screenHeightPx,
    )
```

Then change the existing `Surface` modifier block to:

```kotlin
Surface(
    modifier = barModifier,
    shape = barShape,
    shadowElevation = 8.dp,
    color = Color.Transparent,
) {
```

Do not change the inner progress bar, artwork, text, play/pause, Search, or Settings content.

- [ ] **Step 4: Pass backdrop to root bottom bar**

In `App.kt`, update the root `NowPlayingBar` call at the bottom of `LibraryHomeScreen` to include:

```kotlin
backdrop = rememberRhythHausBackdrop(),
```

This root bar is outside route content. If using a freshly remembered backdrop here produces a compile/runtime issue because it is not recording content, instead hoist a `val rootBackdrop = rememberRhythHausBackdrop()` at the top of the root `Box`, apply `recordRhythHausBackdrop(rootBackdrop)` to the `AnimatedContent` modifier before `.offset(...)`, and pass `backdrop = rootBackdrop` to the root bar. Prefer the hoisted `rootBackdrop` approach if there is any ambiguity.

Concrete preferred implementation:

```kotlin
val rootBackdrop = rememberRhythHausBackdrop()
```

inside the root `Box` body before `AnimatedContent`, then change the `AnimatedContent` modifier to:

```kotlin
modifier = Modifier
    .fillMaxSize()
    .recordRhythHausBackdrop(rootBackdrop)
    .offset(x = predictiveBackOffset.value.dp),
```

and add `backdrop = rootBackdrop` to the root `NowPlayingBar` call.

- [ ] **Step 5: Pass backdrop to drill-down bottom bar**

In `DrillDownView`, pass the existing `drillDownBackdrop` to the drill-down `NowPlayingBar` call:

```kotlin
backdrop = drillDownBackdrop,
```

- [ ] **Step 6: Run bottom-bar focused tests and compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both commands report `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: apply backdrop glass to bottom bar"
```

---

### Task 5: Verification and Evidence

**Files:**
- Modify: `openspec/changes/liquid-glass-backdrop-chrome/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: completed Tasks 1-4 implementation.
- Produces: verified OpenSpec task state and progress handoff.

- [ ] **Step 1: Validate OpenSpec**

Run:

```bash
openspec validate liquid-glass-backdrop-chrome --strict
```

Expected: `Change 'liquid-glass-backdrop-chrome' is valid`.

- [ ] **Step 2: Run supported-platform verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: Gradle commands report `BUILD SUCCESSFUL`; xcodebuild prints an installed Xcode version. If the broad JVM command hits the known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`, rerun that targeted test and then rerun the broad command once before treating it as a blocker.

- [ ] **Step 3: Run diff hygiene**

Run:

```bash
git diff --check
```

Expected: no output and exit 0.

- [ ] **Step 4: Mark OpenSpec tasks complete**

Update `openspec/changes/liquid-glass-backdrop-chrome/tasks.md` to checked items with concise evidence, for example:

```markdown
# Tasks

- [x] 1. Add Backdrop dependencies and a local RhythHaus glass wrapper.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- [x] 2. Record Library/Home content and apply Backdrop glass to nested-scroll top chrome.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- [x] 3. Record album/artist track-list content and apply Backdrop glass to nested-scroll top chrome.
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` passed.
- [x] 4. Apply Backdrop glass to the bottom NowPlayingBar card while preserving existing controls and gestures.
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache` passed.
- [x] 5. Run supported-platform verification and update progress/OpenSpec evidence.
  - Evidence: `<paste exact final commands and outcomes>`.
```

- [ ] **Step 5: Update `progress.md`**

Add a new handoff near the top:

```markdown
## Handoff - 2026-07-06 liquid glass backdrop chrome

Route: openspec+superpowers
Owner: implementation
Scope: Replace nested-scroll top chrome and bottom NowPlayingBar panel surfaces with Kyant0 Backdrop liquid-glass effect.
Implementation:
- Added Backdrop and Shapes dependencies through the version catalog.
- Added local `LiquidGlassChrome.kt` wrapper for Backdrop recording/effects/fallback draw surface.
- Recorded Library/Home and drill-down content layers and routed nested-scroll top chrome through Backdrop glass.
- Routed root and drill-down `NowPlayingBar` rounded card containers through Backdrop glass while preserving existing controls and gestures.
Verification:
- `openspec validate liquid-glass-backdrop-chrome --strict`: pass.
- `<command>`: pass/fail with exact blocker.
Acceptance:
- Requirement matched: yes/no.
- Scope controlled: yes/no.
- Edge cases/risk reviewed: Backdrop shader support may vary by platform; fallback surface remains readable; manual visual validation recommended on Android/iOS/macOS.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `openspec/changes/liquid-glass-backdrop-chrome/*`
- `docs/superpowers/specs/2026-07-06-liquid-glass-backdrop-chrome-design.md`
- `docs/superpowers/plans/2026-07-06-liquid-glass-backdrop-chrome.md`
- `progress.md`
Next owner: user for manual visual validation of glass appearance on Android/iOS/macOS.
Blockers: none, or exact blocker.
Commit: pending or semantic commit hash/message.
```

- [ ] **Step 6: Final commit**

```bash
git add openspec/changes/liquid-glass-backdrop-chrome/tasks.md progress.md docs/superpowers/plans/2026-07-06-liquid-glass-backdrop-chrome.md
git commit -m "docs: record liquid glass backdrop chrome evidence"
```

---

## Self-Review

- Spec coverage: Tasks 1-4 cover dependency/wrapper, top chrome on Home, top chrome on track-list pages, and bottom bar glass; Task 5 covers verification/evidence.
- Placeholder scan: no TBD/TODO/fill-in placeholders remain; evidence examples explicitly instruct exact final commands/outcomes.
- Type consistency: Backdrop type is `LayerBackdrop`; helper names are consistent across tasks.
- Files coverage: every file touched by implementation or evidence is listed in File Structure and task-level Files sections.
