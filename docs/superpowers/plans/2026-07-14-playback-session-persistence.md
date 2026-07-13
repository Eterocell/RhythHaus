# Playback Session Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist a reconciled local playback session and restore it exactly once, always paused and transport-safe.

**Architecture:** The controller owns monotonic request generations, paused loads, and checkpoint production. A single FIFO coordinator serializes restore, complete-snapshot saves, reconciliation, and flush barriers. Platform engines reject remote transport during restore and identify every stale-capable callback with its immutable load generation.

**Tech Stack:** Kotlin Multiplatform, coroutines-core, Preferences DataStore, Koin, Media3, kotlin.test.

## Global Constraints

- Persist only queue IDs, current ID, position, `RepeatMode`, and `ShuffleMode`. Regenerate runtime shuffle order after restore and reconcile.
- Use `playback_session.preferences_pb`; codec limits are `maxIds=10_000`, `maxIdCharacters=4_096`, `maxIdUtf8Bytes=16_384`, and `maxEncodedUtf8Bytes=1_048_576`.
- Restore is `loadPaused`, clamp, seek, pause/state `Paused`, and never play.
- `PlaybackController` owns every monotonically increasing load/clear generation. Every platform callback carries an immutable generation.
- `setCommandsEnabled` must call `PlatformPlaybackEngine.setUserTransportEnabled`; controller and platform transport reject commands while restoring.
- One FIFO coordinator collapses only consecutive checkpoints. Restore, reconcile, and flush are barriers. No desired/durable counters.
- No dependency, SQL migration, UI redesign, cloud sync, Windows/Linux scope, or abrupt-death durability promise.
- Every task commit command uses `GIT_MASTER=1`, a Conventional Commit message, `Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)`, and `Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>`.

---

## File Structure

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt`: ID-only model, codec, checkpoint key.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt`: validated atomic Preferences adapter.
- `shared/src/{androidMain,iosMain,jvmMain}/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.<platform>.kt`: dedicated file factories.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`: contract, controller, generations, command gate, restore, reconcile.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`, `RhythHausPlaybackService.kt`, and `RhythHausTransportBridge.kt`: paused Android load, MediaItem provenance, and production service/bridge transport gate.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: paused load, generation-capturing sources, and pure iOS remote transport decision gate.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` and `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: paused JVM load, generation-capturing sources, and native macOS remote transport state.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`: FIFO coordinator and safe results.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt`: once-only restore.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`, and Android startup files: process ownership and publication ordering.

### Task 1: Create the bounded ID-only snapshot codec

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`

**Interfaces:**

```kotlin
data class PlaybackSessionSnapshot(
    val queueIds: List<String> = emptyList(),
    val currentTrackId: String? = null,
    val positionMillis: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
)
object PlaybackSessionCodec {
    const val maxIds = 10_000
    const val maxIdCharacters = 4_096
    const val maxIdUtf8Bytes = 16_384
    const val maxEncodedUtf8Bytes = 1_048_576
    fun encodeIds(ids: List<String>): String
    fun decodeIds(encoded: String): List<String>?
}
```

- [ ] **Step 1: Write failing codec tests**

```kotlin
@Test fun codecRoundTripsDelimitersAndEmoji() {
    val ids = listOf("a:b", "line\n2", "🎧")
    assertEquals(ids, PlaybackSessionCodec.decodeIds(PlaybackSessionCodec.encodeIds(ids)))
}
@Test fun codecRejectsInvalidForms() {
    assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("")) }
    assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("same", "same")) }
    assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("\uD800")) }
    assertNull(PlaybackSessionCodec.decodeIds("2:ab!"))
    assertNull(PlaybackSessionCodec.decodeIds("2:a"))
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache`

Expected: FAIL because the snapshot codec is absent.

- [ ] **Step 3: Implement exact bounds and all-or-null decoding**

```kotlin
fun encodeIds(ids: List<String>): String {
    require(ids.size <= maxIds)
    require(ids.distinct().size == ids.size)
    return buildString {
        ids.forEach { id ->
            require(id.isNotEmpty() && id.length <= maxIdCharacters)
            require(!id.hasUnpairedSurrogate() && id.encodeToByteArray().size <= maxIdUtf8Bytes)
            append(id.length).append(':').append(id)
        }
    }.also { require(it.encodeToByteArray().size <= maxEncodedUtf8Bytes) }
}
```

