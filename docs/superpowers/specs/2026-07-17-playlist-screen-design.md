# Playlist Screen Design

## Summary

RhythHaus will add a shared Playlist destination that combines durable saved playlists with the live playback queue. The destination has `Saved` and `Queue` tabs. Saved playlists support creation, rename, deletion, duplicate track entries, ordered editing, and playback. The Queue tab exposes the controller-owned current queue while keeping the active occurrence pinned and allowing users to reorder, remove, or clear only upcoming occurrences.

The feature is shared-first across Android, iOS, and desktop JVM/macOS. Saved playlist data uses the existing SQLDelight database through a dedicated `PlaylistRepository`; live queue state remains owned by `PlaybackController` and persisted through the existing playback-session store.

## Goals

- Add a Library entry that opens a playlist hub with `Saved` and `Queue` tabs.
- Add saved-playlist create, rename, delete, add, remove, and reorder workflows.
- Support both per-track `Add to playlist` actions and a searchable multi-select browser from playlist detail.
- Allow the same library track to appear multiple times as distinct playlist entries.
- Preserve duplicate occurrences when a saved playlist becomes the playback queue and after process restart.
- Let users reorder, remove, and clear upcoming queue occurrences without interrupting the current track.
- Remove saved entries automatically when their underlying local-library track is deleted.
- Preserve existing visible-list playback semantics, repeat mode, shuffle mode, playback generation safety, and paused session restoration.

## Non-goals

- Smart, generated, collaborative, or cloud-synchronized playlists.
- Playlist folders, sharing, import/export, remote catalogs, or streaming tracks.
- Editing track metadata or retaining unavailable track metadata snapshots.
- Moving or removing the currently playing queue occurrence from the Queue tab.
- Replacing the existing playback-session DataStore with SQLDelight queue persistence.
- New platform-specific playlist behavior, dependencies, or Windows/Linux product support.

## Current State

`LibraryRepository` owns local sources, scanned tracks, scan sessions, and scan errors. The shared SQLDelight database has no playlist tables or checked-in migration files. Library navigation uses the typed `LibraryRoute` stack for home, album, artist, Search, Now Playing, and Settings surfaces.

`PlaybackState` currently represents its queue as `List<PlayableTrack>` and identifies the current item by track identity. Playback restore and reconciliation call `distinct`/`distinctBy` on track IDs, and the session codec rejects duplicate IDs. A saved playlist with duplicate entries therefore requires occurrence identity throughout queue state, selection, shuffle order, checkpoints, persistence, restore, and reconciliation.

## Navigation and Screens

### Playlist hub

Library home adds a `Playlists` entry that pushes a typed playlist-hub route. The hub uses two tabs:

- `Saved` lists playlists in deterministic repository order with name and track count. It exposes a create action and empty state.
- `Queue` renders the current playback occurrence first and all upcoming occurrences after it. With no active queue, it renders a distinct empty state.

The existing Now Playing bar remains controlled by the route-aware visibility policy and keeps its current transport behavior.

### Saved playlist detail

Selecting a saved playlist pushes a route keyed by playlist ID. The detail screen shows the playlist name and ordered entries. It provides rename and delete actions, an add-tracks action, row playback, row removal, and drag reorder. Deleting the final entry leaves an empty saved playlist. Deleting the playlist requires confirmation and returns to the hub after a successful repository mutation.

A stale playlist route that no longer resolves returns to the hub and presents an actionable message rather than an empty or partially interactive detail screen.

### Add-track workflows

Track rows on Library home, Search, album detail, and artist detail add an overflow action named `Add to playlist`. The action opens a picker of existing playlists and supports inline playlist creation. Every confirmed add appends a new entry, including when the selected playlist already contains the same track.

Playlist detail also opens a searchable multi-select browser over the current authoritative library. Confirming selection appends one occurrence for each selected track in the browser's visible order. Selection state is keyed by library track ID; the resulting saved rows receive independent playlist-entry IDs.

### Queue editing

