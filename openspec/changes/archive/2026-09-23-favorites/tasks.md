## 1. Database and repository contract

- [x] 1.1 Add the `track_favorite` SQLDelight table, indexes/queries, schema migration, and generated-database registration; verify with database generation and migration tests from both current and legacy fixtures.
- [x] 1.2 Extend `LibraryRepository` with favorite-ID reads and absent-track-safe set/unset behavior; implement mapping and transaction boundaries in `SqlDelightLibraryRepository`; verify idempotence, restart persistence, absent-track rejection, and cascade cleanup in repository/database tests.

## 2. Shared authoritative state

- [x] 2.1 Add favorite IDs to Shared's authoritative library publication and startup/scan/destructive-mutation reload paths; preserve operation-token/revision ownership and verify stale publication cannot overwrite a newer favorite or library state.
- [x] 2.2 Thread narrow favorite projections and callbacks through Shared library and Now Playing composition without adding feature-to-feature implementation dependencies; verify toggling does not mutate playback queue, selection, repeat, shuffle, or progress.

## 3. Library presentation

- [x] 3.1 Add Favorites browse destination, standard ordering, empty state, and visible-track playback queue using existing library grouping and selection seams; verify membership and queue behavior at JVM/UI level.
- [x] 3.2 Add accessible favorite toggles to song rows and album/artist detail presentations, including checked/unchecked state and selection-mode exclusion; verify semantics and cross-surface propagation with Compose tests.

## 4. Now Playing and resources

- [x] 4.1 Add the Now Playing favorite action only for an authoritative available current track and route its mutation through Shared; verify unavailable/removed tracks expose no stale action and successful toggles update Library.
- [x] 4.2 Add localized English and Simplified Chinese labels, descriptions, empty-state copy, and checked-state wording; verify resource-ledger, localization, formatting, and accessibility coverage.

## 5. Integration verification

- [x] 5.1 Run focused database, library, Shared, Now Playing, and Settings-adjacent JVM/host tests plus required iOS test compilation and Android assembly; record observable behavior and any environment blockers.
  - Evidence: database migration/repository selectors passed; library JVM selectors passed; Shared compilation and concurrency/publication selectors passed; Now Playing semantics passed; iOS test-code compilation passed. Android assembly was attempted and blocked before compilation by missing NDK `30.0.15729638` (`android.toolchain.cmake`); this is an environment blocker, not a source failure.
- [x] 5.2 Run standalone `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict OpenSpec validation, and `git diff --check`; complete independent review of lifecycle, concurrency, migration, dependency direction, and UI semantics before archival.
  - Evidence: formatting, Spotless, Detekt, architecture, strict OpenSpec, and diff checks passed in the recorded verification lane; final independent review of commits `6fd6f0c2..bfe19fe3` returned Ready with no Critical, Important, or Minor findings.
