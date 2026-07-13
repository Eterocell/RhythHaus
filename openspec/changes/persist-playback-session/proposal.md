## Why

RhythHaus loses playback context on process restart. Local playback should recover queue and listening position without starting audio unexpectedly or allowing platform transport controls to bypass startup safety.

## What Changes

- Persist one bounded ID-only snapshot containing queue IDs, current ID, position, `RepeatMode`, and `ShuffleMode` in `playback_session.preferences_pb`.
- Restore only after authoritative library content is available, reconcile missing tracks, regenerate runtime shuffle order, and always finish paused with no autoplay.
- Make controller generations authoritative for load, clear, and all stale-capable callbacks. Make each platform load acknowledge ready paused media.
- Gate controller commands and Android service/bridge, iOS remote, and JVM/macOS native platform transport during restore.
- Serialize restore, checkpoint, reconcile, and flush using one FIFO coordinator with checkpoint-only consecutive coalescing and explicit safe failure behavior.
- Make Android process startup own context/Koin, retain process singletons on iOS/JVM, and behaviorally test concurrent/repeated restore-once calls.

## Capabilities

### New Capabilities
- `playback-session-persistence`: durable local session storage, generation-safe paused restore, transport-safe reconciliation, and FIFO checkpoint persistence.

### Modified Capabilities

None.

## Impact

- Shared playback controller, platform engines, process lifecycle, Koin graph, and library refresh publication ordering.
- Preferences DataStore factories and pure session codec, without SQLDelight changes.
- Common, JVM, concrete iOS, and Android-host production-service tests for existing playback/transport behavior plus persistence safety.
- No new dependency, schema migration, UI redesign, cloud sync, Windows/Linux work, or abrupt-death durability guarantee.
