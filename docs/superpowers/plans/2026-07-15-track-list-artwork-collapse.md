# Track List Artwork Collapse Implementation Plan

> **SUPERSEDED — DO NOT EXECUTE:** Live macOS testing disproved this plan's nested-scroll architecture. The validated replacement uses one `LazyColumn` with aligned upper/sticky-lower artwork slices. Generate a new implementation plan only after the amended design in `docs/superpowers/specs/2026-07-15-track-list-artwork-collapse-design.md` is approved.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make album and artist Track List artwork collapse progressively with the track content, eliminating the transient gap while preserving no-artwork Miuix behavior.

**Architecture:** Add a pure pixel-geometry and signed-consumption policy, then adapt it to one remembered Compose nested-scroll connection used only by artwork-backed drill-down pages. Derive artwork height, list placement, and visual progress from one current snapshot; no-artwork pages continue using their existing Miuix scroll behavior unchanged.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Miuix 0.9.3, Kotlin Multiplatform common tests, Gradle.

## Global Constraints

- Shared Compose Multiplatform UI only.
- Expanded artwork height is the available drill-down width.
- Collapsed height is the system-bar top inset plus the existing 56 dp toolbar height.
- Upward movement collapses artwork one pixel per consumed pixel before list scrolling; positive post-scroll movement expands it only after the list reaches its start.
- Artwork height and track-content placement must derive from one clamped current geometry snapshot.
- Zero or inverted ranges render at collapsed height, consume no artwork-collapse scroll, and never divide by zero.
- No-artwork pages retain the existing Miuix large-title connection, glass chrome, content padding, divider, and title behavior.
- Preserve album/artist navigation, safe insets, 44 dp back target, title chips, artwork loading, track ordering, playback selection, scrollbar, and Now Playing spacing.
- Do not add parallax, snap, spring, fling, arbitrary multipliers, timing-based collapse, dependencies, persistence changes, platform integrations, or Windows/Linux support.
- Preserve the user's existing `roadmap.md` edits, including item 22; modify only item 21 during completion evidence.

---

### Task 1: Pure artwork-collapse geometry and consumption

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`

**Interfaces:**
- Produces: `internal data class ArtworkCollapseGeometry(val expandedHeightPx: Float, val collapsedHeightPx: Float)`.
- Produces: `internal data class ArtworkCollapseSnapshot(val offsetPx: Float, val headerHeightPx: Float, val progress: Float)`.
- Produces: `internal data class ArtworkCollapseConsumption(val offsetPx: Float, val consumedY: Float)`.
- Produces: `ArtworkCollapseGeometry.snapshot(offsetPx: Float)`, `consumeUpward(offsetPx: Float, availableY: Float)`, and `consumeDownward(offsetPx: Float, availableY: Float)`.
- Consumes: only Kotlin `Float` arithmetic; no Compose state or Miuix types.

- [ ] **Step 1: Write failing geometry and signed-consumption tests**

Create `ArtworkCollapseTest.kt`:

```kotlin
package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ArtworkCollapseTest {
    private val geometry = ArtworkCollapseGeometry(
        expandedHeightPx = 320f,
        collapsedHeightPx = 80f,
    )

    @Test
    fun snapshotKeepsHeaderAndContentOnOneClampedGeometry() {
        assertEquals(ArtworkCollapseSnapshot(0f, 320f, 0f), geometry.snapshot(0f))
        assertEquals(ArtworkCollapseSnapshot(120f, 200f, 0.5f), geometry.snapshot(120f))
        assertEquals(ArtworkCollapseSnapshot(240f, 80f, 1f), geometry.snapshot(500f))
    }

    @Test
    fun upwardMovementIsConsumedOneForOneUntilCollapsed() {
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 50f, consumedY = -50f),
            geometry.consumeUpward(offsetPx = 0f, availableY = -50f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 240f, consumedY = -20f),
            geometry.consumeUpward(offsetPx = 220f, availableY = -80f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 240f, consumedY = 0f),
            geometry.consumeUpward(offsetPx = 240f, availableY = -20f),
        )
    }

    @Test
    fun downwardMovementExpandsSymmetrically() {
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 170f, consumedY = 50f),
            geometry.consumeDownward(offsetPx = 220f, availableY = 50f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 0f, consumedY = 20f),
            geometry.consumeDownward(offsetPx = 20f, availableY = 80f),
        )
    }

    @Test
    fun invalidRangesRenderCollapsedAndConsumeNothing() {
        listOf(
            ArtworkCollapseGeometry(80f, 80f),
            ArtworkCollapseGeometry(60f, 80f),
        ).forEach { invalid ->
            assertEquals(ArtworkCollapseSnapshot(0f, 80f, 1f), invalid.snapshot(40f))
            assertEquals(ArtworkCollapseConsumption(0f, 0f), invalid.consumeUpward(0f, -30f))
            assertEquals(ArtworkCollapseConsumption(0f, 0f), invalid.consumeDownward(0f, 30f))
        }
    }

    @Test
    fun resizedGeometryClampsExistingOffsetImmediately() {
        val narrowed = ArtworkCollapseGeometry(expandedHeightPx = 200f, collapsedHeightPx = 80f)

        assertEquals(ArtworkCollapseSnapshot(120f, 80f, 1f), narrowed.snapshot(offsetPx = 220f))
    }
}
```

- [ ] **Step 2: Run the tests and verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Expected: Kotlin compilation fails because the artwork-collapse types do not exist.

- [ ] **Step 3: Implement the minimal pure policy**

Create `ArtworkCollapse.kt` with these exact value types and semantics:

```kotlin
package com.eterocell.rhythhaus.library.ui

