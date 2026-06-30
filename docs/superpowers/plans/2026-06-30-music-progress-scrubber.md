# Music Progress Scrubber Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the expanded now-playing Miuix progress slider with a music-focused scrubber that supports one-shot tap seeking and one-shot drag-release seeking.

**Architecture:** Add a small shared Compose component backed by pure calculation helpers. The component owns transient scrub preview state so engine progress cannot fight the pointer while scrubbing, and `NowPlayingScreen` delegates progress UI to it. Tests cover helper math and the single-seek interaction reducer without requiring platform UI gesture tests.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, kotlin.test commonTest, existing Haus color/style primitives, existing `PlaybackController.seekTo(Long)` API.

## Global Constraints

- Route is `openspec+superpowers`.
- Shared-first: implementation belongs in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus` unless the task explicitly says otherwise.
- Do not add dependencies.
- Do not modify platform-specific source files for Android, iOS, JVM/macOS, or Swift.
- Do not add Material/Material3 UI usage to new code; use Compose foundation/layout/ui plus existing Haus constants.
- Preserve `PlaybackController`, `PlatformPlaybackEngine`, and `NowPlayingScreen` public call-site APIs.
- `NowPlayingBar` remains a passive mini progress indicator.
- Use TDD: write failing tests, run them red, implement, run green.
- Verification target for this plan: `./gradlew :shared:jvmTest --configuration-cache`, `./gradlew :shared:compileKotlinJvm --configuration-cache`, then `./init.sh` before final completion.

---

### Task 1: Scrubber Calculation and Interaction State

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/MusicProgressScrubberTest.kt`

**Interfaces:**
- Produces: `internal fun scrubberFractionForOffset(offsetX: Float, widthPx: Float): Float`
- Produces: `internal fun scrubberPositionForFraction(fraction: Float, durationMillis: Long): Long`
- Produces: `internal class MusicScrubInteractionState(positionMillis: Long, durationMillis: Long)` with methods/properties:
  - `val displayPositionMillis: Long`
  - `val displayFraction: Float`
  - `fun updatePlaybackPosition(positionMillis: Long, durationMillis: Long)`
  - `fun startScrub(fraction: Float)`
  - `fun updateScrub(fraction: Float)`
  - `fun finishScrub(): Long?`
  - `fun cancelScrub()`
- Consumes: no production UI code yet.

- [ ] **Step 1: Write failing tests for math helpers and reducer behavior**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/MusicProgressScrubberTest.kt`:

```kotlin
package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicProgressScrubberTest {
    @Test
    fun offsetMappingClampsToTrackBounds() {
        assertEquals(0f, scrubberFractionForOffset(offsetX = -10f, widthPx = 200f))
        assertEquals(0.25f, scrubberFractionForOffset(offsetX = 50f, widthPx = 200f))
        assertEquals(1f, scrubberFractionForOffset(offsetX = 240f, widthPx = 200f))
        assertEquals(0f, scrubberFractionForOffset(offsetX = 10f, widthPx = 0f))
    }

    @Test
    fun fractionMappingClampsToDurationBounds() {
        assertEquals(0L, scrubberPositionForFraction(fraction = -0.25f, durationMillis = 240_000L))
        assertEquals(60_000L, scrubberPositionForFraction(fraction = 0.25f, durationMillis = 240_000L))
        assertEquals(240_000L, scrubberPositionForFraction(fraction = 1.25f, durationMillis = 240_000L))
        assertEquals(0L, scrubberPositionForFraction(fraction = 0.5f, durationMillis = 0L))
    }

    @Test
    fun scrubbingPreviewIgnoresPlaybackProgressUntilRelease() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.4f)
        state.updatePlaybackPosition(positionMillis = 12_000L, durationMillis = 100_000L)

        assertEquals(40_000L, state.displayPositionMillis)
        assertEquals(0.4f, state.displayFraction)
        assertEquals(40_000L, state.finishScrub())
    }

    @Test
    fun dragReleaseProducesOneFinalSeekTargetAndThenReturnsToPlaybackState() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.2f)
        state.updateScrub(0.7f)
        val target = state.finishScrub()
        state.updatePlaybackPosition(positionMillis = target ?: -1L, durationMillis = 100_000L)

        assertEquals(70_000L, target)
        assertEquals(70_000L, state.displayPositionMillis)
        assertEquals(0.7f, state.displayFraction)
    }

    @Test
    fun canceledScrubDoesNotProduceSeekTarget() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.8f)
        state.cancelScrub()

        assertNull(state.finishScrub())
        assertEquals(10_000L, state.displayPositionMillis)
        assertEquals(0.1f, state.displayFraction)
    }
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache
```

Expected: FAIL because `scrubberFractionForOffset`, `scrubberPositionForFraction`, and `MusicScrubInteractionState` do not exist.

- [ ] **Step 3: Implement pure helpers and reducer state**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt` with the non-UI content first:

