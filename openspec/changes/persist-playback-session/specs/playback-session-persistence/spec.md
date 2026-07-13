## ADDED Requirements

### Requirement: Playback session storage is bounded, atomic, and ID-only
The system SHALL persist queue IDs, current ID, position, `RepeatMode`, and `ShuffleMode` in one edit of the dedicated `playback_session.preferences_pb` Preferences DataStore. It SHALL not persist effective shuffled order.

#### Scenario: Codec accepts supported IDs
- **WHEN** ordered IDs contain delimiters, newlines, or emoji within the exact bounds `maxIds=10_000`, `maxIdCharacters=4_096`, `maxIdUtf8Bytes=16_384`, and `maxEncodedUtf8Bytes=1_048_576`
- **THEN** `<decimal-character-count>:<ID>` round-trips their order and contents

#### Scenario: Codec rejects invalid input
- **WHEN** encoding or decoding finds an empty or duplicate ID, unpaired surrogate, malformed count, truncation, trailing input, or bound violation
- **THEN** decode uses the empty/default snapshot
- **AND** encode fails before `DataStore.edit`, preserving a previous valid snapshot

#### Scenario: Preferences data is corrupt
- **WHEN** the DataStore file cannot be read
- **THEN** `ReplaceFileCorruptionHandler { emptyPreferences() }` supplies an empty/default session

### Requirement: Paused engine loads have controller-owned provenance
The system SHALL make `PlaybackController` the sole monotonically increasing allocator of load and clear generations.

#### Scenario: Platform load acknowledges paused readiness
- **WHEN** the controller requests `loadPaused(track, generation)`
- **THEN** the engine clears native play intent before prepare/load and returns `LoadedPlayback(generation, durationMillis)` only after ready media exists
- **AND** it performs and reports no playing action or callback

#### Scenario: Restore applies position without autoplay
- **WHEN** a reconciled current item restores
- **THEN** the controller runs loadPaused, clamps position, seeks, and pauses with state `Paused`
- **AND** it never calls play

#### Scenario: Callback provenance is stale
- **WHEN** status, progress, completion, error, or skip callback generation differs from the active controller generation
- **THEN** the controller ignores it without changing state or emitting a checkpoint

#### Scenario: Android same-track requests overlap
- **WHEN** two loads of the same Android track use different generations
- **THEN** Android accepts READY, status, progress, completion, and error only while the observable current `MediaItem` request token equals the active generation-containing token

#### Scenario: iOS or JVM source becomes stale
- **WHEN** a later load or clear occurs
- **THEN** iOS/JVM delegate, progress, and completion sources created for older generations are invalidated

### Requirement: Restore gates controller and platform transport
The system SHALL disable controller commands and platform transport before restore and reenable them before `Ready` or `FailedSafe`.

#### Scenario: Controller command is disabled
- **WHEN** commands are disabled
- **THEN** public queue, selection, repeat/shuffle, play, pause, stop, seek, restart, and skip commands perform no mutation, engine action, or checkpoint emission

#### Scenario: Platform play or seek arrives during restore
- **WHEN** a platform transport request arrives while transport is disabled
- **THEN** the process-shared Android gate makes `RhythHausPlaybackService`/`RhythHausTransportBridge` and `SkipRoutingPlayer` expose no relevant available command and reject/no-op play, pause, stop, seek, next, and previous before native action
- **AND** iOS `IOSRemoteTransportGate` returns command failed for play and seek with no provider action
- **AND** JVM/macOS `MacAudioPlayerBridge` retains a transport-enabled Boolean independently of its native handle, reapplies it at reset/native-handle creation before remote registration or acceptance, and makes every native remote handler disabled or ignored

#### Scenario: macOS disabled transport survives paused load reset
- **WHEN** macOS transport is disabled before `loadPaused`, which resets or creates its native handle
- **THEN** retained disabled state is applied to that handle before remote commands register or execute
- **AND** remote play and seek are rejected with no native effect until an explicit reenable
- **AND** the enabled remote path works after explicit reenabling

### Requirement: Restore and reconciliation retain a paused reconciled session
The system SHALL restore once after authoritative library content is available, reconcile queue IDs to that library, regenerate runtime shuffle order, and retain valid modes.

#### Scenario: Current track survives reconciliation
- **WHEN** the current ID remains in the reconciled queue
- **THEN** reconciliation updates queue/runtime shuffle state without reload or position change

#### Scenario: Current track is missing or all tracks are missing
- **WHEN** the current ID is missing but another queued ID survives
- **THEN** the first survivor loads paused at zero
- **WHEN** no queued ID survives
- **THEN** controller clears queue/current and engine with a new generation while preserving modes

#### Scenario: Restore load fails
- **WHEN** paused loading fails during restore
- **THEN** the controller applies an empty paused fail-safe state and commands reenable

### Requirement: Complete checkpoints use defined coalescing
The system SHALL emit complete snapshots immediately for queue/current, seek, pause/stop, repeat/shuffle, restore normalization, and reconciliation.

#### Scenario: Playing progress repeats in one bucket
- **WHEN** progress repeats for the same active generation, current track ID, and whole-second bucket
- **THEN** at most one progress checkpoint is emitted
- **AND** the key resets on load, selection, seek, stop, restore, and reconciliation

### Requirement: FIFO persistence produces safe completion
The system SHALL serialize restore, checkpoint, reconcile, and flush with one FIFO actor. It SHALL collapse only consecutive checkpoints to the newest snapshot; restore, reconcile, and flush SHALL be barriers.

#### Scenario: Reconciliation arrives during restore
- **WHEN** a caller requests reconciliation while restore is running
- **THEN** the actor queues it after restore
- **AND** the caller publishes authoritative library content only after it completes

#### Scenario: Normal restore completes
- **WHEN** a restore succeeds
- **THEN** the actor saves `sessionSnapshot()` after normalization and before collecting checkpoints
- **AND** later reconciliation returns `Applied` after its save

#### Scenario: Read or save fails safe
- **WHEN** persisted data is malformed or corrupt
- **THEN** restore uses empty/default data
- **WHEN** `store.read()` throws an unexpected non-corruption I/O exception
- **THEN** restore completes its reply, reenables controller and platform commands, applies empty paused controller state, and enters process-lifetime `FailedSafe` with persistence attempts stopped
- **WHEN** a save fails
- **THEN** the coordinator preserves the last durable snapshot, enters process-lifetime `FailedSafe`, stops persistence attempts, and completes pending flush/reconcile callers without hanging
- **AND** failed-safe reconciliation updates paused in-memory controller state, permits publication, skips saving, and returns `FailedSafeApplied`

### Requirement: Playback graph is process-owned
The system SHALL initialize Android context/Koin in `RhythHausApplication`, retain iOS/JVM singleton startup, and keep controller/coordinator alive across Compose recreation.

#### Scenario: Restore once is invoked concurrently or repeatedly
- **WHEN** `PlaybackProcessLifecycle.restoreOnce` is called simultaneously or later in the same process
- **THEN** it executes restore behavior exactly once

#### Scenario: First restore waiter is cancelled
- **WHEN** the first caller is cancelled after its shared restore attempt has started and another caller invokes `restoreOnce`
- **THEN** the shared attempt continues without reset or replacement
- **AND** the second caller awaits it
- **AND** coordinator restore executes exactly once

#### Scenario: Process ends abruptly
- **WHEN** the process ends before a checkpoint write finishes
- **THEN** that write may be lost and the system makes no stronger durability claim
