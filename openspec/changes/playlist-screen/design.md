## Context

RhythHaus has an existing SQLDelight `RhythHausDatabase` for local-library data and a DataStore-owned playback session, but no durable playlists or editable playback-queue screen. The current queue identifies slots by track ID and deduplicates IDs in session persistence and reconciliation, which cannot represent repeated tracks as independent positions.

This change adds a shared Library playlist destination for Android, iOS, and JVM/macOS. It introduces durable saved playlists and an occurrence-aware controller queue while retaining the existing local-first architecture, platform database factories, playback ownership, route-aware Now Playing visibility, and existing playback fail-safe behavior.

## Goals / Non-Goals

**Goals:**

- Provide a Playlist hub with `Saved` and `Queue` tabs and typed saved-playlist detail routes.
- Persist ordered saved playlists, including distinct duplicate track entries, through a dedicated `PlaylistRepository` that shares `RhythHausDatabase` with the library.
- Preserve queue occurrence identity through visible-list selection, playback, shuffle, skipping, persistence, restoration, and authoritative-library reconciliation.
- Allow the user to edit only upcoming queue occurrences without changing the active occurrence or transport state.
- Upgrade existing Android, iOS, and JVM/macOS databases without losing local-library or playback-session data.

**Non-Goals:**

- Cloud, smart, collaborative, generated, shared, imported, or exported playlists.
- Playlist folders, remote catalogs, streaming tracks, or new platform-specific behavior or dependencies.
- Retaining unavailable-track snapshots, editing track metadata, or moving or removing the current queue occurrence.
- Replacing the DataStore playback-session store with SQLDelight queue persistence.

## Decisions

### Dedicated playlist repository and relational occurrence rows

`PlaylistRepository` SHALL own playlist and playlist-entry reads, CRUD, ordering, and transactions while sharing the existing `RhythHausDatabase` with `LibraryRepository`. `Playlist` records use stable IDs, name, and timestamps. `PlaylistEntry` records use their own stable occurrence ID, playlist ID, library-track ID, position, and timestamp. Names are trimmed and non-empty but not unique.

The repository boundary keeps playlist concerns out of library scanning and track lifecycle APIs while preserving one transaction domain for library-track deletion and playlist-entry cascades. A composite `(playlistId, trackId)` identity was rejected because it forbids independent duplicate occurrences. Adding playlist methods to `LibraryRepository` was rejected because it couples durable user collections to source scanning and library management.

### Contiguous transactional ordering and database cascades

Entry positions SHALL be deterministic and contiguous. Adds append after the current final position. Remove and reorder operations rewrite the complete resulting sequence in one database transaction and publish a new repository snapshot only after the transaction succeeds. Foreign keys SHALL cascade entry deletion when either a playlist or referenced library track is deleted, while preserving the playlist when only an entry's track is deleted.

Atomic replacement avoids UI-observable partial order and preserves consistency during source removal, rescan cleanup, and clear-library. Sparse position updates were rejected because they complicate deterministic order and recovery. Application-managed cleanup alone was rejected because it cannot guarantee atomic cleanup with library-track deletion.

### Occurrence-aware queue and DataStore session compatibility

Queue slots SHALL contain a stable occurrence ID plus `PlayableTrack`. Queue-position semantics, including current selection, highlighting, shuffle order, skip behavior, checkpoints, and reconciliation, use occurrence IDs. Generic visible lists create new occurrence IDs; saved-playlist playback derives them from durable playlist-entry IDs so duplicates remain stable across restart.

The playback session remains DataStore-owned and persists ordered occurrence ID and library-track ID pairs plus an explicit current occurrence. Restore accepts repeated track IDs but validates occurrence-ID uniqueness. Legacy track-ID-only snapshots normalize valid rows into deterministic unique occurrences without autoplay. SQLDelight queue persistence was rejected because the established playback session is DataStore-owned and must remain the sole live queue snapshot.

### Controller-owned upcoming queue edits

`PlaybackController` SHALL serialize reorder, remove, and clear commands for upcoming occurrences. Commands validate the latest controller state and return a recoverable rejected result for stale IDs, current-occurrence mutations, and invalid target positions. A successful command emits one complete state update and an immediate session checkpoint while retaining current loading, playback position, play or pause status, repeat mode, shuffle mode, engine generation, and stale-callback protections.

Direct Compose mutation was rejected because it bypasses serialization and session checkpointing. Rebuilding the complete queue for every edit was rejected because it risks interrupting current playback and changing controller generation.

### Shared route and accessible interaction design

Library home SHALL push a playlist hub route, and selecting a saved playlist SHALL push a route keyed by playlist ID. The hub has `Saved` and `Queue` tabs. Saved detail supports create, rename, confirmed deletion, row playback, removal, drag reorder, and accessible move controls. Per-track `Add to playlist` opens a picker with inline creation; detail-side add opens a searchable multi-select browser and appends selection in visible order.

Rows use occurrence IDs as Compose keys and controls have semantic labels containing the affected playlist or track name. A stale playlist route returns to the hub with a recoverable message. A separate platform-native playlist UI was rejected because it would diverge from shared behavior and test coverage.

## Risks / Trade-offs

- [Schema upgrade failure on existing installations] -> Add a checked-in SQLDelight migration, open databases through migration-capable platform factories, and verify upgrades from a pre-change schema on JVM.
- [Duplicate tracks are accidentally collapsed] -> Remove track-ID deduplication from queue-position flows and test selection, persistence, restore, shuffle, skip, and reconciliation with duplicates.
- [Concurrent queue command targets become stale] -> Validate inside the controller serialization boundary, return a recoverable result, and refresh UI from controller state.
- [Playlist data and active queue diverge] -> Treat the active controller queue as a resolved snapshot; saved-playlist edits do not retroactively edit it, while authoritative track deletion triggers normal reconciliation.
- [Gesture-only ordering excludes keyboard and assistive users] -> Provide labeled move-up and move-down alternatives and explicit destructive confirmations.

## Migration Plan

1. Add SQLDelight playlist and playlist-entry tables, foreign keys, indexes required for ordered reads, and the corresponding migration from the current schema.
2. Update Android, iOS, and JVM/macOS database factories to open existing databases with the migration-capable schema path, preserving prior library rows, sources, scan history, and DataStore session data.
3. Validate migration from a pre-change JVM database, including retained legacy rows and playlist and track cascade behavior.
4. Release the occurrence-aware session codec with legacy snapshot normalization. Existing track-ID-only sessions remain valid queue membership, receive deterministic occurrence IDs, and restore paused without autoplay.
5. Roll back application code only before a schema-using release. After migration, forward-compatible application fixes are required because persisted playlist tables must not be dropped or recreated.

## Open Questions

No open product questions remain. The approved design fixes the repository boundary, storage ownership, route behavior, duplicate semantics, queue-edit scope, and migration requirement.