```kotlin
package com.eterocell.rhythhaus

internal fun scrubberFractionForOffset(offsetX: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return (offsetX / widthPx).coerceIn(0f, 1f)
}

internal fun scrubberPositionForFraction(fraction: Float, durationMillis: Long): Long {
    if (durationMillis <= 0L) return 0L
    return (durationMillis * fraction.coerceIn(0f, 1f)).toLong().coerceIn(0L, durationMillis)
}

internal class MusicScrubInteractionState(
    positionMillis: Long,
    durationMillis: Long,
) {
    private var playbackPositionMillis: Long = positionMillis.coerceAtLeast(0L)
    private var playbackDurationMillis: Long = durationMillis.coerceAtLeast(0L)
    private var scrubFraction: Float? = null

    val displayPositionMillis: Long
        get() = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
            ?: playbackPositionMillis.coerceIn(0L, playbackDurationMillis.takeIf { it > 0L } ?: Long.MAX_VALUE)

    val displayFraction: Float
        get() {
            val duration = playbackDurationMillis
            if (duration <= 0L) return 0f
            return scrubFraction ?: (playbackPositionMillis.coerceIn(0L, duration).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    fun updatePlaybackPosition(positionMillis: Long, durationMillis: Long) {
        playbackPositionMillis = positionMillis.coerceAtLeast(0L)
        playbackDurationMillis = durationMillis.coerceAtLeast(0L)
    }

    fun startScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun updateScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun finishScrub(): Long? {
        val target = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
        scrubFraction = null
        return target
    }

    fun cancelScrub() {
        scrubFraction = null
    }
}
```

If the exact implementation needs import-free changes to compile, keep the public/internal signatures exactly as above.

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 5: Commit Task 1**

Before committing, describe staged diffs to the user/session log. Then run:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/MusicProgressScrubberTest.kt
git commit -m "test: define music scrubber seek semantics"
```

---

### Task 2: Shared Compose Scrubber UI and NowPlayingScreen Integration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/MusicProgressScrubberTest.kt`

**Interfaces:**
- Consumes from Task 1: `scrubberFractionForOffset`, `scrubberPositionForFraction`, and `MusicScrubInteractionState`.
- Produces: `@Composable internal fun MusicProgressScrubber(positionMillis: Long, durationMillis: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier)`.
- `NowPlayingScreen` will call `MusicProgressScrubber(positionMillis = positionMillis, durationMillis = durationMillis, onSeek = playbackController::seekTo, modifier = Modifier.fillMaxWidth())`.

- [ ] **Step 1: Add a failing regression test for tap/drag seek emission semantics**

Append this test to `MusicProgressScrubberTest.kt` to lock the state-level contract used by the UI:

```kotlin
@Test
fun tapAndDragContractsEmitOnlyCommittedSeekTargets() {
    val duration = 200_000L
    val tapTarget = scrubberPositionForFraction(
        scrubberFractionForOffset(offsetX = 50f, widthPx = 200f),
        duration,
    )
    assertEquals(50_000L, tapTarget)

    val state = MusicScrubInteractionState(positionMillis = 0L, durationMillis = duration)
    val emittedTargets = mutableListOf<Long>()

    state.startScrub(scrubberFractionForOffset(offsetX = 20f, widthPx = 200f))
    state.updateScrub(scrubberFractionForOffset(offsetX = 80f, widthPx = 200f))
    state.updateScrub(scrubberFractionForOffset(offsetX = 160f, widthPx = 200f))
    state.finishScrub()?.let(emittedTargets::add)

    assertEquals(listOf(160_000L), emittedTargets)
}
```

