# Playback Session Persistence Design

## Goal and scope

RhythHaus will persist a local playback-session snapshot and restore it once per process after authoritative library content is available. Restoration always leaves playback paused. It never autoplays, even if the app was playing before exit.

The snapshot contains only queue IDs, current ID, position, `RepeatMode`, and `ShuffleMode`. It stores no artwork, paths, URIs, metadata, errors, engine objects, or exact effective shuffled order. After restore, the controller regenerates its runtime shuffle order from the reconciled queue and restored `ShuffleMode`.

This change adds no dependency, SQLDelight migration, UI redesign, cloud sync, Windows/Linux support, or abrupt-death durability promise.

## Snapshot storage and codec

Each platform creates an independent Preferences DataStore named `playback_session.preferences_pb`, with `ReplaceFileCorruptionHandler { emptyPreferences() }`. A snapshot is written in one `DataStore.edit` operation.

Ordered IDs use a direct concatenation of `<decimal-character-count>:<ID>`, where the count is Kotlin `String.length`. The codec has these exact bounds:

| Bound | Value |
| --- | ---: |
| `maxIds` | 10,000 |
| `maxIdCharacters` | 4,096 |
| `maxIdUtf8Bytes` | 16,384 |
| `maxEncodedUtf8Bytes` | 1,048,576 |

Encoding rejects an empty ID, duplicate ID, unpaired UTF-16 surrogate, ID over any per-ID bound, more than `maxIds`, and encoded output over `maxEncodedUtf8Bytes`. `maxIdUtf8Bytes=16_384` is a defense-in-depth ceiling independently unreachable for valid strings under `maxIdCharacters=4_096`; the largest reachable three-byte UTF-8 payload is 12,288 bytes. Decoding validates the same bounds while parsing character counts with `substring`; it rejects non-decimal counts, missing colon, empty ID, truncation, duplicate ID, unpaired surrogate, malformed input, and trailing data. A corrupt Preferences file or malformed persisted fields produces the empty/default snapshot as one unit. An invalid in-memory save throws before `DataStore.edit`, so the prior valid durable snapshot remains unchanged.

## Engine boundary and callback provenance

`PlaybackController` exclusively owns a monotonically increasing generation. Every `load` and `clear` allocates the next generation. The engine contract is:

```kotlin
data class LoadedPlayback(val generation: Long, val durationMillis: Long?)

interface PlatformPlaybackEngine {
    var listener: PlaybackEngineListener?
    suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback
    fun clear(generation: Long)
    fun setUserTransportEnabled(enabled: Boolean)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMillis: Long)
    fun release()
}

interface PlaybackEngineListener {
    fun onPlaybackStatus(generation: Long, status: PlaybackStatus)
    fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?)
    fun onPlaybackCompleted(generation: Long)
    fun onPlaybackError(generation: Long, error: PlaybackError)
    fun onSkipToNext(generation: Long)
    fun onSkipToPrevious(generation: Long)
}
```

The controller accepts a callback only when its immutable generation equals the active generation. `LoadedPlayback` must also contain the requested generation, otherwise load fails safe.

`loadPaused` clears native play intent before prepare/load and acknowledges only after media is ready, with no playing action or playing callback. Restore uses this exact sequence: `loadPaused`, clamp the saved position to the acknowledged duration when known, seek, then pause and publish `Paused`. It never calls `play`.

Android assigns a unique request token containing the generation to each `MediaItem`. It accepts READY, status, progress, completion, and error only if the observable current `MediaItem` token equals the active request token. This prevents same-track loads with different generations from crossing. iOS and JVM capture the generation in each delegate, progress loop, and completion source, and invalidate all older sources on every load or clear.

## Restore transport gate

`PlaybackController.setCommandsEnabled(enabled)` controls both controller APIs and platform transport by calling `engine.setUserTransportEnabled(enabled)`. While disabled, public queue, selection, mode, and transport methods perform no state mutation, checkpoint emission, or engine action.

The platform gate also blocks bypasses during restore:

- Android uses one process-shared transport-enabled gate, read by `SkipRoutingPlayer` and the production wrapper in `RhythHausPlaybackService`. While disabled, that wrapper exposes no available relevant commands and rejects/no-ops play, pause, stop, seek, next, and previous through `RhythHausTransportBridge`; no engine-only test seam substitutes for this gate.
- iOS `PlaybackEngine.ios.kt` owns an internal pure `IOSRemoteTransportGate` decision seam. Remote play and seek return command failed and invoke no provider action while disabled.
- JVM/macOS requires `MacAudioPlayerBridge` to own its transport-enabled Boolean independently of its native handle. `setTransportEnabled` updates the stored Boolean and current handle. `resetPlayer` and native-handle creation immediately apply the stored Boolean to the new handle before registering or accepting any remote command. Every remote handler in `shared/src/nativeInterop/macos/rhythhaus_audio.mm` checks that state and is disabled or ignored while disabled, so disable → `loadPaused`/reset → remote play or seek remains rejected until explicit reenabling.

