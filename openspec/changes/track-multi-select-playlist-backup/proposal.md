## Why

Per-track Add to Playlist buttons consume row space and make adding several tracks repetitive. Saved playlists also cannot currently be backed up or restored across devices without relying on unsafe transfer of device-local database state.

## What Changes

- Replace per-row Add to Playlist buttons on Library home Songs, album detail, artist detail, and Search with page-scoped long-press multi-selection.
- Show checkboxes during selection and temporarily replace the Now Playing bar with a contextual bar that adds the ordered selection to an existing or newly created playlist.
- Add Settings actions to export all saved playlists and import a versioned logical playlist backup through system document panels on Android, iOS, and desktop JVM/macOS.
- Match imported entries uniquely against destination-library title, artist, album, and duration metadata; report unmatched and ambiguous entries instead of guessing.
- Import restorable playlists as new, conflict-free local copies in one transaction while preserving playlist order, entry order, and duplicate occurrences.
- Explicitly exclude raw SQLite database copying or replacement, audio/media transfer, cloud synchronization, and selection on playlist detail, Queue, or Now Playing.

## Capabilities

### New Capabilities
- `track-multi-selection`: Page-scoped long-press selection, checkbox semantics, contextual bottom-bar behavior, ordered submission, and dismissal rules for eligible track lists.
- `playlist-backup`: Versioned bounded logical export/import, metadata matching, preview, conflict handling, transactional restore, system document integration, and result reporting.

### Modified Capabilities
- `saved-playlists`: Replace the per-row single-track Add to Playlist entry points with the generalized ordered multi-track picker while preserving the playlist-detail searchable browser.

## Impact

- Shared selection and playlist UI under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/` and `search/`.
- New shared backup domain/codec/service code and platform document adapters under Android, iOS, and JVM source sets.
- Settings and app-shell orchestration, localized English/Chinese resources, accessibility semantics, and platform file-panel bridges.
- `PlaylistRepository` gains a multi-playlist transactional import operation; the persisted SQLDelight schema does not change and requires no migration.
- New common, JVM, Android-host, and iOS tests plus runtime/visual QA for gestures, contextual chrome, system panels, and import reporting.
