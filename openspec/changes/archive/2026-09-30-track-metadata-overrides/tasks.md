# Tasks

## 1. Durable metadata boundary

- [x] 1.1 Add SQLDelight `track_metadata_override` relationship, v5→v6 migration, schema snapshots and legacy-version-zero upgrade support; prove migration, cascade and retained raw tags with database tests.
- [x] 1.2 Add the typed override/editor contract and repository write/read/effective projection to SQLDelight and in-memory implementations; prove partial edits, rescan, revert, sort, invalid/missing IDs and deletion in repository tests, and update all repository consumers.

## 2. Playback metadata continuity

- [x] 2.1 Add a controller operation that refreshes queued metadata by track ID without restarting playback or losing occurrence identity, generation, progress, repeat/shuffle, or active timer; prove concurrent replacement and checkpoint semantics.
- [x] 2.2 Refresh loaded system media metadata where Android, iOS, and macOS adapters support a no-reload update, otherwise use effective metadata on the next load; prove platform bridge behavior, compile each target, and record any platform API limitation explicitly.

## 3. Application and editor

- [x] 3.1 Add revision-serialized metadata mutation and fresh projection to Shared `App` without scan preemption; prove scan/edit races, stale publication rejection, and playback metadata refresh using application-layer tests.
- [x] 3.2 Add the accessible Library track-row edit action and editor (raw/effective values, partial updates, restore-one/all, save/cancel/errors) and EN/ZH resources; prove compact and split UI actions and disabled states.
- [x] 3.3 Integrate editor lifecycle with Shared routing/Back so a stale completion cannot mutate a replacement editor, and verify current queue plus playlists/search/grouping reflect committed changes without changing media source.

## 4. Acceptance and handoff

- [x] 4.1 Run database generation/migration tests, Library/Shared/Core playback tests, three-platform compilation, formatting, static/architecture checks, and strict OpenSpec validation; review full diff and record observed limitations.
- [x] 4.2 Synchronize the canonical specs, archive this change only after verified tasks are complete, update `roadmap.md` and `progress.md`, and preserve user-owned physical-device UI/listening acceptance as an explicit pending item.
