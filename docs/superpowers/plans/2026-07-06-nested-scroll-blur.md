# Nested Scroll Blur Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared nested-scroll frosted chrome effect to Library/Home and album/artist track-list pages.

**Architecture:** Implement a small common Compose visual primitive in `App.kt` backed by pure progress math in `LibraryNavigation.kt` so behavior can be unit tested. Apply it to the existing Home and `DrillDownView` `LazyColumn`s without altering playback, scanning, route-stack, or NowPlayingBar state.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform common UI, Haze (`dev.chrisbanes.haze:haze`) for shared blur, existing Miuix/RhythHaus primitives, JVM common tests.

## Global Constraints

- Use Haze as the only new Gradle dependency for the blur/backdrop effect.
- Do not change platform-specific app entry points.
- Preserve existing NowPlayingBar scroll-hide behavior.
- Preserve existing Library/Home, album detail, artist detail, Search, Settings, scanner, and playback behavior.
- Use shared common Compose APIs only.

---

### Task 1: Scroll Chrome Progress Model

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Produces: `data class NestedScrollChromeState(val progress: Float, val headerOffsetPx: Float)`
- Produces: `fun nestedScrollChromeStateFor(position: LibraryScrollPosition, activationDistancePx: Float = 160f, maxHeaderOffsetPx: Float = 18f): NestedScrollChromeState`

- [ ] **Step 1: Add failing tests**
  - Test top-of-list state returns `progress = 0f` and `headerOffsetPx = 0f`.
  - Test partial scroll clamps progress between 0 and 1.
  - Test item-index scroll returns `progress = 1f` and max offset.

- [ ] **Step 2: Run focused test**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`
Expected before implementation: compile/test failure because the new function is missing.

- [ ] **Step 3: Implement pure model**
  - Convert scroll position into activation distance: if `firstVisibleItemIndex > 0`, treat as fully active; otherwise use `firstVisibleItemScrollOffset / activationDistancePx`.
  - Clamp progress to `0f..1f`.
  - Set `headerOffsetPx = -maxHeaderOffsetPx * progress`.

- [ ] **Step 4: Verify**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`
Expected: build successful.

### Task 2: Apply Chrome to Home and Track Lists

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: `nestedScrollChromeStateFor(position: LibraryScrollPosition, activationDistancePx: Float = 160f, maxHeaderOffsetPx: Float = 18f): NestedScrollChromeState`
- Produces: `@Composable private fun NestedScrollBlurChrome(...)`

- [ ] **Step 1: Add shared composable**
  - `NestedScrollBlurChrome` draws a top-aligned layer with `statusBarsPadding()`, paper color at scroll-dependent alpha, a Haze frosted panel strip, and a bottom divider line.
  - Use Haze `hazeSource` on the scrolling list and `hazeEffect` on the chrome.

- [ ] **Step 2: Wire Home list**
  - Track `homeListState` position via the existing `LibraryScrollPosition` flow.
  - Apply a small top padding/spacer to let the first header visually scroll under the chrome.
  - Overlay `NestedScrollBlurChrome` above the Home list.

- [ ] **Step 3: Wire DrillDown track lists**
  - Reuse the same state calculation from `listState.toLibraryScrollPosition()`.
  - Overlay `NestedScrollBlurChrome` above album and artist track-list pages.

- [ ] **Step 4: Verify compile and tests**

Run: `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache`
Expected: build successful.

### Task 3: Roadmap and Progress Evidence

**Files:**
- Modify: `roadmap.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: verification output from Task 2.

- [ ] **Step 1: Mark roadmap item complete**
  - Change the Nested Scroll/Haze line from `[ ]` to `[x]` only after verification passes.

- [ ] **Step 2: Append progress handoff**
  - Record route, changed files, verification command/output, and next owner/blockers.

- [ ] **Step 3: Final diff check**

Run: `git diff --check`
Expected: no output, exit 0.
