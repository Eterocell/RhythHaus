## Why

RhythHaus can scan and play local music but cannot show listeners what they have played recently or which tracks receive repeated listening. Playback history is the next Phase 2 daily-use capability and provides the durable listener-owned relationship required by later sorting and smart playlists without broadening into recommendations or synchronization.

## What Changes

- Add durable per-track play count and last-played timestamp persistence, including a SQLDelight schema migration.
- Record one history event only when a library track enters actual `Playing` state for a distinct playback generation.
- Publish play-history state as part of Shared's authoritative library projection and reconcile it with scan and destructive library lifecycle changes.
- Add Recently Played and Recently Added library browse modes while retaining existing Albums, Artists, Songs, and Favorites behavior.
- Keep Recently Added derived from existing `LibraryTrack.createdAtEpochMillis`; do not duplicate that metadata in a new relation.
- Add English and Simplified Chinese labels, empty states, semantics, and focused migration, controller, Shared, and Compose coverage.

## Capabilities

### New Capabilities

- `playback-history`: durable play-history persistence, counted playback-generation behavior, authoritative publication, and recent-library presentations.

### Modified Capabilities

- `local-library-scanning`: library track deletion and clear/source-removal lifecycle requirements must remove associated history; existing track rescans retain history.

## Impact

- `:core:database`: new SQLDelight relation, queries, versioned migration, generated schemas, and migration coverage.
- `:feature:library:api` and `:feature:library:impl`: narrow history contract/implementation plus recent browse modes and localized Compose UI.
- `:shared`: observes authoritative playback state, records admitted play events, and publishes immutable history projections through existing library composition.
- `:core:playback`: exposes a listener-safe, generation-aware actual-playing event seam without changing platform engine ownership.
- Shared/core/library tests and Android/iOS/JVM compilation coverage.