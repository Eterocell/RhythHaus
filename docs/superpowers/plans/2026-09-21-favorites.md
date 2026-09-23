# Favorites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Add durable per-track favorites and a coherent Favorites browse/action experience without mutating media, sources, or playback state.

**Architecture:** Store a foreign-keyed `track_favorite` relation in `:core:database`; expose a narrow favorite projection and set/unset operation through `LibraryRepository`; publish favorite IDs with Shared's authoritative library state. Library and Now Playing receive immutable state plus callbacks, while Shared owns mutation serialization and reconciliation. Existing library grouping and playback-selection seams remain the only queue/rendering path.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Kotlin test, Compose UI semantics, Koin, Gradle configuration cache.

**Spec:** `openspec/changes/favorites/proposal.md`, `openspec/changes/favorites/design.md`, `openspec/changes/favorites/specs/track-favorites/spec.md`, `openspec/changes/favorites/specs/local-library-scanning/spec.md`.

## Global Constraints

- Every SQLDelight schema change includes its migration in the same change set.
- `:core:database` owns schema, generated database types, drivers, migrations, and database migration tests.
- `:shared` remains the composition/facade root; feature implementations do not depend on `:shared` or other feature implementations.
- Favorite actions never write media tags, mutate source access, launch pickers, or alter playback queue/repeat/shuffle/progress.
- State publications use the existing App-owned operation/revision ownership; stale work MUST NOT overwrite a newer authoritative publication.
- Existing track identity is `(sourceId, sourceLocalKey)` and favorite state follows the persisted track ID through metadata rescans.
- UI controls must remove inactive semantics/actions rather than relying on visual alpha.

## Review Focus

- Stale favorite toggle completion after a scan or destructive mutation: only the current authoritative revision may publish it; test in Shared publication/reconciliation coverage.
- Absent or deleted track ID passed to the repository: no row may be created and no stale UI action may remain; test repository and Now Playing coverage.
- Existing database migration with foreign-key enforcement: legacy tracks and playlists survive while favorite rows start empty; test migration fixtures.
- Favorite persistence through metadata upsert and cascade deletion: update preserves it, source/remove-missing/clear removes it; test database/repository lifecycle coverage.
- Favorites queue versus global queue: playing from Favorites must use only visible favorite tracks without changing existing playback modes; test Shared/library selection coverage.

---

### Task 1: SQLDelight schema and migration

**Files:**
- Create: `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/TrackFavorite.sq`
- Create: `core/database/src/commonMain/sqldelight/migrations/2.sqm` (or the next schema migration expected by the current generated schema)
- Modify: `core/database/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`
- Modify: `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq` only if the generator requires an anchor declaration
- Test: `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt` and focused new database tests beside it

**Interfaces:**
- Produces SQLDelight queries for all favorite IDs, one-track membership, idempotent set/unset, and cleanup through `ON DELETE CASCADE`.
- Keeps generated schema version and database table allow-list aligned.

- [ ] **Step 1: Write failing migration/lifecycle tests** for a current-version database, a legacy version-2 fixture, metadata upsert, absent-track set, and source/track deletion cascade.
- [ ] **Step 2: Run the focused database tests** and record the expected missing-query/schema failure.
- [ ] **Step 3: Add the relation and migration** with `trackId` primary key, foreign key cascade, deterministic timestamp field if retained by the approved design, and exact SQLDelight query names.
- [ ] **Step 4: Regenerate/build the database and run focused tests**; verify old track/playlist rows migrate, favorites survive an upsert by the same track identity, absent IDs are rejected, and deletion leaves no favorite row.
- [ ] **Step 5: Commit** with `feat(database): persist track favorites`.

### Task 2: Repository API and implementation

**Files:**
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryTest.kt` or the repository test file matching current layout

**Interfaces:**
- Consumes generated favorite queries from Task 1.
- Produces `favoriteTrackIds(): Set<String>` and an absent-track-safe `setTrackFavorite(trackId: String, favorite: Boolean): Boolean` (adjust only if existing repository result conventions require a named result type).

- [ ] **Step 1: Write failing repository tests** for empty reads, set/unset idempotence, absent-track rejection, and persistence across a reopened database.
- [ ] **Step 2: Run the focused repository selector** and confirm the new API is absent/failing.
- [ ] **Step 3: Implement the narrow API** with track-existence validation and database mutation boundaries; do not expose SQLDelight types.
- [ ] **Step 4: Run repository/database tests** and verify exact returned state and no orphan rows.
- [ ] **Step 5: Commit** with `feat(library): expose favorite repository state`.

### Task 3: Shared authoritative favorite projection

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPublication.kt` or the existing publication-owner file identified by the current source layout
- Test: existing Shared App/library publication and lifecycle test files; add only behavior tests for stale publication and mutation isolation

**Interfaces:**
- Consumes `LibraryRepository.favoriteTrackIds()` and `setTrackFavorite`.
- Produces immutable favorite IDs, toggle callbacks, and filtered Favorites tracks to composed library/Now Playing surfaces.

