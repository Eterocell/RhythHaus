# Sorting and Filtering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic, ephemeral sorting and filtering to flat Library Home presentations without changing persistence, playback, or grouped browsing semantics.

**Architecture:** A pure feature-owned `LibraryBrowseQuery` projection filters and sorts immutable Shared-provided track projections. `LibraryAppState` owns ephemeral query state; Shared adapts source IDs and timestamps; Library renders localized controls and uses the resulting visible sequence for selection and playback.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight read projections, Compose resources, Kotlin test, Gradle configuration cache.

**Spec:** `openspec/changes/sorting-filtering/specs/library-sorting-filtering/spec.md`

## Global Constraints

- No SQLDelight schema or migration changes; query state is ephemeral.
- Common domain/state/UI stays in commonMain; no platform-specific code is required.
- Existing Favorites, Recently Played, Recently Added, Albums, and Artists semantics remain intact.
- Query changes do not mutate files, sources, favorites, history, playback, repeat, shuffle, or progress.
- Source filters use authoritative source IDs; unknown IDs produce an empty result.
- Every visible flat sequence used for rows is also used for selection reconciliation and playback queue construction.
- English and Simplified Chinese labels and accessibility state descriptions are required.

## Review Focus

- Equal and missing sort values: prove deterministic title/artist/album/ID tie-breaking and no crashes.
- Mode/query precedence: prove Favorites and Recent membership cannot be broadened by a query.
- Grouped browsing: prove Albums/Artists remain grouped and their detail track order is unchanged.
- Selection after filtering: prove excluded IDs cannot remain selected or enter a queue.
- Passive filtered-empty state: prove filtering does not expose import or clear actions.

---

### Task 1: Pure query model and projection

**Files:**
- Create: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowseQuery.kt`
- Test: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowseQueryTest.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt`

**Interfaces:**
- Produces `LibraryBrowseQuery`, `LibrarySort`, `LibrarySortDirection`, and `visibleTracksForBrowseQuery(...)`.
- Consumes existing `BrowseMode`, `Track`, `TrackPlayHistory`, favorites, and recent projections.

- [ ] Write failing tests for every sort key, direction, filter, missing values, tie-breakers, and mode membership.
- [ ] Implement the query enums, immutable value, filter-first projection, deterministic comparator, and explicit grouped-mode pass-through.
- [ ] Run `./gradlew :feature:library:impl:commonTest --tests '*LibraryBrowseQueryTest*' --configuration-cache` or the repository-equivalent focused test task.
- [ ] Commit `feat: add library browse query projection`.

### Task 2: Shared query state and projections

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowseQueryStateTest.kt`

**Interfaces:**
- Consumes Task 1 query model.
- Produces active query callbacks and immutable source/created/modified/play-count projections to Library.

- [ ] Write failing state tests for default query, independent sort/filter updates, and reset on new shell.
- [ ] Add `browseQuery` state and narrow setters to `LibraryAppState`; pass source IDs and modified times from `LibraryContentState` through shell calls.
- [ ] Apply query state only to Home flat projections; preserve grouped and detail behavior.
- [ ] Run Shared focused state and Library compilation tests.
- [ ] Commit `feat: wire library browse query state`.

### Task 3: Accessible localized controls

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt`
- Modify: `feature/library/impl/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml`
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowseControlsJvmTest.kt`

**Interfaces:**
- Consumes active query and source labels from Task 2.
- Produces localized sort/filter controls and passive filtered-empty state.

- [ ] Write failing semantics/resource tests for sort direction, favorite/artwork toggles, source selection, clear actions, and EN/ZH labels.
- [ ] Implement compact-safe controls below BrowseModePicker with selected/checked semantics and source omission when unavailable.
- [ ] Ensure filtered empty results do not render import or clear actions.
- [ ] Run focused Library UI JVM tests and resource generation.
- [ ] Commit `feat: add accessible library sort filters`.

### Task 4: Selection and playback integration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionState.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowseSelectionTest.kt`

**Interfaces:**
- Consumes visible query projection and current `TrackSelectionAction` reducer.
- Produces stale-selection removal and exact visible queue behavior after query changes.

- [ ] Write failing regressions for filtered-out selection, reordered queue playback, and unchanged active playback state.
- [ ] Reconcile visible IDs on query changes and ensure callbacks pass the exact projected order.
- [ ] Run Shared selection and playback integration selectors.
- [ ] Commit `fix: reconcile library selection after query changes`.

### Task 5: Verification and lifecycle closeout

**Files:**
- Modify: `openspec/changes/sorting-filtering/tasks.md`
- Modify: `openspec/specs/local-library-scanning/spec.md` only if review identifies a lifecycle requirement gap.
- Modify: `roadmap.md`
- Modify: `progress.md`
- Create: `openspec/specs/library-sorting-filtering/spec.md`

- [ ] Run focused Library/Shared JVM tests, iOS test-code compilation, Spotless, Detekt, architecture, strict OpenSpec validation, and `git diff --check`.
- [ ] Run `./init.sh`; record the exact result and preserve existing NDK/timeout blockers if reproduced.
- [ ] Perform final whole-branch review and address Critical/Important findings.
- [ ] Synchronize the capability into `openspec/specs/library-sorting-filtering/spec.md`, mark all tasks complete, archive the change, and commit lifecycle evidence.
