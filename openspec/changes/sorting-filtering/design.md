# Design

## Context

Favorites and playback history already publish immutable listener-owned projections through `LibraryContentState`. `LibraryHomeContent` derives visible flat tracks and passes that exact sequence to playback and selection callbacks. Existing Albums and Artists group tracks and preserve disc/track/title ordering. Sorting/filtering should deepen this seam without making the database or playback layers aware of presentation preferences.

## Goals

- Let users choose a deterministic sort key and ascending/descending direction.
- Let users narrow Home content to favorites, tracks with artwork, or one source.
- Keep filters and sorting coherent across rows, selection, and playback queues.
- Preserve current browse-mode, Back, compact/wide, localization, and accessibility behavior.
- Avoid persistence and schema migration for ephemeral presentation state.

## Non-goals

- Metadata editing, tag writes, smart playlists, saved sort presets, cloud sync, search redesign, or source-management changes.
- Sorting grouped album/artist cards themselves; those remain alphabetic and their track detail order remains disc/track/title.
- Filtering Now Playing, playlists, or Settings.

## Decisions

### Query model

Add a public feature-owned `LibraryBrowseQuery` with `sort: LibrarySort`, `direction: LibrarySortDirection`, `favoriteOnly`, `artworkOnly`, and nullable `sourceId`. `LibrarySort` is Title, Artist, Album, Added, Modified, PlayCount, or Favorite. Query state lives in `LibraryAppState` and resets with the shell state; it is not written to DataStore or SQLDelight.

### Projection boundary

A pure helper receives `List<Track>` plus immutable maps for source IDs, created/modified timestamps, favorite IDs, and play history. It filters first, then sorts with a deterministic case-insensitive primary key and title/artist/album/id tie-breakers. Missing optional values sort last in ascending order and first in descending order only through direction reversal of the complete comparator; no entries are silently fabricated. Favorite sort places favorites first in ascending mode and last in descending mode.

The helper applies query sorting only to flat Home modes (`Songs`, `Favorites`, `RecentlyPlayed`, `RecentlyAdded`). Existing mode-specific membership/order remains authoritative: Favorites and Recently Played still filter/order by their semantics, then the explicit query sort is applied only when the user changes away from the default mode sort. Albums and Artists continue their existing grouping and detail ordering.

### UI

Add a compact, scrollable controls row below the browse-mode picker. A sort button cycles/open-selects sort key and direction; filter controls expose favorite, artwork, and source choices. Controls have selected/checked semantics and localized labels. The source selector is omitted when there are no sources. Clearing a filter is explicit and returns to the full list. Empty result state remains passive for filtered results and does not show import actions.

### State and playback

Shared passes source IDs and metadata projections to Library. Home computes one `visibleTracks` list from browse mode and query, reports that list to selection reconciliation, and supplies it to `onPlayTrack`. Changing query clears selection on the same flat page when the visible set/order changes, preventing stale selected IDs or queue order.

## Error and edge behavior

- Unknown source IDs in a query produce no matches rather than falling back to all sources.
- Missing metadata values use stable empty-string/zero ordering and never crash.
- A query with no visible tracks displays a localized filtered-empty message and no fake import affordance.
- Query changes do not mutate files, sources, favorites, history, playback state, repeat, shuffle, or progress.

## Verification

Cover pure projection boundaries, tie-breaking and missing values, every filter, query state transitions, visible selection reconciliation, compact/wide accessibility, English/Chinese labels, and unchanged grouped detail ordering. Run focused Library/Shared tests, iOS test compilation, formatting, Detekt, architecture, strict OpenSpec validation, and `git diff --check`.