Implement `decodeIds` by parsing decimal digits through a colon, requiring a positive count, taking exactly that many Kotlin characters, rejecting malformed/truncated/trailing/duplicate/surrogate/over-bound values, and returning `null` for any invalid stored value. Add `ProgressCheckpointKey(generation, currentTrackId, secondBucket)` and complete `PlaybackCheckpoint` models.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt && GIT_MASTER=1 git commit -m "feat: add playback session codec" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 2: Add validated DataStore session storage

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt`
- Create: `shared/src/{androidMain,iosMain,jvmMain}/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.<platform>.kt`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt`

**Interfaces:**

```kotlin
interface PlaybackSessionStore {
    suspend fun read(): PlaybackSessionSnapshot
    suspend fun save(snapshot: PlaybackSessionSnapshot)
}
expect fun createPlaybackSessionStore(): PlaybackSessionStore
```

- [ ] **Step 1: Write failing all-or-nothing tests**

```kotlin
@Test fun invalidSaveKeepsPriorDurableSnapshot() = runBlocking {
    val store = DataStorePlaybackSessionStore(testPreferencesDataStore())
    val valid = PlaybackSessionSnapshot(queueIds = listOf("one"), currentTrackId = "one")
    store.save(valid)
    assertFailsWith<IllegalArgumentException> {
        store.save(valid.copy(queueIds = List(PlaybackSessionCodec.maxIds + 1) { "$it" }))
    }
    assertEquals(valid, store.read())
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache`

Expected: FAIL because the session store is absent.

- [ ] **Step 3: Validate before one edit and add factories**

```kotlin
override suspend fun save(snapshot: PlaybackSessionSnapshot) {
    val encodedQueue = PlaybackSessionCodec.encodeIds(snapshot.queueIds)
    val encodedCurrent = snapshot.currentTrackId?.let { PlaybackSessionCodec.encodeIds(listOf(it)) }
    dataStore.edit { preferences ->
        preferences[queueIdsKey] = encodedQueue
        preferences[currentIdKey] = encodedCurrent ?: ""
        preferences[positionKey] = snapshot.positionMillis.coerceAtLeast(0L)
        preferences[repeatModeKey] = snapshot.repeatMode.name
        preferences[shuffleModeKey] = snapshot.shuffleMode.name
    }
}
```

Use `ReplaceFileCorruptionHandler { emptyPreferences() }` and the exact file name `playback_session.preferences_pb` in every actual. Decode any invalid field set as the full empty/default snapshot.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt && GIT_MASTER=1 git commit -m "feat: persist playback session snapshots" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 3: Make all engine loads paused and generation-safe

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- Modify: `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`
- Modify: `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridgeTest.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridge.kt`
- Modify: `shared/src/nativeInterop/macos/rhythhaus_audio.mm`
- Modify: `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt` and/or `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`

**Interfaces:**

```kotlin
data class LoadedPlayback(val generation: Long, val durationMillis: Long?)
interface PlatformPlaybackEngine {
    suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback
    fun clear(generation: Long)
    fun setUserTransportEnabled(enabled: Boolean)
}
```

- [ ] **Step 1: Write failing paused-load and platform-gate tests**

```kotlin
@Test fun androidOldTokenCannotAcknowledgeSameTrackNewGeneration() = runBlocking {
    val tracker = Media3RequestTokenTracker()
    val first = tracker.begin(10L)
    val second = tracker.begin(11L)
    assertFalse(tracker.accepts(first, observedCurrentToken = first))
    assertTrue(tracker.accepts(second, observedCurrentToken = second))
}
@Test fun serviceBridgeRejectsPlayAndSeekWhenTransportDisabled() {
    val bridge = RhythHausTransportBridge.forHostTest()
    bridge.setTransportEnabled(false)
    assertFalse(bridge.handleServicePlayForTest())
    assertFalse(bridge.handleServiceSeekForTest(2_000L))
    assertFalse(bridge.availableCommandsForTest().contains(Player.COMMAND_PLAY_PAUSE))
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:testAndroidHostTest --configuration-cache`

Expected: FAIL because generation-aware paused loading and transport handling are absent.

- [ ] **Step 3: Implement provenance and platform gating**

```kotlin
private fun nextGeneration(): Long = ++activeGeneration

private suspend fun loadPaused(track: PlayableTrack): LoadedPlayback {
    val generation = nextGeneration()
    return engine.loadPaused(track, generation).also { loaded ->
        check(loaded.generation == generation)
    }
}

fun setCommandsEnabled(enabled: Boolean) {
    commandsEnabled = enabled
    engine.setUserTransportEnabled(enabled)
}
```

