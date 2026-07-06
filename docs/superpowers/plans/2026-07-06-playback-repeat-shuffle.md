# Playback Repeat and Shuffle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add shared repeat and shuffle playback modes and expose them as controls on `NowPlayingScreen`.

**Architecture:** Playback mode state and effective queue-order decisions live in shared common code (`PlaybackState` / `PlaybackController`). Platform engines remain single-current-item players; UI and platform media controls call controller methods so previous/next/completion semantics cannot drift. Shuffle is represented by controller-owned effective order state; visible library lists remain in their existing order.

**Tech Stack:** Kotlin Multiplatform, shared commonMain/commonTest, Compose Multiplatform, Material vector icons already available through `compose-material-icons-extended`, Gradle configuration cache.

## Global Constraints

- Repeat modes are exactly: single-track repeat, playlist repeat, play-current-song-then-stop, and play-current-list-then-stop.
- Shuffle modes are exactly: off and shuffle songs inside the current playback list.
- Default playback mode state is `RepeatMode.StopAfterQueue` and `ShuffleMode.Off`.
- `StopAfterCurrent` completion remains on the current track at its end, publishes a non-playing state, does not reset position to 0, and does not advance.
- `StopAfterQueue` completion advances through non-final effective tracks and stops on the final effective track at its end without wrapping.
- Manual previous/next wraps only in `RepeatMode.RepeatPlaylist`; all other repeat modes no-op at missing boundaries.
- Manual previous/next remains usable for adjacent tracks in `RepeatMode.RepeatOne` and `RepeatMode.StopAfterCurrent`.
- Enabling shuffle immediately generates an effective order containing every queued track exactly once while keeping the current track active.
- Disabling shuffle returns navigation to original queue order from the current track.
- Queue replacement while shuffled regenerates the effective order and preserves selected/current track when possible.
- Shuffle changes playback order only; it does not reorder visible library or browse lists.
- Add controls only to shared `NowPlayingScreen`; do not add repeat/shuffle controls to `NowPlayingBar`, system notification, lock screen, or platform-specific UI.
- Do not add dependencies.
- Preserve scanner, library database, artwork, metadata, theme, navigation architecture, and platform playback engines except for their existing calls into shared skip callbacks.

---

### Task 1: Add playback mode model, controller APIs, and default-state tests

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:64-190`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`

**Interfaces:**
- Produces: `enum class RepeatMode { RepeatOne, RepeatPlaylist, StopAfterCurrent, StopAfterQueue }`
- Produces: `enum class ShuffleMode { Off, On }`
- Produces: `PlaybackState.repeatMode: RepeatMode`
- Produces: `PlaybackState.shuffleMode: ShuffleMode`
- Produces: `PlaybackController.setRepeatMode(mode: RepeatMode)`
- Produces: `PlaybackController.cycleRepeatMode()` cycling `StopAfterQueue -> RepeatPlaylist -> RepeatOne -> StopAfterCurrent -> StopAfterQueue`
- Produces: `PlaybackController.setShuffleMode(mode: ShuffleMode)`
- Produces: `PlaybackController.toggleShuffleMode()`
- Consumes: existing `PlaybackController`, `PlaybackState`, and `FakePlaybackEngine` from `Playback.kt`

