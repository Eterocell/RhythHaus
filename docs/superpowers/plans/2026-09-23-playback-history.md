# Playback History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and present per-track play counts, recently played tracks, and recently added tracks while counting only real, generation-deduplicated playback starts.

**Architecture:** `:core:database` stores listener-owned history in a foreign-keyed relation; library API/implementation owns atomic mutation and projections. `:core:playback` emits one actual-playing event per admitted generation/occurrence. `:shared` is the sole collector and publication owner, serializing history writes with authoritative library state. `:feature:library:impl` receives immutable projections and derives recent flat lists using existing rows, queue selection, and Back policy.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Kotlin coroutines/Flow, Koin, JUnit/Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-09-23-playback-history-design.md`; `openspec/changes/playback-history/{proposal.md,design.md,specs/**/spec.md,tasks.md}`.

## Global Constraints

- Keep `:shared` as composition root; core/features must not depend on it or app modules.
- Persist history in a relation keyed by `library_track.id`; scanner upserts must not mutate listener-owned state.
- The history event is emitted only after the current generation/occurrence enters `PlaybackStatus.Playing`, once per pair.
- History writes never cancel, preempt, or acquire scan ownership; their publication must serialize with authoritative library lifecycle state.
- Recently Added uses existing `LibraryTrack.createdAtEpochMillis`; do not duplicate it in history storage or widen `Track`.
- Recent modes are flat lists and must preserve existing selection/Back semantics and visible-order queue behavior.
- Provide English and Simplified Chinese labels and explicit accessibility semantics.
- Every SQLDelight schema change includes migration fixtures and migration tests.

## Review Focus

- Duplicate platform `Playing` status for one generation must produce exactly one persisted increment; cover in Task 2.
- A replaced, stale, loading-only, paused, or error generation must never produce history; cover in Task 2.
- Scan/removal/clear publications must not overwrite a concurrently accepted history write or expose deleted IDs; cover in Task 3.
- A recent-mode tap must queue only the displayed, ordered recent list, including deterministic timestamp ties; cover in Task 4.
- Compact width, selection mode, and empty recent history must not expose inactive actions or inaccessible controls; cover in Task 4.

---

### Task 1: Database history relation and repository contract

**Files:**
- Create: `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/TrackPlayHistory.sq`
- Create: `core/database/src/commonMain/sqldelight/migrations/3.sqm`
- Create: `core/database/src/commonMain/sqldelight/databases/4.db`
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt`
- Create: `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/TrackPlayHistoryDatabaseTest.kt`
- Modify: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`
- Modify: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`

**Interfaces:**
- Produces `data class TrackPlayHistory(val trackId: String, val playCount: Long, val lastPlayedAtEpochMillis: Long)` in library API.
- Produces `fun playHistory(): Map<String, TrackPlayHistory>` and `fun recordTrackPlayed(trackId: String, playedAtEpochMillis: Long): Boolean` on `LibraryRepository`.
- Consumes existing `library_track` identity and foreign-key lifecycle.

- [ ] **Step 1: Write failing migration and repository tests**

```kotlin
@Test
fun migrationFromVersionThreePreservesTracksAndStartsWithNoHistory() { /* open 3.db; migrate; assert tracks survive and history empty */ }

@Test
fun recordTrackPlayedIncrementsAndUpdatesTimestampAtomically() {
    assertTrue(repository.recordTrackPlayed("track", 100L))
    assertTrue(repository.recordTrackPlayed("track", 200L))
    assertEquals(TrackPlayHistory("track", 2L, 200L), repository.playHistory()["track"])
}

@Test
fun absentTrackDoesNotCreateHistory() {
    assertFalse(repository.recordTrackPlayed("missing", 100L))
    assertTrue(repository.playHistory().isEmpty())
}
```

- [ ] **Step 2: Run the failing selectors**

