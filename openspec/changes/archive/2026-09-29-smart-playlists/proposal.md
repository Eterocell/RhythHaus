# Proposal

## Why
The Library can browse Favorites and recency but cannot save a reusable, self-updating collection. Static playlists and Save Queue store snapshots; they do not reflect later library, favorite, or listening changes.

## What Changes
- Create, rename, change the rule of, browse, play, and delete named smart playlists from the Saved surface.
- Support Favorites, newest 10/25/50 played or added tracks, exact Artist, exact Artist+Album, and a referenced static saved playlist (including Save Queue results).
- Persist rule definitions separately from static entries, recompute membership from the current authoritative library/playlist projection, and retain a missing-reference rule with an editable empty state.
- Keep version-1 JSON playlist backup explicitly scoped to static playlists; communicate that smart rules are not exported.

## Capabilities

### New Capabilities
- `smart-playlists`: durable named rules with dynamically evaluated membership and read-only derived entries.

### Modified Capabilities
None. The existing `saved-playlists` static-entry and `playlist-backup` v1 contracts are unchanged.

## Impact
SQLDelight schema and v4→v5 migration in `:core:database`; typed contract, repository, state, pure projection, UI/resources in `:feature:playlists`; route/Back, authoritative library projection, playback selection in `:shared`. No new dependencies or platform audio integrations. Manual device/UI acceptance remains user-owned.
