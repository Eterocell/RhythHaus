# Design

## Context
`PlaylistRepository` stores static playlists and `PlaylistStateOwner` serializes reads/writes and revisioned publication. Shared owns `LibraryContentState` (tracks, favorite IDs, play history), route identity and playback selection. The Saved hub contains static playlists, while Queue can already save a static playlist with repeated occurrences. Smart playlists need durable definitions, not duplicated membership rows.

## Goals / Non-Goals

**Goals:** named user-editable single-rule lists for Favorites, newest N plays/additions, exact Artist, exact Artist+Album, and a referenced static saved playlist; deterministic live membership; playing a derived order; EN/ZH accessible compact UI; migration safety.

**Non-Goals:** Boolean rule builders, wall-clock recency, periodic jobs, source-track/tag mutations, smart-list backup in v1, smart lists as nested smart-list sources, arbitrary manual entry edits, or a new navigation authority.

## Decisions

1. Introduce `SmartPlaylistRule` typed variants and `SmartPlaylistSummary` in playlists API; extend the existing `PlaylistRepository` with `smartPlaylists()`, `createSmartPlaylist(name,rule)`, `updateSmartPlaylist(id,name,rule)` and `deleteSmartPlaylist(id)`. SQLDelight `smart_playlist` and `4.sqm` migration store stable kind and argument fields, not derived rows. Validate every write and decode every read; unknown kind/count/blank arguments throw so `PlaylistStateOwner.refresh` publishes `ReadFailed` rather than a false empty list. `SavedPlaylist(sourceId)` write validates an existing static source; after source deletion the smart rule remains as a missing-source entry. Atomic update checks the target exists before mutation. Legacy version-zero JVM bootstrap detects v4 table set explicitly (not via current schema version) and migrates to v5. The iOS and Android SQLDelight schema use the same migration.
2. Add `smartPlaylists: List<SmartPlaylistSummary>` to `PlaylistSnapshot` and load it under the existing `PlaylistStateOwner` lock. Keep static `playlists` and `entriesByPlaylistId` unchanged for JSON backup and all existing callers. `SmartPlaylistRow(occurrenceId,trackId)` projects members from current authoritative tracks/favorites/history plus confirmed static entries in a pure function in the Playlists implementation. Stable unique occurrence IDs use the static entry ID for SavedPlaylist and track ID for set rules, scoped to smart playlist identity; missing source is a distinct presentation, not a new persisted state. Sort before take(N), ties by stable ID. Smart derived data never writes to the database.
3. Saved hub shows a separate Smart heading/create control and distinguishable rows. A feature-owned named rule editor validates/displays single-rule parameters and uses current artist/album/static-source options. It is destination-scoped via existing feature dismissal publisher; failed async mutation cannot dismiss a replacement modal. Detail is read-only for derived entries and has Rename/change rule/Delete, with confirm; no static browser/edit controls. Shared introduces `LibraryRoute.SmartPlaylistDetail(id)` with the same Back and destination instance policy as static detail; missing/deleted rule returns to hub only after a confirmed snapshot. Shared passes `libraryContent` projections down and invokes the existing `PlaylistStateOwner.mutate` and `selectOccurrenceForPlayback`. Confirmation of deletion invalidates only the exact displayed destination.
4. Smart rules are deliberately outside the v1 portable JSON backup; static export remains unchanged. Show an explicit EN/ZH note on Saved hub. No implicit conversion of saved Queue lists to smart definitions: SavedPlaylist rule references a static list, which may be one saved from Queue, and follows subsequent edits.

## Risks / Verification

- Missing referenced static list after deletion: retain named smart list, show recovery and keep rule editing possible.
- Scan/favorite/history publication races: compute membership from the same immutable Library content read during rendering; do not persist a cache or publish a stale derived snapshot.
- Renaming/updating with stale id: reject write, preserve visible state, never edit a different route/modal.
- Empty, duplicate and large source entries: preserve source order and duplicates; use one derived row per other rule. No extra artwork byte reads to evaluate rules.
- Backup v1: never include smart rules; do not silently reinterpret them as static entries.

See `docs/superpowers/specs/2026-09-29-smart-playlists-design.md` and `specs/smart-playlists/spec.md` for exact behavior and acceptance.