Engine callbacks remain generation-filtered and may update the controller. Tests cover controller command rejection plus platform-originated play and seek rejection during restore.

## Session controller, checkpoints, and reconciliation

```kotlin
internal interface PlaybackSessionController {
    val checkpoints: Flow<PlaybackCheckpoint>
    fun sessionSnapshot(): PlaybackSessionSnapshot
    suspend fun restoreSession(snapshot: PlaybackSessionSnapshot, tracks: List<PlayableTrack>)
    suspend fun reconcileSession(tracks: List<PlayableTrack>)
    fun setCommandsEnabled(enabled: Boolean)
}

sealed interface PlaybackCheckpoint {
    val snapshot: PlaybackSessionSnapshot
    data class Immediate(override val snapshot: PlaybackSessionSnapshot) : PlaybackCheckpoint
    data class PlayingProgress(
        val key: ProgressCheckpointKey,
        override val snapshot: PlaybackSessionSnapshot,
    ) : PlaybackCheckpoint
}

data class ProgressCheckpointKey(
    val generation: Long,
    val currentTrackId: String,
    val secondBucket: Long,
)
```

The controller emits immediate complete-snapshot checkpoints for queue/current changes, seek, pause, stop, repeat mode, shuffle mode, restore normalization, and reconciliation. Playing progress is coalesced by `(active generation, current track ID, second bucket)`. The key resets on load, selection, seek, stop, restore, and reconciliation.

Restore and reconciliation retain valid repeat and shuffle modes. Reconciliation drops missing IDs, regenerates runtime shuffle order, preserves a surviving current track without reload, loads the first survivor paused at zero when the current track is gone, and clears the engine with a new generation when no track survives.

## FIFO coordinator and failure semantics

One FIFO coordinator actor serializes `Restore`, `Checkpoint`, `Reconcile`, and `Flush`. It only collapses consecutive `Checkpoint` commands, retaining the newest complete snapshot. `Restore`, `Reconcile`, and `Flush` are barriers that cannot be crossed or collapsed. There are no desired or durable generation counters.

Restore disables commands, reads and reconciles the snapshot, saves the normalized controller snapshot before checkpoint collection, starts collection, and re-enables commands before entering `Ready` or `FailedSafe`. A reconciliation submitted during restore waits in FIFO order and runs afterward. The caller publishes authoritative library content only after its reconciliation completes.

`PlaybackSessionReconcileResult` has only `Applied` and `FailedSafeApplied`. In normal operation reconcile saves and returns `Applied`. On any save failure, the coordinator enters process-lifetime `FailedSafe`, preserves the last durable snapshot, stops all future persistence attempts, and completes queued and future flush/reconcile callers without hanging. Reconciliation in `FailedSafe` still updates the controller's paused reconciled in-memory state, permits library publication, and returns `FailedSafeApplied`; it does not save.

Malformed or corrupt reads use the empty/default snapshot. Every restore exit, including an unexpected `store.read()` I/O exception, executes a finally-equivalent path that re-enables controller and platform commands and completes the restore reply. A non-corruption read exception enters process-lifetime `FailedSafe`, applies an empty paused controller state, stops persistence attempts, and does not retry automatically. A restore load failure applies the same empty paused fail-safe state.

## Process ownership

Android initializes context and Koin in `RhythHausApplication`. iOS and JVM initialize their existing process singleton graph at startup. Koin owns singleton controller, coordinator, store, and `PlaybackProcessLifecycle`. Compose never releases or flushes this singleton graph. `PlaybackProcessLifecycle.restoreOnce` owns one process-scoped deferred/job attempt: the first caller creates it under a mutex and every caller awaits it. Cancelling one waiter neither cancels nor resets the shared attempt, and later calls await that same attempt. It behaviorally executes one restore for repeated, simultaneous, and cancelled-waiter invocation.

## Test focus

Tests cover codec exact and one-over bounds for `maxIds`, `maxIdCharacters`, and `maxEncodedUtf8Bytes`, the largest reachable 12,288-byte UTF-8 payload under the 4,096-character limit, and invalid forms, validation-before-edit preservation, generation provenance and paused acknowledgement, Android token matching, iOS/JVM source invalidation, production Android service/bridge transport gating, pure iOS remote-gate decisions, macOS native bridge remote gating across a disabled handle reset/load then explicit reenabling, paused restore, checkpoint coalescing, FIFO barriers, save and load failure safety including throwing reads, post-restore reconciliation publication, and concurrent/cancelled-waiter `restoreOnce`. Existing transport and playback behavior remains covered while commands are enabled.