internal data class ArtworkCollapseSnapshot(
    val offsetPx: Float,
    val headerHeightPx: Float,
    val progress: Float,
)

internal data class ArtworkCollapseConsumption(
    val offsetPx: Float,
    val consumedY: Float,
)

internal data class ArtworkCollapseGeometry(
    val expandedHeightPx: Float,
    val collapsedHeightPx: Float,
) {
    val collapseRangePx: Float
        get() = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(0f)

    fun snapshot(offsetPx: Float): ArtworkCollapseSnapshot {
        if (collapseRangePx == 0f) {
            return ArtworkCollapseSnapshot(offsetPx = 0f, headerHeightPx = collapsedHeightPx, progress = 1f)
        }
        val clampedOffset = offsetPx.coerceIn(0f, collapseRangePx)
        return ArtworkCollapseSnapshot(
            offsetPx = clampedOffset,
            headerHeightPx = expandedHeightPx - clampedOffset,
            progress = clampedOffset / collapseRangePx,
        )
    }

    fun consumeUpward(offsetPx: Float, availableY: Float): ArtworkCollapseConsumption {
        val before = snapshot(offsetPx).offsetPx
        if (availableY >= 0f || collapseRangePx == 0f) return ArtworkCollapseConsumption(before, 0f)
        val after = (before - availableY).coerceAtMost(collapseRangePx)
        return ArtworkCollapseConsumption(offsetPx = after, consumedY = -(after - before))
    }

    fun consumeDownward(offsetPx: Float, availableY: Float): ArtworkCollapseConsumption {
        val before = snapshot(offsetPx).offsetPx
        if (availableY <= 0f || collapseRangePx == 0f) return ArtworkCollapseConsumption(before, 0f)
        val after = (before - availableY).coerceAtLeast(0f)
        return ArtworkCollapseConsumption(offsetPx = after, consumedY = before - after)
    }
}
```

- [ ] **Step 4: Run focused GREEN tests**

Run the same command from Step 2.

Expected: all `ArtworkCollapseTest` tests pass.

- [ ] **Step 5: Review the policy boundaries**

Confirm negative input is accepted only by `consumeUpward`, positive input only by `consumeDownward`, consumed values preserve the gesture sign, no consumption exceeds the remaining range, invalid ranges report collapsed geometry, and no Compose/Miuix dependency entered this file.

- [ ] **Step 6: Commit the policy and tests**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt
GIT_MASTER=1 git commit -m "fix: define coordinated artwork collapse geometry" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 2: Compose nested-scroll adapter and drill-down wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt:67-157`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt:68-223`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`

