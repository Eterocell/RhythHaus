# Restart Current Track Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make track-row selection restart the active track from zero or play a different track from the exact visible list, without changing dedicated transport controls.

**Architecture:** Add a state-aware `PlaybackController.restartCurrentTrack()` that performs ordered engine operations without replacing the queue. Add one shared Library selection helper that validates the clicked ID and chooses between restart and visible-queue replacement. Keep Compose surfaces declarative by routing home, drill-down, and Search row clicks through that helper while retaining separate play/pause callbacks.

**Tech Stack:** Kotlin 2.4.0, Kotlin Multiplatform, Compose Multiplatform 1.11.1, kotlinx.coroutines 1.11.0, Kotlin common tests, Gradle.

## Global Constraints

- Selecting the active track SHALL restart it at position zero and ensure playback.
- Selecting another track SHALL use the exact visible-list membership and ordering as the queue and auto-play the selected track.
- Current-track selection SHALL preserve the existing queue even when the track is clicked from another list.
- Library home uses `snapshot.tracks`; album and artist detail use their rendered track lists; Search uses the current filtered results.
- Invalid or stale selected IDs SHALL be a no-op rather than falling back to the first queue item.
- Preserve repeat and shuffle mode values.
- Preserve dedicated play/pause, skip, completion, Now Playing, media-session, and platform playback-engine behavior.
- Do not change dependencies, persistence, database schemas, navigation, styling, or Windows/Linux scope.

---

### Task 1: Playback controller restart command

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:232-304`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`

**Interfaces:**
- Produces: `fun PlaybackController.restartCurrentTrack()`.
- Preserves: `togglePlayPause()`, `setQueue(...)`, repeat/shuffle APIs, and `PlatformPlaybackEngine`.
- Consumes: existing `playWhenLoaded`, `loadSelected(...)`, `launchEngineAction(...)`, and engine mutex serialization.

- [ ] **Step 1: Extend the recording test engine**

In `PlaybackControllerTest.kt`, extend or add a test engine that records ordered events:

```kotlin
private sealed interface EngineEvent {
    data class Load(val trackId: String) : EngineEvent
    data class Seek(val positionMillis: Long) : EngineEvent
    data object Play : EngineEvent
    data object Pause : EngineEvent
}
```

Its `load`, `seekTo`, `play`, and `pause` methods append the corresponding event before notifying the listener. Provide a suspendable load gate for the loading-state test.

- [ ] **Step 2: Write failing restart tests**

Add tests proving:

```kotlin
@Test fun restartCurrentPlayingTrackSeeksToZeroBeforePlaying()
@Test fun restartCurrentPausedTrackSeeksToZeroAndPlays()
@Test fun restartCurrentStoppedTrackSeeksToZeroAndPlays()
@Test fun restartCurrentLoadingTrackWaitsForLoadWithoutSeekingStaleEngineItem()
@Test fun restartCurrentErrorTrackReloadsAndAutoplays()
@Test fun restartCurrentTrackPreservesQueueRepeatAndShuffle()
@Test fun restartWithoutCurrentTrackIsNoOp()
@Test fun togglePlayPauseStillDoesNotSeek()
```

For loaded-state tests, first establish a track and a non-zero position. Assert that the events emitted after restart are exactly `Seek(0L), Play`. For loading, block `load`, call restart, assert no `Seek`, release load, and assert playback begins. For preservation, compare queue IDs, repeat mode, and shuffle mode before and after restart.

- [ ] **Step 3: Run tests and verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: compilation fails because `restartCurrentTrack()` does not exist, or the new behavior assertions fail.

- [ ] **Step 4: Implement the minimal restart method**

Add beside the existing transport methods:

```kotlin
fun restartCurrentTrack() {
    val current = _state.value.currentTrack ?: return
    _state.value = _state.value.copy(positionMillis = 0L, error = null)
    when (_state.value.status) {
        PlaybackStatus.Loading -> playWhenLoaded = true
        PlaybackStatus.Idle,
        PlaybackStatus.Error,
        -> loadSelected(current, autoPlay = true)
        else -> launchEngineAction {
            engine.seekTo(0L)
            engine.play()
        }
    }
}
```