Attach `Media3RequestToken(generation, nonce)` to every Android `MediaItem`, clear player play intent before prepare, and finish `loadPaused` only at matching READY with no playing callback. Add one process-shared transport-enabled gate wired from `AndroidPlaybackEngine.setUserTransportEnabled` through `RhythHausTransportBridge` and the production `RhythHausPlaybackService` wrapper into `SkipRoutingPlayer`. While false, the wrapper removes relevant available commands and rejects/no-ops play, pause, stop, seek, next, and previous before player work. `RhythHausTransportBridgeTest` must exercise that service/bridge path, never an engine-only helper.

In `PlaybackEngine.ios.kt`, create generation-capturing delegate/progress/completion sources, invalidate old sources at load/clear, and route remote play/seek through internal pure `IOSRemoteTransportGate`. Its disabled decision returns command failed and invokes no provider action; tests in `IOSNowPlayingInfoTest.kt` and/or `IOSAudioPlayerBridgeTest.kt` prove disabled then enabled behavior. In `PlaybackEngine.jvm.kt`, make `MacAudioPlayerBridge` own a transport-enabled Boolean separately from its native handle. `setTransportEnabled` updates retained state and any current handle; `resetPlayer` and native handle creation immediately apply retained state before remote registration or acceptance. Add checks to every macOS remote handler in `nativeInterop/macos/rhythhaus_audio.mm`. `JvmPlaybackEngineTest` disables transport before `loadPaused` resets the handle, proves remote play/seek are rejected with no native effect, then reenables and proves the enabled path works. Keep enabled transport behavior intact.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache && ./gradlew :shared:testAndroidHostTest --configuration-cache && ./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`

Expected: JVM and Android-host suites pass; iOS main compilation passes. `iosSimulatorArm64Test` filtering is not relied upon because current project convention validates iOS tests as the full `:shared:iosSimulatorArm64Test` task in final verification.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridge.kt shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt shared/src/nativeInterop/macos/rhythhaus_audio.mm shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridgeTest.kt shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt && GIT_MASTER=1 git commit -m "feat: add paused generation safe playback loads" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 4: Add controller session restore and checkpoint behavior

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`

**Interfaces:**

```kotlin
internal interface PlaybackSessionController {
    val checkpoints: Flow<PlaybackCheckpoint>
    fun sessionSnapshot(): PlaybackSessionSnapshot
    suspend fun restoreSession(snapshot: PlaybackSessionSnapshot, tracks: List<PlayableTrack>)
    suspend fun reconcileSession(tracks: List<PlayableTrack>)
    fun setCommandsEnabled(enabled: Boolean)
}
```

- [ ] **Step 1: Write failing restore/coalescing tests**

```kotlin
@Test fun restoreLoadsClampsSeeksAndPausesWithoutPlay() = runBlocking {
    val engine = FakePlaybackEngine(loadedDurationMillis = 1_000L)
    val controller = PlaybackController(engine, Dispatchers.Default)
    controller.restoreSession(saved(positionMillis = 2_000L), listOf(track("one")))
    assertEquals(listOf("loadPaused:one", "seek:1000", "pause"), engine.commands)
    assertEquals(PlaybackStatus.Paused, controller.state.value.status)
}
@Test fun progressUsesGenerationTrackAndSecondBucket() = runBlocking {
    val controller = readyController()
    controller.onPlaybackProgress(1L, 1_100L, 10_000L)
    controller.onPlaybackProgress(1L, 1_900L, 10_000L)
    assertEquals(1, controller.collectedProgressCheckpointCount())
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`

Expected: FAIL because session restore and complete checkpoint behavior are absent.

- [ ] **Step 3: Implement guarded restore and reconciliation**

```kotlin
val loaded = engine.loadPaused(current, nextGeneration())
val clamped = loaded.durationMillis?.let { snapshot.positionMillis.coerceIn(0L, it) }
    ?: snapshot.positionMillis.coerceAtLeast(0L)
engine.seekTo(clamped)
engine.pause()
publishPaused(current, clamped, loaded.durationMillis)
emitImmediateCheckpoint()
```