The current occurrence is visually distinct and pinned. It has no remove or drag affordance. Upcoming occurrences support drag reorder and removal. `Clear upcoming` removes all upcoming occurrences while leaving the current occurrence loaded and playable at its existing position.

Queue edits do not change repeat mode, shuffle mode, current playback position, engine generation, or play/pause status. They enter the same serialized controller ownership boundary as other queue and session operations.

## Domain and Persistence Design

### Repository boundary

Add a shared `PlaylistRepository` separate from `LibraryRepository`. It has focused in-memory and SQLDelight implementations and owns playlist and playlist-entry CRUD, ordered reads, counts, and transactions. Both repositories use the existing `RhythHausDatabase`, preserving one database transaction domain without mixing playlist methods into source scanning and track lifecycle APIs.

### Data model

The durable model contains:

- `Playlist`: stable ID, user-visible name, created timestamp, and updated timestamp.
- `PlaylistEntry`: stable occurrence ID, playlist ID, library-track ID, zero-based position, and created timestamp.

Playlist names are trimmed and must be non-empty. Names are not required to be unique. A playlist entry is identified by its own ID, not by `(playlistId, trackId)`, so duplicate track occurrences are valid and independently reorderable/removable.

Deleting a playlist cascades all of its entries. Deleting a library track cascades entries that reference it while preserving the containing playlist. Source removal, rescan cleanup, and clear-library operations therefore remove affected entries atomically with their tracks. Reads defensively omit unresolved entries even though foreign-key cascades are authoritative.

### Ordering and transactions

Playlist entries use a deterministic contiguous position sequence. Add appends after the current final position. Remove and reorder rewrite the complete resulting sequence inside one transaction. The repository publishes new state only after the transaction succeeds; it does not expose optimistic partial ordering.

### Database migration

The schema change must include a SQLDelight migration in the same change set. Existing Android, iOS, and JVM/macOS installations must upgrade without losing library tracks, sources, scan history, or playback-session data. Platform database factories must use the migration-capable schema path rather than recreating an existing database or assuming only fresh schema creation. JVM upgrade tests must open a pre-change schema, migrate it, verify old rows, and exercise playlist foreign keys and cascades.

## Occurrence-aware Playback

### Queue identity

Introduce a queue occurrence model with a stable occurrence ID and a `PlayableTrack`. Track identity continues to identify the local media item; occurrence identity identifies one ordered queue slot. Playback state, current selection, shuffle order, skip behavior, row highlighting, checkpoints, session snapshots, restore, and reconciliation use occurrence identity wherever queue position matters.

Home, album, artist, and Search lists generate occurrence IDs when converted into queues. Saved-playlist playback derives occurrence IDs from playlist-entry IDs, preserving duplicates and exact visible order. Selecting the second occurrence of the same track starts that occurrence rather than resolving to the first matching track.

### Session persistence and compatibility

The playback-session snapshot persists ordered occurrence IDs together with their library-track IDs and identifies the current occurrence explicitly. Restore validates occurrence uniqueness while allowing repeated track IDs. Legacy track-ID-only snapshots migrate or normalize into deterministic unique occurrences without autoplay and without losing valid queue membership.

Reconciliation resolves every occurrence against the authoritative track map, drops only occurrences whose track no longer exists, preserves surviving duplicate occurrences, and keeps the current occurrence without reload when possible. Runtime shuffle order is regenerated from occurrence IDs.

### Saved playlist playback

Selecting a playlist entry builds the queue from the exact visible playlist-entry order and starts the selected occurrence. Selecting the active occurrence restarts it according to the existing current-row selection contract without replacing the queue. Selecting another occurrence replaces the queue with the visible playlist occurrences and begins the selected one. The same visible-list policy remains in force for Library home, album, artist, and Search.

### Queue mutation commands

`PlaybackController` owns serialized commands to reorder upcoming occurrences, remove an upcoming occurrence, and clear upcoming occurrences. Each command validates against the latest state inside the controller boundary. Stale IDs, attempts to mutate the current occurrence, and invalid target positions are no-ops with a recoverable result for UI messaging.

