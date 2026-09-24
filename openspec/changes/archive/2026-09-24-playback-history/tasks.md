# Tasks

## 1. Durable history persistence

- [x] 1.1 Add the `track_play_history` SQLDelight relation, read/upsert queries, schema migration, generated database fixtures, and migration coverage for current, legacy, rescan-preservation, and cascade-deletion cases.
- [x] 1.2 Extend `LibraryRepository` and `SqlDelightLibraryRepository` with absent-track-safe history reads and atomic record-play behavior; cover first play, later increment, restart persistence, and absent IDs.

## 2. Actual-playing event admission

- [x] 2.1 Add a controller-owned, buffered actual-playing event projection keyed by generation and occurrence, and emit it exactly once when the current generation first enters `Playing`.
- [x] 2.2 Cover duplicate `Playing` callbacks, failed/loading-only selections, stale generations, retries/restarts, and successive admitted generations in common playback tests without changing platform engine APIs.

## 3. Shared authoritative history state

- [x] 3.1 Extend library content/publication state and startup/scan/destructive reload paths with immutable history and created-time projections, preserving listener-owned history through rescans and omitting deleted records.
- [x] 3.2 Install exactly one Shared collector for actual-play events; serialize repository mutation and authoritative publication without cancelling scans, and cover stale/concurrent publication and removed-track cases.
- [x] 3.3 Thread narrow immutable history and created-time projections through Shared library composition without feature-to-feature implementation dependencies.

## 4. Recent Library presentation

- [x] 4.1 Add Recently Played and Recently Added browse modes, pure deterministic ordering helpers, localized empty presentation, and flat-list queue behavior using existing selection and Back seams.
- [x] 4.2 Add English and Simplified Chinese labels plus Compose/JVM semantics coverage for mode selection, empty history, ordering, compact/wide readability, and selection-mode control exclusion.

## 5. Integration verification and lifecycle closeout

- [x] 5.1 Run focused database, repository, playback, Shared, and Library JVM/host tests plus iOS test-code compilation and Android assembly; record observable behavior and exact environment blockers.
- [x] 5.2 Run standalone `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict OpenSpec validation, and `git diff --check`; independently review migration, event deduplication, concurrency, dependency direction, and accessibility before synchronization/archival.