## Why

RhythHaus has local discovery, playback, and playlists, but no durable low-friction way to mark tracks for repeated listening. Favorites is the first Phase 2 daily-use capability and establishes the persisted user-relationship boundary needed by later history, smart playlists, and sorting without coupling those deferred features into this change.

## What Changes

- Add a durable, track-ID keyed favorite relation to the SQLDelight library database, including a migration from the current schema.
- Expose a narrow library API for reading favorite IDs and atomically toggling one track's favorite state.
- Publish favorite state with the existing authoritative library snapshot so UI never derives it from stale local toggles.
- Add a Favorites browse mode and accessible favorite toggles to the existing library browser, album/artist detail tracks, and Now Playing.
- Preserve favorite relationships across rescans that update an existing source-local track; cascade removal only when the owning track is removed.
- Keep playback, source/file mutation, metadata editing, history, smart playlists, and new Settings navigation out of scope.

## Capabilities

### New Capabilities

- `track-favorites`: durable favorite state, repository behavior, library presentation, and cross-surface toggle behavior.

### Modified Capabilities

- `local-library-scanning`: track lifecycle requirements now preserve or remove favorite relationships consistently with persisted track identity.

## Impact

- `:core:database`: one SQLDelight relation, queries, migration, and migration coverage.
- `:feature:library:api` and `:feature:library:impl`: favorite contracts, repository implementation, presentation model, and library UI.
- `:shared`: authoritative publication and callbacks into existing Library and Now Playing composition.
- `:feature:nowplaying`: a favorite action projected from Shared without a dependency on library implementation.
- Shared and feature Compose/JVM tests, plus platform compilation verification.