Run: `./gradlew :core:database:jvmTest --tests 'com.eterocell.rhythhaus.library.TrackPlayHistoryDatabaseTest' :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache`

Expected: compilation/test failure because the history table and Repository contract do not exist.

- [ ] **Step 3: Add the minimum schema and contract**

```sql
CREATE TABLE track_play_history (
    trackId TEXT NOT NULL PRIMARY KEY REFERENCES library_track(id) ON DELETE CASCADE,
    playCount INTEGER NOT NULL,
    lastPlayedAtEpochMillis INTEGER NOT NULL
);

recordTrackPlayed:
INSERT INTO track_play_history(trackId, playCount, lastPlayedAtEpochMillis)
SELECT :trackId, 1, :playedAtEpochMillis
WHERE EXISTS(SELECT 1 FROM library_track WHERE id = :trackId)
ON CONFLICT(trackId) DO UPDATE SET
    playCount = playCount + 1,
    lastPlayedAtEpochMillis = excluded.lastPlayedAtEpochMillis;
```

Implement SQLDelight mapping and in-memory test repository semantics; add migration `3.sqm`, generated `4.db`, and test fixtures using the same sequential-generation workaround established for Favorites.

- [ ] **Step 4: Run Green coverage**

Run: `./gradlew :core:database:jvmTest --tests 'com.eterocell.rhythhaus.library.ExistingDatabaseMigrationTest' --tests 'com.eterocell.rhythhaus.library.TrackPlayHistoryDatabaseTest' :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache`

Expected: BUILD SUCCESSFUL; proves v3 migration, absent-ID rejection, first/incremented writes, restart persistence, rescan preservation, and source/remove-missing/clear cascade cleanup.

- [ ] **Step 5: Commit the isolated persistence boundary**

```bash
git add core/database feature/library/api feature/library/impl
git commit -m "feat(database): persist track playback history"
```

### Task 2: Generation-deduplicated actual-playing events

**Files:**
- Modify: `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- Modify: `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`

**Interfaces:**
- Produces `data class PlaybackStarted(val generation: Long, val occurrenceId: String, val trackId: String)`.
- Produces `val PlaybackController.playbackStarted: Flow<PlaybackStarted>`.
- Consumes existing `PlaybackStatus.Playing`, `engineGeneration`, and `currentOccurrence` ownership gate.

- [ ] **Step 1: Write failing controller event tests**

```kotlin
@Test
fun playingStatusEmitsOneEventForCurrentGenerationAndOccurrence() = runTest {
    controller.setOccurrenceQueue(queue, "one")
    engine.reportStatus(PlaybackStatus.Playing)
    engine.reportStatus(PlaybackStatus.Playing)
    assertEquals(listOf(PlaybackStarted(generation, "one", "track-1")), events.receiveAll())
}

@Test
fun loadingFailureAndStaleGenerationEmitNoPlayStartedEvent() = runTest { /* load/fail/replace; assert channel empty */ }

@Test
fun retryOrNewSelectionPlayingCountsNewGeneration() = runTest { /* assert two events with distinct generations */ }
```

- [ ] **Step 2: Run Red playback coverage**

Run: `./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`

Expected: failure because no actual-playing event flow exists.

- [ ] **Step 3: Implement a controller-owned buffered event channel**

```kotlin
public data class PlaybackStarted(val generation: Long, val occurrenceId: String, val trackId: String)

private var lastStartedKey: PlaybackStartedKey? = null