- [ ] **Step 1: Write failing default and mutation tests**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt` with these initial tests and helper:

```kotlin
package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackControllerTest {
    @Test
    fun playbackStateDefaultsToStopAfterQueueAndShuffleOff() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCanSetRepeatAndShuffleModes() {
        val controller = PlaybackController(FakePlaybackEngine())

        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.setShuffleMode(ShuffleMode.On)

        assertEquals(RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCyclesRepeatModeInSpecifiedOrder() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.StopAfterCurrent, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
    }

    @Test
    fun controllerTogglesShuffleMode() {
        val controller = PlaybackController(FakePlaybackEngine())

        controller.toggleShuffleMode()
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        controller.toggleShuffleMode()
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: FAIL because `RepeatMode`, `ShuffleMode`, and controller mode APIs do not exist yet.

- [ ] **Step 3: Add enums and state properties**

In `Playback.kt`, add the enums immediately after `PlaybackStatus`:

```kotlin
enum class RepeatMode {
    RepeatOne,
    RepeatPlaylist,
    StopAfterCurrent,
    StopAfterQueue,
}

enum class ShuffleMode {
    Off,
    On,
}
```

Update `PlaybackState` constructor to include defaults after `durationMillis` and before `error`:

```kotlin
val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
val shuffleMode: ShuffleMode = ShuffleMode.Off,
```

When `setQueue()` creates a new `PlaybackState` for an empty or selected queue, preserve the existing mode values rather than resetting them. For the empty queue branch, use:

```kotlin
_state.value = PlaybackState(
    queue = tracks,
    repeatMode = _state.value.repeatMode,
    shuffleMode = _state.value.shuffleMode,
)
```

For the selected branch, add:

```kotlin
repeatMode = _state.value.repeatMode,
shuffleMode = _state.value.shuffleMode,
```

- [ ] **Step 4: Add controller mode APIs**

In `PlaybackController`, add public methods before `play()`:

```kotlin
fun setRepeatMode(mode: RepeatMode) {
    _state.value = _state.value.copy(repeatMode = mode)
}

fun cycleRepeatMode() {
    setRepeatMode(
        when (_state.value.repeatMode) {
            RepeatMode.StopAfterQueue -> RepeatMode.RepeatPlaylist
            RepeatMode.RepeatPlaylist -> RepeatMode.RepeatOne
            RepeatMode.RepeatOne -> RepeatMode.StopAfterCurrent
            RepeatMode.StopAfterCurrent -> RepeatMode.StopAfterQueue
        },
    )
}

fun setShuffleMode(mode: ShuffleMode) {
    _state.value = _state.value.copy(shuffleMode = mode)
}

fun toggleShuffleMode() {
    setShuffleMode(
        when (_state.value.shuffleMode) {
            ShuffleMode.Off -> ShuffleMode.On
            ShuffleMode.On -> ShuffleMode.Off
        },
    )
}
```

Task 2 will replace the simple shuffle setter with order-regeneration logic; keep this task minimal.

- [ ] **Step 5: Run tests to verify they pass**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 6: Commit Task 1**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt
git commit -m "feat: add playback mode state"
```

---

### Task 2: Implement effective queue-order navigation and repeat/shuffle semantics

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:121-283`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt:164-210` only if an existing assertion must be adjusted from the old stop-at-0 behavior to stop-at-end behavior

**Interfaces:**
- Consumes: Task 1 enums and controller mode APIs
- Produces: `PlaybackController.skipToNext()` and `PlaybackController.skipToPrevious()` public methods
- Produces: deterministic test hook through constructor parameter `shuffleOrderFactory: (List<String>, String?) -> List<String> = ::defaultShuffleOrder`
- Produces: mode-aware implementations of `onPlaybackCompleted()`, `onSkipToNext()`, and `onSkipToPrevious()`

- [ ] **Step 1: Add failing repeat and transport tests**

Append these tests to `PlaybackControllerTest.kt`:

```kotlin
    @Test
    fun stopAfterQueueAdvancesMiddleTrackAndStopsAtFinalTrackEnd() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.play()

        engine.complete()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)

        engine.complete()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)

        engine.complete()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(3_000L, controller.state.value.positionMillis)
    }

    @Test
    fun stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.setRepeatMode(RepeatMode.StopAfterCurrent)
        controller.play()

        engine.complete()

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(1_000L, controller.state.value.positionMillis)
    }

    @Test
    fun repeatPlaylistWrapsCompletionAndManualTransport() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.play()

        engine.complete()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)

        controller.skipToPrevious()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun repeatOneReplaysCurrentTrackButManualTransportCanMoveWithoutWrapping() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.play()

        engine.complete()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)

        controller.skipToPrevious()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
    }

    @Test
    fun shuffleUsesGeneratedOrderAndKeepsCurrentTrackActive() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { ids, currentId -> listOf(currentId!!, "track-3", "track-1") },
        )
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")

        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { _, currentId -> listOf(currentId!!, "track-3", "track-1") },
        )
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        assertEquals("track-3", controller.state.value.currentTrack?.id)

        controller.setShuffleMode(ShuffleMode.Off)
        controller.skipToPrevious()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
    }

    @Test
    fun shuffledQueueReplacementRegeneratesOrderAndPreservesSelectedTrack() {
        val generatedOrders = mutableListOf<List<String>>()
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { ids, currentId ->
                val order = listOf(currentId!!) + ids.filterNot { it == currentId }.reversed()
                generatedOrders += order
                order
            },
        )
        controller.setQueue(testTracks(3), selectedTrackId = "track-2")
        controller.setShuffleMode(ShuffleMode.On)

        controller.setQueue(testTracks(4), selectedTrackId = "track-3")

        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(listOf("track-3", "track-4", "track-2", "track-1"), generatedOrders.last())
    }