Keep seek and play in the same action. Do not alter `togglePlayPause()` or the platform interface.

- [ ] **Step 5: Run tests and verify GREEN**

Run the command from Step 3. Expected: all `PlaybackControllerTest` tests pass.

- [ ] **Step 6: Commit controller behavior with its tests**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt
GIT_MASTER=1 git commit -m "feat: add current track restart command" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 2: Shared Library playback-selection policy

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt`

**Interfaces:**
- Consumes: `PlaybackController`, `PlayableTrack`, `setQueue(...)`, `play()`, and `restartCurrentTrack()`.
- Produces:

```kotlin
internal fun selectLibraryTrackForPlayback(
    playbackController: PlaybackController,
    visibleQueue: List<PlayableTrack>,
    selectedTrackId: String,
)
```

- [ ] **Step 1: Write failing selection-policy tests**

Add tests proving:

```kotlin
@Test fun currentSelectionRestartsWithoutReplacingExistingQueue()
@Test fun differentSelectionReplacesQueueWithVisibleOrderAndAutoplays()
@Test fun differentSelectionPreservesRepeatAndShuffleModes()
@Test fun invalidSelectionDoesNotFallBackToFirstVisibleTrack()
@Test fun emptyVisibleQueueIsNoOp()
```

The current-selection test must initialize the controller with a queue different from `visibleQueue`, seek to a non-zero position, call the helper, and assert that the original queue remains while position becomes zero and status becomes playing.

- [ ] **Step 2: Run tests and verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache
```

Expected: compilation fails because `selectLibraryTrackForPlayback` does not exist.

- [ ] **Step 3: Implement the minimal shared helper**

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController

internal fun selectLibraryTrackForPlayback(
    playbackController: PlaybackController,
    visibleQueue: List<PlayableTrack>,
    selectedTrackId: String,
) {
    if (visibleQueue.none { it.id == selectedTrackId }) return
    if (playbackController.state.value.currentTrack?.id == selectedTrackId) {
        playbackController.restartCurrentTrack()
    } else {
        playbackController.setQueue(visibleQueue, selectedTrackId)
        playbackController.play()
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run the command from Step 2. Expected: all selection-policy tests pass.

- [ ] **Step 5: Commit the helper with its tests**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt
GIT_MASTER=1 git commit -m "feat: centralize library track selection playback" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 3: Apply selection policy to Library lists

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt:159-172`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt:156-162`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt:87-168`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt:52-65,110-118,147-151`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: `selectLibraryTrackForPlayback(...)` and each surface's rendered track list.
- Produces: separate `onTrackClick: (Track) -> Unit` and `onPlayPause: (Track) -> Unit` responsibilities in drill-down content.
- Preserves: selection state, Now Playing expansion, dedicated transport toggle, navigation, and visuals.

- [ ] **Step 1: Add a callback-separation regression seam**

Add an internal pure policy to `LibraryDetailContent.kt`:

```kotlin
internal enum class DrillDownTrackAction { SelectTrack, ToggleTransport }

internal fun drillDownTrackAction(isTransportControl: Boolean): DrillDownTrackAction =
    if (isTransportControl) DrillDownTrackAction.ToggleTransport else DrillDownTrackAction.SelectTrack
```

Add a failing `LibraryNavigationTest` asserting row actions are `SelectTrack` and transport actions are `ToggleTransport`. This seam documents the split without introducing screenshot or Compose UI test infrastructure.

- [ ] **Step 2: Run the focused test and verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected: compilation fails because the action policy does not exist.

- [ ] **Step 3: Rewire Library home**

Replace the inline `setQueue` plus `togglePlayPause` block with:

```kotlin
selectLibraryTrackForPlayback(
    playbackController = playbackController,
    visibleQueue = snapshot.tracks.map { it.toPlayableTrack() },
    selectedTrackId = track.id,
)
```

Retain `onTrackSelected(track.id)` and existing row selection visuals.

- [ ] **Step 4: Separate drill-down row selection and transport**

Rename `LibraryAppShell.playPauseFromTracks` to a selection-focused function using the shared helper. Pass it through `LibraryRouteContent` as `onTrackClickFromTracks`.

In `DrillDownView`, add separate callbacks:

```kotlin
onTrackClick: (Track) -> Unit,
onPlayPause: (Track) -> Unit,
```

Track rows invoke `onTrackClick(track)`. The `NowPlayingBar` continues invoking `onPlayPause(currentTrack)`. At route wiring, `onPlayPause` must keep the existing `playbackController.togglePlayPause()` behavior and must not call the selection helper.

- [ ] **Step 5: Run focused tests and compile**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: all tests and compilation pass.

- [ ] **Step 6: Commit Library UI wiring with its regression test**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
GIT_MASTER=1 git commit -m "feat: apply restart selection to library lists" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 4: Apply visible-result queue behavior to Search

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt:62-71,173-183`

**Interfaces:**
- Consumes: `selectLibraryTrackForPlayback(...)`.
- Preserves: filtering, displayed result order, selected/equalizer visuals, and immediate dismissal.

- [ ] **Step 1: Replace the Search click policy**

Capture the current filtered results as the visible queue at click time:

```kotlin
selectLibraryTrackForPlayback(
    playbackController = playbackController,
    visibleQueue = filtered.map { it.toPlayableTrack() },
    selectedTrackId = track.id,
)
onDismiss()
```

Remove the complete-library queue construction and direct `play()` call.

- [ ] **Step 2: Run selection tests and shared compilation**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' \
  --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: tests and compilation pass.

- [ ] **Step 3: Commit Search wiring**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt
GIT_MASTER=1 git commit -m "feat: queue visible search results on selection" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 5: Full verification and durable evidence

**Files:**
- Modify: `openspec/changes/restart-current-track-selection/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`
- Preserve: approved spec and plan artifacts.

**Interfaces:**
- Consumes: completed implementation tasks and review reports.
- Produces: accurate OpenSpec task state, roadmap completion, and handoff evidence.

- [ ] **Step 1: Run strict OpenSpec validation**

```bash
openspec validate restart-current-track-selection --strict
```

Expected: `Change 'restart-current-track-selection' is valid`.

- [ ] **Step 2: Run shared JVM and platform builds**

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: pass with zero related test or compilation failures.

- [ ] **Step 3: Run iOS checks**

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Record actual results. If the existing common-test `Thread` references still block iOS compilation, record exact file/line output and do not fix them in this change.

- [ ] **Step 4: Run hygiene and scope checks**

```bash
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Confirm no platform-engine, dependency, database, persistence, navigation, styling, Windows, or Linux changes are present.

- [ ] **Step 5: Complete final reviews**

Run task review after each implementation task and a whole-change review after all implementation commits. Resolve every Critical or Important finding and re-run covering tests.

- [ ] **Step 6: Update durable evidence**

Mark OpenSpec tasks complete only when evidence exists. Update roadmap item 17 with the active-track restart and visible-list queue behavior. Add a top `progress.md` handoff containing route, scope, RED/GREEN commands, final verification outcomes, changed files, blockers, and manual QA limits.

- [ ] **Step 7: Revalidate and commit evidence**

```bash
openspec validate restart-current-track-selection --strict
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git add \
  openspec/changes/restart-current-track-selection/tasks.md \
  roadmap.md \
  progress.md
GIT_MASTER=1 git commit -m "docs: complete restart current track selection" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

- [ ] **Step 8: Review final state**

```bash
GIT_MASTER=1 git status --short
GIT_MASTER=1 git log --oneline -6
```

Expected: worktree clean and atomic feature/evidence commits present.