- [ ] **Step 2: Run regression test before UI implementation**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache
```

Expected: PASS if Task 1 is correct. If it fails, fix the reducer before touching UI code.

- [ ] **Step 3: Implement `MusicProgressScrubber` composable in the existing file**

Extend `MusicProgressScrubber.kt` with Compose imports and the composable. Required behavior:

- Use `BoxWithConstraints` or `Box` with `onSizeChanged` to know rail width.
- Use `pointerInput(durationMillis, onSeek)` with `awaitEachGesture` to handle both tap and drag in one gesture path.
- On first down, compute and preview the target fraction immediately.
- If the pointer is released without movement, call `onSeek(...)` once for the down/up position.
- If the pointer moves, update preview on each move and call `onSeek(...)` once when all pointers are up.
- On cancellation, call `cancelScrub()` and do not call `onSeek`.
- Use a minimum touch height of `44.dp` and draw the visible rail smaller inside it.
- Use Haus colors: `HausLine`, `HausPulse`, `HausInk`, `HausMuted` as appropriate.
- Display elapsed/duration labels inside the component using existing `formatMillis`.

Implementation outline that must be followed but may be adjusted for exact Compose API names:

```kotlin
@Composable
internal fun MusicProgressScrubber(
    positionMillis: Long,
    durationMillis: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionState = remember { MusicScrubInteractionState(positionMillis, durationMillis) }
    interactionState.updatePlaybackPosition(positionMillis, durationMillis)

    var widthPx by remember { mutableStateOf(0f) }
    val displayFraction = interactionState.displayFraction
    val displayPositionMillis = interactionState.displayPositionMillis

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(durationMillis, onSeek) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun fractionFor(x: Float) = scrubberFractionForOffset(x, widthPx)
                        interactionState.startScrub(fractionFor(down.position.x))
                        var canceled = false
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                canceled = true
                                break
                            }
                            if (change.pressed) {
                                interactionState.updateScrub(fractionFor(change.position.x))
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        if (canceled) {
                            interactionState.cancelScrub()
                        } else {
                            interactionState.finishScrub()?.let(onSeek)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HausLine),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HausPulse),
            )
            Box(
                modifier = Modifier
                    .offset(x = ((widthPx * displayFraction).toDp() - 7.dp).coerceAtLeast(0.dp))
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(HausPulse)
                    .border(width = 2.dp, color = HausPaper, shape = CircleShape),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = formatMillis(displayPositionMillis), color = HausMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = formatMillis(durationMillis), color = HausMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
```

Important: `Float.toDp()` is available in `Density`; if needed wrap drawing in `with(LocalDensity.current) { ... }` and compute `thumbOffsetDp`. Do not introduce any non-existing imports. If `change.consume()` API differs in the current Compose version, inspect nearby code or Compose API and use the supported consumption method.

- [ ] **Step 4: Replace Miuix Slider in `NowPlayingScreen`**

Modify `NowPlayingScreen.kt`:

- Remove `import top.yukonga.miuix.kmp.basic.Slider` if unused.
- Remove local `progressFraction` if no longer used by this screen.
- Replace the current seek bar `Column` containing `Slider` and labels with:

```kotlin
MusicProgressScrubber(
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    onSeek = playbackController::seekTo,
    modifier = Modifier.fillMaxWidth(),
)
```

Keep all surrounding spacers, status text, and transport controls unchanged.

- [ ] **Step 5: Run focused tests and compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both PASS.

- [ ] **Step 6: Commit Task 2**

Before committing, describe staged diffs to the user/session log. Then run:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/MusicProgressScrubberTest.kt
git commit -m "feat: add music progress scrubber"
```

---

### Task 3: Final Verification and Handoff Evidence

**Files:**
- Modify: `progress.md`

**Interfaces:**
- Consumes: completed Task 1 and Task 2 commits.
- Produces: verification evidence and final handoff notes.

- [ ] **Step 1: Run full verification**

Run:

```bash
./init.sh
```

Expected: BUILD SUCCESSFUL. If it fails, fix only failures caused by this plan. If failure is unrelated or environmental, record the exact blocker in `progress.md` and final response.

- [ ] **Step 2: Update progress.md**

Append a handoff block:

```text
## Handoff - 2026-06-30 music progress scrubber

Route: openspec+superpowers
Owner: implementation
Input: User requested a music-player progress slider that supports single-click destination seeking and avoids multiple intermediate seeks while dragging.
Output: Added shared Compose `MusicProgressScrubber` with pure seek math and interaction-state tests; replaced expanded now-playing Miuix `Slider` with one-shot tap and drag-release seeking. `NowPlayingBar` remains passive progress.
Verification: <exact commands and outcomes>
Next owner: user for manual Android/iOS/macOS interaction validation with real playback.
Blockers: <none or exact blocker>
```

- [ ] **Step 3: Review final diff range**

Run:

```bash
git log --oneline -5
git status --short
git diff --stat HEAD~2..HEAD
```

Expected: only scrubber code/tests/docs/progress changes from this plan, plus any pre-existing unrelated working-tree files remain unstaged.

- [ ] **Step 4: Commit progress handoff**

Before committing, describe staged diffs to the user/session log. Then run:

```bash
git add progress.md docs/superpowers/specs/2026-06-30-music-progress-scrubber-design.md docs/superpowers/plans/2026-06-30-music-progress-scrubber.md
git commit -m "docs: record music progress scrubber workflow"
```

If the spec/plan docs were committed earlier, only stage `progress.md` and use:

```bash
git add progress.md
git commit -m "docs: record music progress scrubber verification"
```

- [ ] **Step 5: Final response**

Report:

- Commits created.
- Files changed.
- Verification commands and outcomes.
- Manual validation still recommended for real audio on Android/iOS/macOS.