**Interfaces:**
- Produces: `internal enum class DrillDownScrollOwner { Artwork, Miuix }` and `internal fun drillDownScrollOwner(hasArtwork: Boolean): DrillDownScrollOwner`.
- Produces: `@Composable internal fun rememberArtworkCollapseState(geometry: ArtworkCollapseGeometry): ArtworkCollapseState`.
- Produces: `ArtworkCollapseState.snapshot: ArtworkCollapseSnapshot` and `ArtworkCollapseState.nestedScrollConnection: NestedScrollConnection`.
- Changes: `DrillDownMiuixScrollChrome` accepts `artworkCollapseSnapshot: ArtworkCollapseSnapshot?`; a non-null snapshot identifies artwork mode and owns its geometry/progress.
- Preserves: the existing `ScrollBehavior` parameter for no-artwork `TopAppBar` behavior.

- [ ] **Step 1: Add failing branch-selection and coupling tests**

Extend `ArtworkCollapseTest.kt`:

```kotlin
@Test
fun artworkPagesUseAppOwnedScrollWhileFallbackPagesUseMiuix() {
    assertEquals(DrillDownScrollOwner.Artwork, drillDownScrollOwner(hasArtwork = true))
    assertEquals(DrillDownScrollOwner.Miuix, drillDownScrollOwner(hasArtwork = false))
}

@Test
fun listTopAndChromeHeightUseTheSameSnapshotValue() {
    val snapshot = geometry.snapshot(offsetPx = 75f)

    assertEquals(snapshot.headerHeightPx, artworkListTopPaddingPx(snapshot))
    assertEquals(snapshot.headerHeightPx, artworkChromeHeightPx(snapshot))
}
```

Add pure seams to the production plan rather than inspecting source text:

```kotlin
internal enum class DrillDownScrollOwner { Artwork, Miuix }

internal fun drillDownScrollOwner(hasArtwork: Boolean): DrillDownScrollOwner =
    if (hasArtwork) DrillDownScrollOwner.Artwork else DrillDownScrollOwner.Miuix

internal fun artworkListTopPaddingPx(snapshot: ArtworkCollapseSnapshot): Float = snapshot.headerHeightPx

internal fun artworkChromeHeightPx(snapshot: ArtworkCollapseSnapshot): Float = snapshot.headerHeightPx
```

- [ ] **Step 2: Run the new tests and verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Expected: compilation fails because `DrillDownScrollOwner`, `drillDownScrollOwner`, and the shared-geometry seams do not exist.

- [ ] **Step 3: Add the branch and shared-geometry seams**

Add the exact enum/functions from Step 1 to `ArtworkCollapse.kt`, keeping them pure. Run the focused test command and expect GREEN before adding Compose state.

- [ ] **Step 4: Add remembered Compose state and connection**

In `ArtworkCollapse.kt`, add Compose nested-scroll imports and implement a stable holder whose offset is the only mutable value:

```kotlin
internal class ArtworkCollapseState internal constructor(
    private val offsetState: androidx.compose.runtime.MutableState<Float>,
    private val geometryState: androidx.compose.runtime.State<ArtworkCollapseGeometry>,
) {
    val snapshot: ArtworkCollapseSnapshot
        get() = geometryState.value.snapshot(offsetState.value)

    val nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection =
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                val result = geometryState.value.consumeUpward(offsetState.value, available.y)
                offsetState.value = result.offsetPx
                return androidx.compose.ui.geometry.Offset(0f, result.consumedY)
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                val result = geometryState.value.consumeDownward(offsetState.value, available.y)
                offsetState.value = result.offsetPx
                return androidx.compose.ui.geometry.Offset(0f, result.consumedY)
            }
        }
}
```

Provide the composable adapter using `rememberUpdatedState` so the remembered connection sees current geometry. Derive `snapshot` from the latest geometry immediately, and use `SideEffect` only to persist any resize clamp:

```kotlin
@Composable
internal fun rememberArtworkCollapseState(
    geometry: ArtworkCollapseGeometry,
): ArtworkCollapseState {
    val offsetState = remember { mutableStateOf(0f) }
    val geometryState = rememberUpdatedState(geometry)
    val state = remember { ArtworkCollapseState(offsetState, geometryState) }
    val clampedOffset = geometry.snapshot(offsetState.value).offsetPx
    SideEffect {
        if (offsetState.value != clampedOffset) offsetState.value = clampedOffset
    }
    return state
}
```

Use imports rather than fully qualified names in the final source. Do not add `as any`, suppression annotations, or a second mutable geometry value.

- [ ] **Step 5: Wire exactly one scroll owner in `DrillDownView`**

Inside `BoxWithConstraints`, derive:

```kotlin
val hasTopBarArtwork = topBarArtworkTrack != null
val collapsedChromeHeight = drillDownStatusBarHeight + NestedScrollChromeToolbarHeight
val density = LocalDensity.current
val artworkGeometry = ArtworkCollapseGeometry(
    expandedHeightPx = with(density) { maxWidth.toPx() },
    collapsedHeightPx = with(density) { collapsedChromeHeight.toPx() },
)
val artworkCollapseState = rememberArtworkCollapseState(artworkGeometry)
val artworkSnapshot = if (hasTopBarArtwork) artworkCollapseState.snapshot else null
val scrollOwner = drillDownScrollOwner(hasTopBarArtwork)
```

Make `NestedScrollChromeToolbarHeight` internal so `LibraryDetailContent.kt` can use the same collapsed geometry. Apply exactly one connection:

```kotlin
val drillDownNestedScrollConnection = when (scrollOwner) {
    DrillDownScrollOwner.Artwork -> artworkCollapseState.nestedScrollConnection
    DrillDownScrollOwner.Miuix -> miuixScrollBehavior.nestedScrollConnection
}
```

Replace the static artwork `maxWidth + 20.dp` reservation with the current shared snapshot converted to `Dp`:

```kotlin
val drillDownTopPadding = artworkSnapshot
    ?.let { with(LocalDensity.current) { artworkListTopPaddingPx(it).toDp() } }
    ?: (drillDownStatusBarHeight + DrillDownMiuixScrollContentTopPadding)
```

Keep the current 20 dp horizontal list padding, 18 dp item spacing, rows, scrollbar, callbacks, and Now Playing spacer. Pass `artworkSnapshot` into the chrome.

- [ ] **Step 6: Make chrome consume the shared snapshot**

Change `DrillDownMiuixScrollChrome` to accept:

```kotlin
artworkCollapseSnapshot: ArtworkCollapseSnapshot? = null,
```

Set artwork mode from the existing artwork identity/bytes, require a non-null snapshot from the artwork branch, and derive:

```kotlin
val artworkProgress = artworkCollapseSnapshot?.progress ?: 0f
val collapsedFraction = if (hasArtwork) artworkProgress else scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
```

Inside `BoxWithConstraints`, use the shared snapshot height for artwork:

```kotlin
val chromeHeight = artworkCollapseSnapshot
    ?.let { with(LocalDensity.current) { artworkChromeHeightPx(it).toDp() } }
    ?: collapsedChromeHeight
```

Preserve `LazyTrackArtworkImage`, crop, scrim, title-chip formulas, background fade animation, safe-start padding, 44 dp back target, and no-artwork `TopAppBar` title/large-title/divider behavior. Do not use Miuix `collapsedFraction` for any artwork-owned geometry or visual progress.

- [ ] **Step 7: Run focused GREEN and Library regression tests**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: tests and compilation pass. If Compose API signatures differ, correct them without weakening the pure behavioral assertions.

- [ ] **Step 8: Review the integrated behavior**

Confirm album and artist routes still share `DrillDownView`; no `LibraryRoutes.kt` change is needed. Verify only one connection is attached per branch, positive expansion occurs only in post-scroll, resize clamping affects chrome and list in the same composition, no fixed expanded artwork reservation remains, and navigation/playback/scrollbar/artwork loading code is unchanged.

- [ ] **Step 9: Commit the adapter, UI wiring, and tests**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt
GIT_MASTER=1 git commit -m "fix: coordinate track list artwork collapse" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 3: Full verification, visual QA, and durable evidence