- [ ] **Step 1: Write failing Shared tests** proving startup loads favorite IDs, a toggle republishes state, a stale completion cannot overwrite a newer revision, and a toggle does not change playback controller state.
- [ ] **Step 2: Run focused Shared tests** and capture failures at the missing projection/callback boundary.
- [ ] **Step 3: Add favorite IDs to the authoritative publication** and reload them in startup, scan, remove-missing, source removal, and clear paths using the existing publication owner/token pattern.
- [ ] **Step 4: Wire the narrow projection/callback into Library and Now Playing composition** while preserving dependency direction and existing playback selection semantics.
- [ ] **Step 5: Run Shared/library/playback selectors** and verify race, mutation isolation, and lifecycle behavior.
- [ ] **Step 6: Commit** with `feat(shared): publish authoritative favorite state`.

### Task 4: Shared favorite composition contract

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and exact existing Shared composition adapters
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt` and exact route/content contracts it composes
- Modify: `feature/nowplaying/src/commonMain/kotlin/**` exact public content contract consumed by Shared
- Test: existing Shared/library/Now Playing composition and playback-isolation tests

**Interfaces:**
- Consumes the authoritative immutable `LibraryContentState.favoriteTrackIds`, current revision, and Shared-owned `setTrackFavoriteAndPublish` seam from Task 3.
- Produces narrow immutable favorite-ID and callback parameters at Library and Now Playing feature API boundaries. The callback receives a track ID and desired favorite state; Shared derives the current expected revision when it invokes the authority seam so feature APIs do not expose mutable revision ownership.

- [ ] **Step 1: Write failing composition tests** proving Library and Now Playing receive the same immutable favorite projection and that invoking the callback only requests the Shared favorite mutation.
- [ ] **Step 2: Run the focused Shared/library/Now Playing selectors** and confirm the new composition parameters are absent.
- [ ] **Step 3: Add narrow feature API parameters and Shared adapters**; thread only favorite IDs plus a callback, never repositories, SQLDelight types, mutable App state, or feature-implementation dependencies.
- [ ] **Step 4: Run focused composition/playback selectors** and verify the callback does not change playback queue, selected occurrence, repeat, shuffle, or progress.
- [ ] **Step 5: Commit** with `feat(shared): compose favorite state callbacks`.

### Task 5: Favorites browse destination and row controls

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt` and/or the exact existing browse-state owner
- Modify: Shared library labels/resource adapters as required by current boundary
- Test: library UI/browser/row Compose JVM tests beside existing tests

**Interfaces:**
- Consumes authoritative `favoriteTrackIds`, `onToggleFavorite`, and `LibraryTrack` list from Task 3.
- Produces Favorites-only visible tracks, standard ordering, accessible checked state, and existing visible-track playback queue.

- [ ] **Step 1: Write failing Compose tests** for Favorites membership/empty state, song/album/artist row toggle semantics, selection-mode action absence, and playback queue filtering.
- [ ] **Step 2: Run the focused UI selectors** and confirm the new destination/actions fail.
- [ ] **Step 3: Implement Favorites destination and filtering** by stable track IDs; reuse existing grouping and selection helpers rather than duplicating queue logic.
- [ ] **Step 4: Add accessible checked/unchecked favorite controls** that are not active during selection-only gestures; preserve long-press selection and normal row playback.
- [ ] **Step 5: Run Compose UI tests** at compact and wide layouts and verify empty state and cross-surface state updates.
- [ ] **Step 6: Commit** with `feat(library): add favorites browsing and controls`.

### Task 6: Now Playing action and localized resources

**Files:**
- Modify: `feature/nowplaying/src/commonMain/kotlin/**` exact Now Playing contract/rendering files
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` adapters
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh/strings.xml`
- Test: Now Playing Compose/JVM tests and resource/localization tests matching current project layout

**Interfaces:**
- Consumes the Shared-provided current-track availability, favorite state, and callback.
- Produces no playback mutation; action is absent when the current track is not authoritative.

- [ ] **Step 1: Write failing Now Playing tests** for checked/unchecked action semantics, absent current-track suppression, and callback propagation without queue/state changes.
- [ ] **Step 2: Run focused Now Playing tests** and confirm failures.
- [ ] **Step 3: Implement the narrow action and EN/ZH copy** using existing resource/adaptor conventions.
- [ ] **Step 4: Run Now Playing/resource/accessibility tests** and verify no stale action survives track removal.
- [ ] **Step 5: Commit** with `feat(nowplaying): add favorite action`.

### Task 7: Integration verification and lifecycle review

**Files:**
- Modify: `openspec/changes/favorites/tasks.md` checkboxes and evidence notes
- Modify: `progress.md` and `roadmap.md` after implementation evidence exists
- Update/archive: canonical specs only after implementation and review pass

- [ ] **Step 1: Run focused database, library, Shared, Now Playing, Android-host, and iOS compilation checks**; record exact successful commands and blockers.
- [ ] **Step 2: Run standalone `spotlessApply --configuration-cache`, `spotlessCheck --configuration-cache`, `detekt --configuration-cache`, and `architectureCheck --configuration-cache`** as separate gates.
- [ ] **Step 3: Run `openspec validate favorites --strict` and `git diff --check`; inspect the complete diff for schema, lifecycle, dependency, concurrency, accessibility, and localization regressions.
- [ ] **Step 4: Obtain independent review, repair all Critical/Important findings, then sync the accepted delta into canonical specs and archive only after all automated acceptance gates pass.
- [ ] **Step 5: Record remaining physical-device/system-media evidence separately; do not claim Favorites implementation acceptance from compilation alone.
- [ ] **Step 6: Commit lifecycle documentation with `docs: record favorites acceptance`.
