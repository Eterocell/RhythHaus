## Context

The current `library_track` table stores scanned metadata and stable source-local identity but has no user-owned track state. `LibraryRepository` is the public persistence boundary, `SqlDelightLibraryRepository` owns its database mapping, and Shared publishes `libraryTracks` into library and Now Playing composition. Track rows already have foreign-key lifecycle behavior through `library_track`; the current schema is version 2 and migrations are required for persisted schema changes.

Favorites crosses `:core:database`, library API/implementation, Shared publication, library UI, and Now Playing. The feature must preserve the contract-first dependency direction: feature implementations do not depend on Shared, while Shared composes library and Now Playing behavior.

## Goals / Non-Goals

**Goals:**

- Store one favorite boolean relationship per library track with foreign-key cleanup.
- Keep favorite reads and toggles authoritative and coherent across library, Favorites browsing, and Now Playing.
- Make the Favorites presentation reuse existing track grouping, row activation, and playback queue semantics.
- Keep mutations serialized with the existing App-owned library operation/publication boundary where publication races could otherwise expose stale state.
- Cover migration, persistence, lifecycle cleanup, accessible toggle state, empty state, and queue behavior with focused tests.

**Non-Goals:**

- Play history, play counts, recently played/added, sorting/filtering, smart playlists, metadata overrides, or file-tag writes.
- New platform permissions, source access, scan behavior, or playback-engine APIs.
- A separate Favorites database/table of copied track metadata.

## Decisions

### Persist a relation keyed by `trackId`

Add a `track_favorite` table with `trackId` as its primary key and a foreign key to `library_track(id) ON DELETE CASCADE`. The relation stores only the track identity and a deterministic `favoritedAtEpochMillis` value for future ordering/diagnostics; current Favorites ordering remains the standard library title/artist ordering. Use a schema migration from version 2 and include the migration in the same change set. Query APIs provide all favorite IDs, membership for a track, and an idempotent insert/delete toggle boundary.

A separate boolean column on `library_track` was rejected: it would mix user-owned state into scanner upserts and risks resetting favorites when metadata is refreshed. A copied favorite-track table was rejected because it duplicates authoritative library metadata and complicates deletion reconciliation.

### Keep API projections narrow

Expose `favoriteTrackIds(): Set<String>` and `setTrackFavorite(trackId, favorite): Boolean` (or an equivalent result that distinguishes absent tracks from a changed state) on `LibraryRepository`. The implementation validates track existence inside the same database operation before inserting; absent IDs cannot create rows. Shared derives `isFavorite` from the authoritative track-ID set and passes callbacks downward rather than exposing SQL/database types to UI modules.

### Publish state through Shared's existing authoritative snapshot

Shared owns the mutable favorite-ID projection alongside `libraryTracks`. A toggle executes on the existing app scope, reloads the repository projection, and republishes the combined track/favorite state. A failed or stale operation leaves the prior authoritative projection intact and reports no false success. Source scans and destructive library mutations reload favorite IDs together with tracks; cascade deletion is therefore reflected without a parallel reconciliation path.

### Reuse the library presentation and use explicit destination state

Add `Favorites` as a library-owned browse destination, with the filtered authoritative list passed to existing track grouping/rows and playback selection helpers. Album and artist detail views retain their context but receive the same favorite-ID projection. Favorite controls use an explicit checked semantic state and are absent from selection-only gestures. Empty Favorites is a normal content state, not a source/import mutation state.

Now Playing receives a narrow favorite projection and callback from Shared. It must render the action only when its current track still exists in the authoritative library and must not change queue or playback state.

### Atomicity and concurrency

Favorite mutation is a small user-state operation, not a scan operation, but its publication must not overwrite a newer library publication. Capture the current library revision or use the existing publication owner/token seam, perform the repository mutation off the UI thread, then publish only if the authoritative owner still accepts the result. Repeated toggles serialize through the Shared scope so the final persisted state matches the latest accepted user action.

## Data Flow

1. Repository startup loads tracks and favorite IDs.
2. Shared publishes both in one authoritative library projection.
3. Library/Now Playing renders favorite state and emits a track ID plus desired state.
4. Shared performs the repository mutation, reloads the favorite-ID set, and publishes the new projection.
5. Rescan/remove-source/clear/remove-missing reload tracks and favorite IDs together; database foreign keys remove deleted relations.

## Verification

- SQLDelight generated schema and migration tests prove version-2 databases migrate, favorites survive restart/rescan updates, and deletion cascades.
- Repository tests prove idempotent set/unset, absent-track rejection, and favorite-ID reads.
- Shared/library tests prove Favorites filtering, queue selection, cross-surface propagation, and no playback-state mutation.
- Compose semantics tests prove checked/unchecked labels, inactive-control absence, and empty Favorites.
- Run focused JVM/database/Android-host/iOS compilation coverage plus standalone Spotless, Detekt, architecture, strict OpenSpec validation, and `git diff --check`.