Successful commands publish one complete state update and emit an immediate playback-session checkpoint. They preserve current track loading, playback position, play/pause status, repeat mode, shuffle mode, engine generation, and stale-callback protections.

## Data Flow and Consistency

Saved-playlist screens observe repository snapshots. Repository writes complete transactionally before UI state is replaced. Playback actions resolve visible saved entries into queue occurrences and then call controller APIs; Composables do not mutate SQL rows or `PlaybackState.queue` directly.

Library source removal and clear-library operations delete authoritative database rows first, allowing playlist-entry cascades to complete. They then reconcile playback against surviving library tracks through the existing FIFO playback-session coordinator before publishing refreshed library and playlist UI state.

Deleting or editing a saved playlist does not retroactively mutate an already active queue. The controller owns a resolved queue snapshot. Later library-track deletion still removes unavailable occurrences during normal authoritative reconciliation.

## Error Handling

- Failed create, rename, delete, add, remove, or reorder operations retain the last confirmed repository state and expose a recoverable message.
- Create and rename dialogs retain entered text after a failed save.
- Empty or whitespace-only names are rejected before repository mutation.
- A stale playlist or entry ID never falls back to another playlist, track, or first queue item.
- A failed playlist read exposes retry rather than rendering successful empty content.
- Queue commands rejected because state changed concurrently refresh from controller state and explain that the queue changed.
- Playback-load and session-store failures keep their existing controller/coordinator fail-safe semantics; playlist persistence does not become a second playback source of truth.

## Accessibility and Responsive Behavior

Interactive controls use semantic labels that include the playlist or track name. Saved and queue rows use occurrence IDs as Compose keys so duplicate tracks remain independently targetable. Drag reorder has an accessible non-gesture alternative for moving entries up or down. Destructive actions require explicit confirmation and do not rely on color alone.

Compact and wide layouts preserve the existing shared Library chrome and safe-content behavior. Text must not overlap row actions, tab labels, or the Now Playing bar. The implementation reuses current track-row, artwork, top-bar, scrollbar, and responsive-width conventions rather than introducing a separate visual system.

## Testing and Verification

TDD coverage includes:

- Playlist repository CRUD, non-unique names, duplicate entries, deterministic order, normalized positions, transaction rollback, and empty-playlist retention.
- SQLDelight migration from the prior schema, foreign-key enforcement, playlist deletion cascade, source/track deletion cascade, and clear-library behavior.
- Occurrence-aware queue creation, selecting the second duplicate, skip/shuffle behavior, checkpoint encoding, legacy snapshot normalization, duplicate restore, and reconciliation.
- Pinned-current queue rules, upcoming reorder/remove/clear, stale command rejection, concurrent mutation serialization, and repeat/shuffle/position/status preservation.
- Hub tabs, typed routes, stale-route recovery, create/rename/delete dialogs, both add workflows, duplicate row keys, empty/loading/error states, confirmation, and exact visible-order playback.

Completion evidence must include:

```bash
openspec validate playlist-screen --strict
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

The known iOS common-test `Thread` blocker must be reported exactly if it remains; no iOS simulator pass may be claimed from main compilation alone. Manual QA covers compact and wide layouts, light and dark themes, duplicate rows, drag and accessible reorder, dialog keyboard behavior, current-track pinning, queue edits during playback, Now Playing bar overlap, and target-device audible queue behavior.

## Implementation Slices

The approved design should be implemented through independently reviewed subagent-driven tasks:

1. SQLDelight schema, migration-capable platform database opening, playlist repository, and repository/migration tests.
2. Occurrence-aware playback state and session persistence compatibility, with controller/session tests.
3. Serialized upcoming-queue mutation commands and tests.
4. Playlist routes, hub/detail state, and navigation tests.
5. Saved playlist UI, CRUD, reorder, and both add-track workflows with UI-state tests.
6. Queue tab UI, accessible reorder controls, and controller wiring.
7. Cross-feature integration, strict OpenSpec validation, platform verification, visual/manual QA evidence, roadmap/progress updates, and final review.
