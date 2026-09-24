# Design

## Context

See `proposal.md` for motivation. `library_track` already owns stable scanned metadata, `createdAtEpochMillis`, and a source-local identity that survives a metadata refresh. Favorites established the pattern of separate listener-owned relations, narrow `LibraryRepository` methods, and Shared's serialized `AuthoritativeLibraryPublicationOwner`. `PlaybackController` already exposes an immutable `StateFlow<PlaybackState>` with a monotonic engine generation and authoritative `Playing` state, but no domain event identifies a generation's first actual playback.

The App owns source scan/destructive-mutation admission and publishes library tracks into Library and Now Playing. Feature implementations must not depend on Shared or each other. Existing Albums/Artists/Songs/Favorites selection and Back behavior treats Songs and Favorites as flat home lists.

## Goals / Non-Goals

**Goals:**
- Persist user-owned count and last-played data without scanner upserts resetting it.
- Count each admitted, actual-playing generation once across all platforms.
- Keep history writes and combined library publications race-safe with scans and destructive mutations.
- Reuse flat-library row, selection, queue, localization, and accessibility seams for recent modes.

**Non-Goals:**
- Playback-duration thresholds, partial-play accounting, manual history editing/clearing, statistics charts, recommendations, smart playlists, history sync, or media-tag writes.
- New platform audio callbacks, source permissions, import paths, or navigation roots.
- Duplicating `createdAtEpochMillis` in history storage.

## Decisions

### Persist a dedicated `track_play_history` relation

Add a table keyed by `trackId` with `playCount INTEGER NOT NULL` and `lastPlayedAtEpochMillis INTEGER NOT NULL`, foreign-keyed to `library_track(id) ON DELETE CASCADE`. The repository records a passed-in timestamp atomically: it validates the track exists, inserts `(1, timestamp)` when absent, or increments/replaces the timestamp when present. An absent track returns `false` with no row. A separate relation avoids scanner upserts modifying user state; the existing `createdAtEpochMillis` remains the sole Recently Added source.

A boolean/columns on `library_track` is rejected because scanner updates own that row. A raw append-only event log is rejected for v1 because it grows unbounded and provides no product behavior beyond the aggregate count and latest timestamp.

### Make the controller emit one actual-play admission per generation

Introduce a small platform-neutral `PlaybackStarted` flow/event from `PlaybackController`, carrying the active generation, current occurrence ID, and track ID. Emit it under the controller's ownership gate only when a status transition results in `PlaybackStatus.Playing` for the current generation and occurrence; retain the last emitted generation/occurrence identity to suppress repeated engine `Playing` callbacks. Resetting/replacing/loading a new generation permits a new event.

Shared owns collection of this event. It captures a wall-clock epoch timestamp at receipt, serializes history persistence through a dedicated App history admission mutex, records only IDs still present in the repository, and publishes a reconciled library projection. Platform engines continue reporting existing status callbacks; no listener API expansion or new engine ownership path is introduced.

An App-side `PlaybackState` observer is rejected: recomposition/collector restarts cannot prove exactly-once delivery. Counting load selection is rejected because a selected file can fail or never start.

### Reconcile history inside authoritative publication ownership

Extend `LibraryContentState` and `AuthoritativeLibraryPublication` with an immutable `Map<String, TrackPlayHistory>`. Every startup, scan, remove-missing, source-removal, and clear reload obtains tracks, favorites, and history through repository projections. History writes enter the same publication owner serialization used for favorites, re-read the history map after persistence, and publish only the accepted combined snapshot. Repository cascade deletion is authoritative; stale keys are not retained in UI state.

History recording is not a scan and must neither cancel nor preempt scans. It serializes its publication step with library lifecycle work, preserving the existing coordinator's destructive-operation ownership.

### Model recent modes as flat projections

Add `RecentlyPlayed` and `RecentlyAdded` after `Favorites` in `BrowseMode`. The library feature receives its existing raw `List<Track>` plus a narrow immutable history map. A pure visible-track helper filters/sorts: Recently Played requires a map value and orders timestamp descending, title/artist ascending; Recently Added orders by a separate Shared-provided immutable track-ID-to-created-time map with the same tie-breakers. Both render through `TrackRow` as flat lists and pass their visible order to the existing playback callback.

Keeping recently added time in `Track` is rejected because it would widen the cross-feature audio display model with persistence-specific metadata. The Shared boundary instead adapts `LibraryTrack.createdAtEpochMillis` into a narrow projection. Albums and Artists retain standard ordering.

## Data Flow

1. Database startup returns tracks, favorite IDs, and history records.
2. Shared publishes an immutable combined snapshot to Library and Now Playing composition.
3. The controller emits one current-generation actual-playing event.
4. Shared serially records the event if the track remains indexed, reloads history, and republishes through the authoritative publication owner.
5. Library derives recent visible lists from that projection; a listener selection uses the exact displayed order.
6. Rescan preserves a surviving track's relation; accepted deletion cascades it and subsequent publication omits it.

## Risks / Trade-offs

- A user can briefly hear audio before the asynchronous repository update becomes visible; this is intentional because history writes must not block engine callbacks. The event remains durable once accepted.
- Timestamp ties are possible on coarse clocks; deterministic title/artist tie-breakers make the UI stable.
- The event-flow consumer is App-scoped and must be installed exactly once per controller lifecycle. Tests must prove no duplicate collector creates duplicate count events.
- Existing iOS simulator execution may remain blocked by Xcode; compile test code and record the limitation rather than claiming runtime coverage.