```

Add this helper inside the test class:

```kotlin
    private fun testTracks(count: Int): List<PlayableTrack> = (1..count).map { index ->
        PlayableTrack(
            id = "track-$index",
            title = "Track $index",
            artist = "Test Artist",
            album = "Test Album",
            durationMillis = index * 1_000L,
            source = AudioSource.FilePath("/tmp/track-$index.mp3"),
        )
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: FAIL because `skipToNext`, `skipToPrevious`, deterministic shuffle injection, and mode-aware completion do not exist yet.

- [ ] **Step 3: Add deterministic shuffle factory and order state**

Change `PlaybackController` constructor:

```kotlin
class PlaybackController(
    private val engine: PlatformPlaybackEngine = createPlatformPlaybackEngine(),
    private val shuffleOrderFactory: (List<String>, String?) -> List<String> = ::defaultShuffleOrder,
) : PlaybackEngineListener {
```

Add private order state near `playWhenLoaded`:

```kotlin
    private var shuffledOrder: List<String> = emptyList()
```

Add top-level helper near the end of `Playback.kt`:

```kotlin
private fun defaultShuffleOrder(ids: List<String>, currentId: String?): List<String> {
    if (ids.size <= 1) return ids
    val shuffled = ids.shuffled()
    if (currentId == null || currentId !in shuffled) return shuffled
    return listOf(currentId) + shuffled.filterNot { it == currentId }
}
```

- [ ] **Step 4: Add effective-order helpers**

Inside `PlaybackController`, add private helpers:

```kotlin
    private fun effectiveOrder(): List<String> = when (_state.value.shuffleMode) {
        ShuffleMode.Off -> _state.value.queue.map { it.id }
        ShuffleMode.On -> shuffledOrder.ifEmpty { _state.value.queue.map { it.id } }
    }

    private fun trackById(trackId: String?): PlayableTrack? = _state.value.queue.firstOrNull { it.id == trackId }

    private fun currentEffectiveIndex(order: List<String> = effectiveOrder()): Int =
        order.indexOf(_state.value.currentTrack?.id)

    private fun nextTrack(wrap: Boolean): PlayableTrack? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val nextId = order.getOrNull(currentIndex + 1) ?: if (wrap) order.firstOrNull() else null
        return trackById(nextId)
    }

    private fun previousTrack(wrap: Boolean): PlayableTrack? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val previousId = order.getOrNull(currentIndex - 1) ?: if (wrap) order.lastOrNull() else null
        return trackById(previousId)
    }

    private fun regenerateShuffleOrder(currentId: String? = _state.value.currentTrack?.id) {
        shuffledOrder = shuffleOrderFactory(_state.value.queue.map { it.id }, currentId)
            .filter { id -> _state.value.queue.any { it.id == id } }
            .distinct()
        val missing = _state.value.queue.map { it.id }.filterNot { it in shuffledOrder }
        shuffledOrder = shuffledOrder + missing
    }
```

- [ ] **Step 5: Replace mode APIs with shuffle-aware logic**

Update `setShuffleMode` from Task 1:

```kotlin
fun setShuffleMode(mode: ShuffleMode) {
    val previous = _state.value.shuffleMode
    if (previous == mode) return
    _state.value = _state.value.copy(shuffleMode = mode)
    when (mode) {
        ShuffleMode.On -> regenerateShuffleOrder()
        ShuffleMode.Off -> shuffledOrder = emptyList()
    }
}
```

Update `setQueue()` so after `_state.value = ...` in the selected branch, it calls:

```kotlin
if (_state.value.shuffleMode == ShuffleMode.On) {
    regenerateShuffleOrder(selected.id)
} else {
    shuffledOrder = emptyList()
}
```

In the empty branch, also clear `shuffledOrder = emptyList()`.

- [ ] **Step 6: Add public skip methods and use them from platform callbacks**

Add public methods:

```kotlin
fun skipToNext() {
    val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
    nextTrack(wrap)?.let { loadSelected(it, autoPlay = true) }
}

fun skipToPrevious() {
    val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
    previousTrack(wrap)?.let { loadSelected(it, autoPlay = true) }
}
```

Replace `onSkipToNext()` body with `skipToNext()` and `onSkipToPrevious()` body with `skipToPrevious()`.

- [ ] **Step 7: Implement completion semantics**

Replace `onPlaybackCompleted()` with:

```kotlin
override fun onPlaybackCompleted() {
    when (_state.value.repeatMode) {
        RepeatMode.RepeatOne -> {
            val current = _state.value.currentTrack ?: return stopAtCurrentTrackEnd()
            loadSelected(current, autoPlay = true)
        }
        RepeatMode.RepeatPlaylist -> {
            val next = nextTrack(wrap = true)
            if (next != null) loadSelected(next, autoPlay = true) else stopAtCurrentTrackEnd()
        }
        RepeatMode.StopAfterCurrent -> stopAtCurrentTrackEnd()
        RepeatMode.StopAfterQueue -> {
            val next = nextTrack(wrap = false)
            if (next != null) loadSelected(next, autoPlay = true) else stopAtCurrentTrackEnd()
        }
    }
}
```

Add helper:

```kotlin
    private fun stopAtCurrentTrackEnd() {
        val duration = _state.value.durationMillis
        _state.value = _state.value.copy(
            status = PlaybackStatus.Stopped,
            positionMillis = duration ?: max(0L, _state.value.positionMillis),
            error = null,
        )
    }
```

- [ ] **Step 8: Run focused playback tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerStopsWhenLastTrackCompletes' --configuration-cache
```

Expected: PASS. If `controllerStopsWhenLastTrackCompletes` asserts only status, it should pass unchanged; if it asserts position `0`, update it to expect the track duration per spec.

- [ ] **Step 9: Commit Task 2**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt
git commit -m "feat: add repeat and shuffle queue navigation"
```

---

### Task 3: Add NowPlayingScreen repeat and shuffle controls

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt:3-238`

**Interfaces:**
- Consumes: `PlaybackState.repeatMode`, `PlaybackState.shuffleMode`
- Consumes: `PlaybackController.cycleRepeatMode()` and `PlaybackController.toggleShuffleMode()`
- Consumes: `PlaybackController.skipToPrevious()` and `PlaybackController.skipToNext()`
- Produces: two shared Compose controls on Now Playing only

- [ ] **Step 1: Replace UI-local previous/next calculations with controller calls**

In `NowPlayingScreen.kt`, replace the previous button `hausClickable` body with:

```kotlin
.hausClickable { playbackController.skipToPrevious() }
```

Replace the next button `hausClickable` body with:

```kotlin
.hausClickable { playbackController.skipToNext() }
```

This makes Now Playing transport use the same mode-aware path as platform callbacks.

- [ ] **Step 2: Add icon imports**

Add imports for icons that exist in Material Icons Extended. Try these first:

```kotlin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
```

If `RepeatOne` is unavailable in the JetBrains icon artifact, use `Icons.Filled.Repeat` for all repeat states and distinguish modes with content description plus highlight color; do not add dependencies.

- [ ] **Step 3: Add derived UI labels and colors**

Near `val isPlaying = playbackState.isPlaying`, add:

```kotlin
    val shuffleEnabled = playbackState.shuffleMode == ShuffleMode.On
    val repeatContentDescription = when (playbackState.repeatMode) {
        RepeatMode.StopAfterQueue -> "Repeat mode: play list then stop. Tap for playlist repeat"
        RepeatMode.RepeatPlaylist -> "Repeat mode: playlist repeat. Tap for single track repeat"
        RepeatMode.RepeatOne -> "Repeat mode: single track repeat. Tap for play current song then stop"
        RepeatMode.StopAfterCurrent -> "Repeat mode: play current song then stop. Tap for play list then stop"
    }
    val shuffleContentDescription = if (shuffleEnabled) {
        "Shuffle on. Tap to turn shuffle off"
    } else {
        "Shuffle off. Tap to shuffle playlist songs"
    }
```

- [ ] **Step 4: Add control row above transport controls**

Immediately before the existing `// Transport controls` row, insert:

```kotlin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackModeButton(
                    selected = shuffleEnabled,
                    contentDescription = shuffleContentDescription,
                    onClick = playbackController::toggleShuffleMode,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = if (shuffleEnabled) Color.White else HausColors.current.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                PlaybackModeButton(
                    selected = playbackState.repeatMode == RepeatMode.RepeatPlaylist || playbackState.repeatMode == RepeatMode.RepeatOne,
                    contentDescription = repeatContentDescription,
                    onClick = playbackController::cycleRepeatMode,
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.RepeatOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = null,
                        tint = if (playbackState.repeatMode == RepeatMode.RepeatPlaylist || playbackState.repeatMode == RepeatMode.RepeatOne) Color.White else HausColors.current.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
```

- [ ] **Step 5: Add reusable mode button composable**

Near the bottom of `NowPlayingScreen.kt`, before `statusLabel`, add:

```kotlin
@Composable
private fun PlaybackModeButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) HausColors.current.pulse else HausColors.current.panel)
            .hausClickable(onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
```

Add imports if needed:

```kotlin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
```

- [ ] **Step 6: Compile and adjust only for actual icon availability**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: PASS. If `RepeatOne` icon is missing, change only the repeat icon expression to `Icons.Filled.Repeat` and keep mode distinctions through state and content description.

- [ ] **Step 7: Run focused playback tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 8: Commit Task 3**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
git commit -m "feat: add now playing repeat shuffle controls"
```

---

### Task 4: Verify OpenSpec change, update evidence, and finalize

**Files:**
- Modify: `openspec/changes/playback-repeat-shuffle/tasks.md`
- Modify: `progress.md`
- Existing from design/plan: `docs/superpowers/specs/2026-07-06-playback-repeat-shuffle-design.md`, `docs/superpowers/plans/2026-07-06-playback-repeat-shuffle.md`, `openspec/changes/playback-repeat-shuffle/*`

**Interfaces:**
- Consumes: completed Tasks 1-3 commits
- Produces: OpenSpec task evidence and session handoff evidence

- [ ] **Step 1: Validate OpenSpec**

Run:

```bash
openspec validate playback-repeat-shuffle --strict
```

Expected: `Change 'playback-repeat-shuffle' is valid`.

- [ ] **Step 2: Run focused tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`. If a known flaky playback test fails, rerun the exact failing test once and then rerun the same broad command before claiming pass.

- [ ] **Step 4: Run iOS verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: Xcode version prints successfully and iOS simulator tests are `BUILD SUCCESSFUL`. If `xcodebuild` is unavailable, record the exact blocker and do not claim iOS validation passed.

- [ ] **Step 5: Check diff hygiene**

Run:

```bash
git diff --check
git status --short
```

Expected: `git diff --check` exits 0. Status should contain only intentional playback-repeat-shuffle docs/spec/evidence changes and implementation commits not yet final-committed if any.

- [ ] **Step 6: Update OpenSpec tasks**

In `openspec/changes/playback-repeat-shuffle/tasks.md`, mark Tasks 1-4 complete and include exact command results under Task 4 bullets.

- [ ] **Step 7: Update progress.md handoff**

Append:

```text
## Handoff - 2026-07-06 playback repeat shuffle

Route: openspec+superpowers
Owner: implementation
Scope: Shared playback repeat/shuffle modes and NowPlayingScreen controls.
Implementation:
- Added shared RepeatMode/ShuffleMode state and controller APIs.
- Centralized completion, previous, and next through mode-aware effective order logic.
- Added shuffle effective order generation that preserves current track and keeps visible library order unchanged.
- Added NowPlayingScreen repeat/shuffle controls using Material vector icons.
Verification:
- openspec validate playback-repeat-shuffle --strict: <pass/fail exact output>
- ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache: <pass/fail exact output>
- ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache: <pass/fail exact output>
- /usr/bin/xcrun xcodebuild -version: <version or blocker>
- ./gradlew :shared:iosSimulatorArm64Test --configuration-cache: <pass/fail exact output>
Acceptance:
- Requirement matched: <yes/no>
- Scope controlled: <yes/no>
- Edge cases/risk reviewed: <notes>
Changed files:
- <list files>
Next owner: user/OpenSpec for manual playback UX validation and archive when satisfied.
Blockers: <none or exact blocker>
Commit: <commit hashes/messages or pending>
```

Replace placeholders with exact evidence; do not leave angle brackets.

- [ ] **Step 8: Commit evidence/docs finalization**

```bash
git add docs/superpowers/specs/2026-07-06-playback-repeat-shuffle-design.md docs/superpowers/plans/2026-07-06-playback-repeat-shuffle.md openspec/changes/playback-repeat-shuffle progress.md
git commit -m "docs: record playback repeat shuffle evidence"
```

- [ ] **Step 9: Final staged-diff/user-facing summary**

Run:

```bash
git log --oneline -5
git status --short
```

Report commit hashes, exact verification commands, and any remaining uncommitted files. Do not claim manual device UX validation unless it was actually performed.