private fun emitPlaybackStartedIfNew(state: PlaybackState) {
    val occurrence = state.currentOccurrence ?: return
    val key = PlaybackStartedKey(state.engineGeneration, occurrence.id)
    if (state.status == PlaybackStatus.Playing && lastStartedKey != key) {
        lastStartedKey = key
        playbackStartedChannel.trySend(PlaybackStarted(key.generation, key.occurrenceId, occurrence.track.id))
    }
}
```

Call it only after the current-generation `Playing` state has won the existing ownership/state transition. Preserve channel ordering and do not modify `PlaybackEngineListener` or platform engine APIs.

- [ ] **Step 4: Run Green playback coverage**

Run: `./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`

Expected: BUILD SUCCESSFUL; repeated `Playing` deduplicates; stale/loading/error paths are silent; distinct admitted generations emit once each.

- [ ] **Step 5: Commit the playback event seam**

```bash
git add core/playback
git commit -m "feat(playback): emit actual playing events"
```

### Task 3: Shared history admission and authoritative publication

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackHistoryAdmissionTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
- Modify: all Shared/library test fake `LibraryRepository` implementations found by compiler.

**Interfaces:**
- Consumes `PlaybackController.playbackStarted`, `LibraryRepository.recordTrackPlayed`, and `LibraryRepository.playHistory`.
- Produces `LibraryContentState.playHistory: Map<String, TrackPlayHistory>` and `LibraryContentState.createdAtByTrackId: Map<String, Long>`.
- Produces a single App-owned event collector and a history mutation helper that republishes through `AuthoritativeLibraryPublicationOwner`.

- [ ] **Step 1: Write failing Shared lifecycle/race tests**

```kotlin
@Test
fun admittedPlaybackRecordsIndexedTrackAndRepublishesRecentHistory() = runTest { /* emit event; assert count/map/publication */ }

@Test
fun playbackHistoryDoesNotCancelActiveScan() = runTest { /* block scan; record history; assert scan remains owner */ }

@Test
fun scanOrDestructivePublicationCannotOverwriteAcceptedHistory() = runTest { /* coordinate delayed loaders; assert final map */ }

@Test
fun removedTrackEventIsIgnoredWithoutStalePublication() = runTest { /* remove then emit; assert absent */ }
```

- [ ] **Step 2: Run Red Shared selectors**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`

Expected: compile/test failure because Shared does not own history projections or a controller event collector.

- [ ] **Step 3: Extend publication and install one collector**

```kotlin
internal data class LibraryContentState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
    val favoriteTrackIds: Set<String> = emptySet(),
    val playHistory: Map<String, TrackPlayHistory> = emptyMap(),
    val createdAtByTrackId: Map<String, Long> = emptyMap(),
)

LaunchedEffect(controller) {
    controller.playbackStarted.collect { event ->
        recordPlaybackHistoryAndPublish(event)
    }
}
```

Load `playHistory` and `createdAtByTrackId` with each content snapshot. Run the repository write and resulting map reload inside `libraryPublicationOwner.mutateAndPublish`; do not acquire scan admission or cancel a scan. Revalidate that `event.trackId` remains in the repository during the atomic repository operation.

- [ ] **Step 4: Run Green Shared coverage**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`

Expected: BUILD SUCCESSFUL; proves one collector, indexed-only writes, publication race safety, no scan preemption, deletion cleanup, and dependency assembly.

- [ ] **Step 5: Commit Shared ownership**

```bash
git add shared
git commit -m "feat(shared): publish playback history"
```

### Task 4: Recent browse modes, resources, and semantics

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `feature/library/impl/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt`
- Modify: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt`

**Interfaces:**
- Consumes immutable `Map<String, TrackPlayHistory>` and `Map<String, Long>` supplied by Shared.
- Produces `BrowseMode.RecentlyPlayed`, `BrowseMode.RecentlyAdded`, and `visibleTracksForBrowseMode(tracks, browseMode, favoriteTrackIds, playHistory, createdAtByTrackId)`.
- Uses existing `onPlayTrack(visibleTracks, selectedTrack)` and `trackSelectionPageKeyFor` flat-page policy.

- [ ] **Step 1: Write failing pure/UI behavior tests**

