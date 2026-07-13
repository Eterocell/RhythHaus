## Context

Playback is memory-only. Platform remote handlers can currently act directly in engines, and asynchronous platform callbacks need provenance to avoid stale loads changing current state. The change must restore only a paused reconciled session and retain existing enabled-state playback behavior.

## Decisions

### Snapshot format and safe storage

The snapshot contains queue IDs, current ID, position, `RepeatMode`, and `ShuffleMode` only. It never stores effective shuffled order. Runtime shuffle order is regenerated after restore and reconciliation.

Every platform factory uses an independent `playback_session.preferences_pb` Preferences DataStore and `ReplaceFileCorruptionHandler { emptyPreferences() }`. IDs are concatenated `<decimal-character-count>:<ID>` values. `maxIds=10_000`, `maxIdCharacters=4_096`, `maxIdUtf8Bytes=16_384`, and `maxEncodedUtf8Bytes=1_048_576`. Encode rejects empty, duplicate, unpaired-surrogate, malformed, and over-bound values before `DataStore.edit`. Decode validates the same constraints and rejects malformed, truncated, or trailing input. Invalid stored state becomes empty/default; invalid in-memory save preserves the prior valid snapshot.

### Generation-provenanced paused loads

The controller allocates a monotonically increasing generation for each load and clear. `loadPaused(track, generation)` returns `LoadedPlayback(generation, durationMillis)` only when media is ready while native play intent is clear. The controller validates the returned generation, clamps, seeks, pauses, and publishes `Paused`. It never calls `play` in restore.

All status, progress, completion, error, and skip callbacks carry immutable request generations. Android puts a unique generation-containing token in each `MediaItem` and accepts a callback only when its observed current item token equals the active token. iOS/JVM capture generations in delegate, progress, and completion sources and invalidate old sources at load/clear.

### Restore command and platform transport gate

`setCommandsEnabled` calls `PlatformPlaybackEngine.setUserTransportEnabled`. Disabled controller APIs do nothing. On Android, `RhythHausPlaybackService`, `RhythHausTransportBridge`, and `SkipRoutingPlayer` share one process transport-enabled gate. The service wrapper reports no relevant available commands and rejects/no-ops play, pause, stop, seek, next, and previous before native player work. Android-host tests exercise this production gate through the service/bridge rather than engine-only helpers. In `PlaybackEngine.ios.kt`, internal pure `IOSRemoteTransportGate` returns command-failed decisions for disabled play and seek without provider work; `IOSNowPlayingInfoTest.kt` and/or `IOSAudioPlayerBridgeTest.kt` cover it. On macOS, `MacAudioPlayerBridge` owns a transport-enabled Boolean independently of its native handle. `setTransportEnabled` writes that retained Boolean and the current handle; `resetPlayer`/native handle creation immediately reapplies the retained value before remote-command registration or acceptance. Every handler in `nativeInterop/macos/rhythhaus_audio.mm` checks it before play, pause, stop, toggle, or seek. `JvmPlaybackEngineTest.kt` covers disable before `loadPaused` reset, rejected remote play/seek with no native effect, then explicit reenable and enabled behavior through a testable bridge/helper. Controller callback filtering continues while disabled.

### Controller persistence boundary

`PlaybackSessionController` exposes complete-snapshot checkpoints, snapshots, paused restore, reconciliation, and command gating. Immediate checkpoints occur for queue/current, seek, pause/stop, repeat/shuffle, restore normalization, and reconciliation. Progress emits at most once per `(generation, currentTrackId, secondBucket)` and resets the coalescing key on load, select, seek, stop, restore, and reconcile.

### FIFO coordinator and failures

One actor serializes `Restore`, `Checkpoint`, `Reconcile`, and `Flush`. It collapses only an uninterrupted run of checkpoints to its newest snapshot. Restore, reconcile, and flush are strict barriers. It has no desired/durable counters.

Restore disables commands, restores, writes the normalized snapshot before checkpoint observation, then enables commands before `Ready` or `FailedSafe`. Reconcile commands received during restore remain queued and execute after it. Callers publish library data only once reconcile has completed.

Malformed reads use empty/default. Every restore exit uses a finally-equivalent completion path that reenables commands and completes its reply. A non-corruption `store.read()` I/O exception enters process-lifetime `FailedSafe`, applies empty paused controller state, stops persistence attempts, and completes its restore reply without hanging. Restore load failure applies the same empty paused fail-safe controller state. A save failure retains the last durable snapshot, enters process-lifetime `FailedSafe`, stops saves, and completes all pending flush/reconcile calls with safe results. Failed-safe reconcile still updates paused in-memory state and allows publication, returning `FailedSafeApplied` without saving. Normal reconcile returns `Applied`.

### Process lifecycle

`RhythHausApplication` initializes Android context/Koin. iOS/JVM keep their process singleton startup. Compose does not release or flush controller/coordinator. `PlaybackProcessLifecycle.restoreOnce` creates one process-scoped deferred/job attempt while holding a mutex. Every caller awaits that same attempt; cancelling a caller must not cancel or reset it. The behavior runs once per process.

## Risks and mitigations

- Stale platform callbacks: immutable generations plus Android observable-MediaItem-token equality and iOS/JVM source invalidation.
- Remote-control restore bypass: Android production service/bridge gate, iOS pure decision gate, macOS native gate, and host/platform tests.
- Partial or hostile stored input: bounded codec, corruption handler, all-or-empty decode.
- Read/save failure or blocked flush: a single failure transition restores an empty paused state when needed, completes waiters, and suppresses future saves.
- Library publication before playback reconciliation: FIFO reconcile completion precedes publication.

## Non-Goals

No autoplay, exact shuffle-order persistence, cloud sync, migration, dependency addition, UI redesign, Windows/Linux scope, or abrupt-death durability promise.