Guard each public queue, select, mode, play, pause, stop, seek, restart, and skip API with `if (!commandsEnabled) return`. Regenerate runtime shuffle order from reconciled IDs and restored mode. Preserve current playback without reload when it survives. Use empty paused state if restore load fails. Emit immediate snapshots for the listed discrete transitions; emit progress only for a new key and reset the key on load/select/seek/stop/restore/reconcile.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt && GIT_MASTER=1 git commit -m "feat: restore paused playback sessions" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 5: Build the FIFO coordinator with failed-safe completion

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt`

**Interfaces:**

```kotlin
enum class PlaybackSessionReconcileResult { Applied, FailedSafeApplied }
internal fun interface PlaybackSessionReconciler {
    suspend fun reconcile(tracks: List<LibraryTrack>): PlaybackSessionReconcileResult
}
```

- [ ] **Step 1: Write failing FIFO and failure tests**

```kotlin
@Test fun reconcileQueuedDuringRestoreRunsBeforeCallerCompletes() = runBlocking {
    val controller = BlockingRestoreController()
    val coordinator = PlaybackSessionCoordinator(controller, RecordingStore(), backgroundScope)
    val restore = async { coordinator.restoreOnce(tracks) }
    controller.restoreStarted.await()
    val reconcile = async { coordinator.reconcile(tracks) }
    controller.allowRestore.complete(Unit)
    restore.await()
    assertEquals(PlaybackSessionReconcileResult.Applied, reconcile.await())
}
@Test fun saveFailureCompletesFlushAndFutureReconcileSafe() = runBlocking {
    val coordinator = readyCoordinator(store = FailingStore())
    coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("one")))
    coordinator.flush()
    assertEquals(PlaybackSessionReconcileResult.FailedSafeApplied, coordinator.reconcile(tracks))
}
@Test fun throwingReadStillReenablesCommandsAndCompletesRestore() = runBlocking {
    val controller = RecordingSessionController()
    val coordinator = PlaybackSessionCoordinator(controller, ThrowingReadStore(), backgroundScope)
    coordinator.restoreOnce(tracks)
    assertTrue(controller.commandsEnabled)
    assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`

Expected: FAIL because the coordinator is absent.

- [ ] **Step 3: Implement barrier-preserving actor**

```kotlin
private sealed interface Command {
    data class Checkpoint(val checkpoint: PlaybackCheckpoint) : Command
    data class Restore(val tracks: List<PlayableTrack>, val reply: CompletableDeferred<Unit>) : Command
    data class Reconcile(val tracks: List<LibraryTrack>, val reply: CompletableDeferred<PlaybackSessionReconcileResult>) : Command
    data class Flush(val reply: CompletableDeferred<Unit>) : Command
}
```

Process commands in received order. Before executing a checkpoint run, consume only immediately following checkpoint commands and write its newest snapshot once. Never consume across restore, reconcile, or flush. Put each restore reply in a `try/finally`-equivalent completion path: disable commands before read/restore; on every success, malformed fallback, load failure, or unexpected exception, reenable controller/platform commands and complete the reply. For a non-corruption `store.read()` exception, apply empty paused controller state, enter `FailedSafe`, and stop persistence attempts. A save exception records the last successful snapshot as durable history, changes phase to `FailedSafe`, stops subsequent `store.save` calls, and completes every waiting or future flush/reconcile reply. Reconcile in FailedSafe calls controller reconciliation but skips saving and returns `FailedSafeApplied`.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt && GIT_MASTER=1 git commit -m "feat: coordinate playback session persistence" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 6: Wire process ownership and publication after reconciliation

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt`
- Modify: `androidApp/src/main/kotlin/com/eterocell/rhythhaus/RhythHausApplication.kt`
- Modify: `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`

**Interfaces:**

```kotlin
class PlaybackProcessLifecycle(private val coordinator: PlaybackSessionCoordinator) {
    suspend fun restoreOnce(tracks: List<PlayableTrack>)
}
```

- [ ] **Step 1: Write failing lifecycle/publication tests**

```kotlin
@Test fun concurrentRestoreOnceInvocationsRunOneRestore() = runBlocking {
    val coordinator = CountingCoordinator()
    val lifecycle = PlaybackProcessLifecycle(coordinator)
    coroutineScope { repeat(20) { launch { lifecycle.restoreOnce(tracks) } } }
    assertEquals(1, coordinator.restoreCount)
}
@Test fun cancelledFirstWaiterDoesNotReplaceSharedRestoreAttempt() = runBlocking {
    val coordinator = BlockingRestoreCoordinator()
    val lifecycle = PlaybackProcessLifecycle(coordinator, processScope)
    val first = launch { lifecycle.restoreOnce(tracks) }
    coordinator.restoreStarted.await()
    first.cancelAndJoin()
    val second = async { lifecycle.restoreOnce(tracks) }
    coordinator.allowRestore.complete(Unit)
    second.await()
    assertEquals(1, coordinator.restoreCount)
}
@Test fun libraryContentPublishesAfterReconcileCompletes() = runBlocking {
    val events = mutableListOf<String>()
    val reconciler = PlaybackSessionReconciler { events += "reconcile"; PlaybackSessionReconcileResult.Applied }
    refreshLibrary(reconciler, content) { events += "publish" }
    assertEquals(listOf("reconcile", "publish"), events)
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`

Expected: FAIL because the process lifecycle and reconciliation publication boundary are absent.

- [ ] **Step 3: Implement singleton ownership and post-reconcile publication**

```kotlin
suspend fun restoreOnce(tracks: List<PlayableTrack>) {
    val sharedAttempt = restoreMutex.withLock {
        restoreAttempt ?: processScope.async { coordinator.restoreOnce(tracks) }.also { restoreAttempt = it }
    }
    sharedAttempt.await()
}
```

Initialize Android context and Koin in `RhythHausApplication`, not `MainActivity`. Bind singleton engine/controller/store/coordinator/lifecycle. Give lifecycle one process scope plus a mutex-protected shared deferred/job; callers await it but their cancellation cannot cancel/reset it. Leave iOS/JVM process singleton startup intact. Remove `DisposableEffect` release and any Compose flush. Make scan, remove, and clear helpers await `reconciler.reconcile(content.tracks)` before publishing, and publish for both `Applied` and `FailedSafeApplied`.

- [ ] **Step 4: Run GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
GIT_MASTER=1 git add androidApp/src/main/kotlin/com/eterocell/rhythhaus/RhythHausApplication.kt androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt && GIT_MASTER=1 git commit -m "feat: own playback sessions by process" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 7: Verify implementation and record evidence

**Files:**
- Modify: `openspec/changes/persist-playback-session/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

**Interfaces:**

```kotlin
enum class PlaybackSessionReconcileResult { Applied, FailedSafeApplied }
```

- [ ] **Step 1: Run strict and platform checks**

```bash
openspec validate persist-playback-session --strict
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: strict validation plus JVM, Android-host, desktop, and Android builds pass. Record an exact iOS failure if it occurs.

- [ ] **Step 2: Review requirement coverage and evidence**

```bash
GIT_MASTER=1 git diff --check && GIT_MASTER=1 git diff -- shared androidApp openspec/changes/persist-playback-session docs/superpowers/specs/2026-07-14-playback-session-persistence-design.md docs/superpowers/plans/2026-07-14-playback-session-persistence.md
```

Confirm paused restore has no play action, no exact shuffle order persists, transport rejection tests cover Android/iOS/JVM, and failed-safe callers complete. Update task status, `progress.md`, and `roadmap.md` with actual command output and any manual device limits.

- [ ] **Step 3: Commit evidence**

```bash
GIT_MASTER=1 git add openspec/changes/persist-playback-session/tasks.md progress.md roadmap.md && GIT_MASTER=1 git commit -m "docs: record playback session evidence" -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

## Plan Self-Review

**Spec coverage:** Tasks 1 and 2 cover the bounded file/codec and validation-before-edit. Task 3 covers paused generation-aware engines and remote transport rejection. Task 4 covers controller restore, reconciliation, and checkpoint keys. Task 5 covers FIFO barriers and failed-safe completion. Task 6 covers process ownership and publication ordering. Task 7 includes strict OpenSpec, Android-host, and iOS verification.

**Placeholder scan:** Every task has exact source paths, interfaces, test code, commands, expected outcomes, and commit command. No deferred implementation marker remains.

**Type consistency:** The plan uses `loadPaused`, `LoadedPlayback(generation, durationMillis)`, `setUserTransportEnabled`, `ProgressCheckpointKey`, and the two-value `PlaybackSessionReconcileResult` consistently.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-14-playback-session-persistence.md`. Two execution options:

1. **Subagent-Driven (recommended)**, dispatch a fresh subagent per task and review between tasks.
2. **Inline Execution**, execute tasks in this session using executing-plans with checkpoints.

Which approach?