```kotlin
@Test
fun recentlyPlayedUsesLastPlayedDescendingThenTitleArtist() { /* timestamps and exact order */ }

@Test
fun recentlyAddedUsesCreatedAtDescendingThenTitleArtist() { /* timestamps and exact order */ }

@Test
fun recentModeTapQueuesOnlyDisplayedOrderedTracks() = runComposeUiTest { /* tap; capture queue */ }

@Test
fun emptyRecentlyPlayedShowsLocalizedMessageWithoutImportAction() = runComposeUiTest { /* semantics assertions */ }

@Test
fun compactRecentModeControlsExposeAllModesAndSelectedSemantics() = runComposeUiTest { /* 400dp width */ }
```

- [ ] **Step 2: Run Red Library selectors**

Run: `./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' --configuration-cache`

Expected: compile/test failure because recent modes, resources, and shared projections do not exist.

- [ ] **Step 3: Implement deterministic flat recent projections**

```kotlin
RecentlyPlayed -> tracks.filter { it.id in playHistory }
    .sortedWith(compareByDescending<Track> { playHistory.getValue(it.id).lastPlayedAtEpochMillis }
        .thenBy { it.title.lowercase() }.thenBy { it.artist.lowercase() })
RecentlyAdded -> tracks.sortedWith(compareByDescending<Track> { createdAtByTrackId.getValue(it.id) }
    .thenBy { it.title.lowercase() }.thenBy { it.artist.lowercase() })
```

Require a created-time projection for every displayed track; never derive ordering from a missing map value. Extend flat-mode selection/Back predicates to include both recent modes. Add local resource strings for both languages, selected button semantics, and a normal non-import empty history message.

- [ ] **Step 4: Run Green Library and accessibility coverage**

Run: `./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' --configuration-cache`

Expected: BUILD SUCCESSFUL; proves ordering/ties, empty state, visible queue, selected semantics, compact mode reachability, selection behavior, and Back policy.

- [ ] **Step 5: Commit recent presentation**

```bash
git add feature/library/impl shared
git commit -m "feat(library): add recent playback browsing"
```

### Task 5: Cross-platform verification, review, and closeout

**Files:**
- Modify: `openspec/changes/playback-history/tasks.md`
- Modify: `openspec/specs/local-library-scanning/spec.md`
- Create: `openspec/specs/playback-history/spec.md`
- Modify: `roadmap.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes Tasks 1–4 complete artifacts and their focused tests.
- Produces synchronized canonical specs, verification evidence, and an archive-ready change.

- [ ] **Step 1: Run changed-path cross-platform verification**

Run:

```bash
./gradlew :core:database:jvmTest :core:playback:jvmTest :feature:library:impl:jvmTest :shared:jvmTest :shared:compileTestKotlinIosSimulatorArm64 --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
```

Expected: all available tasks BUILD SUCCESSFUL. If Android cannot begin because the known NDK/toolchain is absent, preserve the exact command/error in `progress.md`; do not classify it as a source success or failure.

- [ ] **Step 2: Run standalone quality gates**

Run:

```bash
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
./gradlew architectureCheck --configuration-cache
openspec validate playback-history --strict
openspec validate --specs
git diff --check
```

Expected: each available command succeeds; inspect and repair formatter output before the final diff check.

- [ ] **Step 3: Obtain independent review**

Review migration compatibility, controller event deduplication, collector lifecycle, publication races, generated dependency edges, recent queue ordering, localized resources, accessibility semantics, and tests that could falsely pass. Repair any confirmed finding before archival.

- [ ] **Step 4: Synchronize and archive after all tasks are complete**

```bash
openspec sync playback-history --yes
openspec archive playback-history --yes
```

If archive rejects an already-synchronized delta without changing files, verify canonical specs and archive the completed change directory using the repository's established documented fallback; never duplicate requirements.

- [ ] **Step 5: Record lifecycle evidence and commit**

```bash
git add openspec roadmap.md progress.md
git commit -m "docs: complete playback history"
```

Record passed commands, exact platform/environment limits, final review verdict, canonical spec path, archive path, and the next safe roadmap action.