**Files:**
- Modify: `openspec/changes/track-list-artwork-collapse/tasks.md`
- Modify: `roadmap.md` (item 21 only; preserve item 22 and all user edits)
- Modify: `progress.md`
- Create: `.superpowers/sdd/track-list-artwork-collapse-task-1-report.md`
- Create: `.superpowers/sdd/track-list-artwork-collapse-task-2-report.md`
- Create: `.superpowers/sdd/track-list-artwork-collapse-task-3-report.md`
- Preserve: `docs/superpowers/specs/2026-07-15-track-list-artwork-collapse-design.md`
- Preserve: `docs/superpowers/plans/2026-07-15-track-list-artwork-collapse.md`

**Interfaces:**
- Consumes: completed Task 1 and Task 2 commits and review evidence.
- Produces: strict OpenSpec completion state, exact build/test evidence, roadmap completion, and a handoff for item 22's separate workflow.

- [ ] **Step 1: Run strict OpenSpec validation**

```bash
openspec validate track-list-artwork-collapse --strict
```

Expected: `Change 'track-list-artwork-collapse' is valid`.

- [ ] **Step 2: Run supported automated verification**

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: record actual outcomes. If the known common-test `Thread` references still block iOS, record exact current file/line output and do not fix that unrelated issue.

- [ ] **Step 3: Run diagnostics and diff hygiene**

Run Kotlin LSP diagnostics on every changed Kotlin file if the server is available. If it remains unavailable by prior user decision, record that and rely on executable Gradle compilation/tests.

```bash
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Expected: no whitespace errors. The only scope outside source/tests is approved planning and evidence; item 22 remains unchecked and unchanged.

- [ ] **Step 4: Perform visual QA**

Use the repository visual-QA workflow. Where a runnable desktop or device target is available, exercise:

- album and artist pages with artwork;
- forward collapse, fully collapsed list scrolling, and reverse expansion;
- no visible gap at partial or full collapse;
- no-artwork fallback behavior;
- compact and wide window widths;
- title chips, background fade, back target, scrollbar, and Now Playing overlay.

If runtime capture is unavailable, perform the required source-level visual reviews and explicitly retain runtime validation as a manual limitation.

- [ ] **Step 5: Run task and final review gates**

For each SDD task, obtain specification-compliance review first and code-quality review second. After all implementation tasks, run the repository post-implementation review/Oracle gate. Fix only findings introduced by this change, rerun affected checks, and record final verdicts.

- [ ] **Step 6: Update durable evidence without overwriting the roadmap**

Mark OpenSpec tasks complete only after evidence exists. Change only roadmap item 21 from unchecked to a concise completed description of coordinated pinned collapse, verification, blockers, and manual visual-QA limits. Preserve item 22 byte-for-byte. Prepend `progress.md` with route, owner, input, output, exact commands/results, changed files, review verdicts, next owner, blockers, and commits. Preserve task reports under change-specific names rather than overwriting generic SDD reports.

- [ ] **Step 7: Revalidate artifacts and scope**

```bash
openspec validate track-list-artwork-collapse --strict
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Expected: OpenSpec and diff hygiene pass; status contains only intended completion evidence before the final commit.

- [ ] **Step 8: Commit completion evidence**

Stage the change-specific artifacts only. Do not stage unrelated edits discovered during execution.

```bash
GIT_MASTER=1 git add \
  openspec/changes/track-list-artwork-collapse/tasks.md \
  roadmap.md \
  progress.md \
  .superpowers/sdd/track-list-artwork-collapse-task-1-report.md \
  .superpowers/sdd/track-list-artwork-collapse-task-2-report.md \
  .superpowers/sdd/track-list-artwork-collapse-task-3-report.md
GIT_MASTER=1 git commit -m "docs: complete track list artwork collapse" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

- [ ] **Step 9: Review final state and hand off to item 22**

```bash
GIT_MASTER=1 git status --short
GIT_MASTER=1 git log --oneline -5
```

Expected: item 21 artifacts and implementation are committed, no implementation task remains open, and the next owner starts a separate brainstorming/spec/plan cycle for roadmap item 22 rather than mixing it into this change.
