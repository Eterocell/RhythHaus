# Playlist Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared Playlist hub with durable duplicate-preserving saved playlists and an editable, occurrence-aware live playback queue.

**Architecture:** Saved playlists live in new SQLDelight tables behind a focused `PlaylistRepository`, sharing the existing library database and transaction domain. Playback keeps its controller and DataStore ownership, but represents each queue slot as a `QueueOccurrence` so queue position can be independent from library-track media identity. Shared Compose routes and screens observe repository/controller state and send commands only through those owners.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight 2.3.2, AndroidX DataStore Preferences, Koin, Kotlin coroutines, Kotlin test, JVM JDBC SQLite, Android SQLite, Native SQLite.

## Global Constraints

- Implement only the approved `playlist-screen` change. Do not add smart, generated, collaborative, cloud, remote, imported, exported, folder, or streaming playlists.
- Support Android, iOS, and desktop JVM/macOS. Do not add Windows or Linux product or packaging work, dependencies, or platform-specific playlist behavior.
- Keep saved playlist data in SQLDelight and keep the live queue and session in `PlaybackController` and the existing DataStore store. Do not persist live queues in SQLDelight.
- Lock `PlaylistEntry.id` as the saved-playlist `QueueOccurrence.id`. A `QueueOccurrence` carries an occurrence ID and `PlayableTrack`; the engine continues to receive the library-track ID as media identity.
- Persist ordered occurrence-ID/library-track-ID pairs in one ordered serialized DataStore preference value. Never use a set to represent queue membership. Read valid legacy track-ID-only snapshots compatibly and normalize them deterministically.
- Playlist names are trimmed, non-empty, and not unique. Playlist entries allow duplicate library-track IDs and have independent IDs. Removing the final entry retains the playlist.
- Put the migration at `shared/src/commonMain/sqldelight/migrations/1.sqm`; it contains SQL only, with no explicit `BEGIN` or `END`. Configure `schemaOutputDirectory` in `shared/build.gradle.kts` and check in the baseline database at `shared/src/commonMain/sqldelight/databases/1.db`.
- Open JVM databases with `JdbcSqliteDriver(url = ..., properties = Properties().apply { put("foreign_keys", "true") }, schema = RhythHausDatabase.Schema)`. Android uses an `AndroidSqliteDriver.Callback` whose `onOpen` calls `db.setForeignKeyConstraintsEnabled(true)`; iOS uses `NativeSqliteDriver` with `onConfiguration` returning `config.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true))`. Prove foreign-key enforcement on every platform driver path.
- Queue commands mutate only upcoming occurrences. Current occurrence removal and movement are rejected, current playback is not restarted or replaced, and accepted edits preserve position, status, repeat, shuffle, generation, and stale-callback safety.
- Use occurrence IDs as Compose keys. Every interactive playlist or queue control has a localized semantic label containing the playlist or track name; provide accurate localized content/state descriptions and move-up/move-down alternatives to drag, without assigning a false role when Compose lacks a suitable list-row role.
- Add English and Chinese resource strings. Preserve shared Library chrome, adaptive layout, safe content, track-row/artwork conventions, and the current route-aware Now Playing bar policy.
- Every production behavior starts with a failing focused test. Each task receives two-stage review. Use `GIT_MASTER=1` in git command examples; do not commit during plan creation.
- Completion requires strict OpenSpec validation, the platform matrix, manual/visual QA evidence, SDD reports, and updates to `progress.md` and `roadmap.md`. If iOS still fails only at `AppScanCancellationTest.kt:64:28` and `:340:27` on unresolved JVM-only `Thread`, report that exact blocker and do not claim an iOS test pass.
- `LibraryDatabaseIosTest` execution is a playlist completion gate: iOS main compilation does not prove foreign-key enforcement. If `:shared:iosSimulatorArm64Test` cannot execute that test because the existing common-test `Thread` compilation failure blocks it, record `[blocked] iOS FK proof` with the exact command and output, then stop before the final completion/evidence commit unless the user explicitly accepts the unverified iOS risk.

---

## File Structure

- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq`: playlist and playlist-entry schema plus ordered CRUD queries.
- `shared/src/commonMain/sqldelight/migrations/1.sqm`: additive migration from the checked-in version-1 baseline.
- `shared/src/commonMain/sqldelight/databases/1.db`: SQLDelight baseline schema before playlist tables.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`: domain records, focused repository contract, and in-memory test implementation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`: transaction-backed SQLDelight implementation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: injects `PlaylistRepository`, owns playlist snapshot refresh, and threads playlist dependencies into the Library shell/routes.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`: `QueueOccurrence`, occurrence-aware playback state, selection, shuffle/skip, reconciliation, and queue commands.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt` and `PlaybackSessionStore.kt`: pair codec, snapshots, DataStore compatibility reads, and ordered persistence.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`: shared hub, saved detail, add workflows, and queue presentation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`, `LibraryAppState.kt`, `LibraryAppShell.kt`, `LibraryRoutes.kt`, `LibraryHomeContent.kt`, `LibraryRows.kt`, and `LibraryDetailContent.kt`: route, owner wiring, entry points, and track overflow actions.
- Existing tests under `shared/src/commonTest`, `shared/src/jvmTest`, and `shared/src/androidHostTest`: focused contract, controller, navigation, migration, and platform-driver coverage.

### Task 1: SQLDelight Schema, Migration, Playlist Repository, and DI

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq`
- Create: `shared/src/commonMain/sqldelight/migrations/1.sqm`
- Create: `shared/src/commonMain/sqldelight/databases/1.db`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`
- Create: `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt`
- Create: `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseIosTest.kt`
- Modify: `shared/build.gradle.kts:118-125`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt:34-70`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt:5-98`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt:250-271`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt:7-20`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt:11-23`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt:6-17`

**Interfaces:**
- Consumes: `LibraryDatabase.database: RhythHausDatabase`, `LibraryRepository.removeMissingTracks(sourceId, latestScanId)`, `removeSource(sourceId)`, and `clearAll()`.
- Produces: `data class Playlist(id: String, name: String, createdAtEpochMillis: Long, updatedAtEpochMillis: Long)`, `data class PlaylistEntry(id: String, playlistId: String, trackId: String, position: Int, createdAtEpochMillis: Long)`, and `PlaylistRepository` methods `playlists()`, `playlist(id)`, `entries(playlistId)`, `create(name)`, `rename(id, name)`, `delete(id)`, `append(playlistId, trackIds)`, `removeEntry(entryId)`, and `reorder(playlistId, entryIds)`.

- [ ] **Step 1: Write separate failing common-contract and JVM SQL integration tests**

```kotlin
@Test
fun duplicateEntriesRemainIndependentInTheInMemoryContract() {
    val playlist = repository.create(" Road trip ")
    repository.append(playlist.id, listOf("track-a", "track-a"))
    assertEquals(listOf("track-a", "track-a"), repository.entries(playlist.id).map(PlaylistEntry::trackId))

    assertEquals("Road trip", repository.playlist(playlist.id)?.name)
}
```

The common contract test uses only `InMemoryPlaylistRepository` for naming, duplicate-entry, ordering, rollback-result, and empty-playlist behavior. It does not claim SQL foreign-key behavior. Add `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`, which creates one migration-capable `LibraryDatabase(databaseFile)` and constructs both `SqlDelightLibraryRepository` and `SqlDelightPlaylistRepository` from that same database before testing migration and cascades.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --configuration-cache`

Expected: compilation fails because playlist contracts, SQL repository, migration fixture, and platform foreign-key test seams do not exist.

- [ ] **Step 3: Implement schema, migration, drivers, and repository**

```sql
CREATE TABLE playlist_entry (
  id TEXT NOT NULL PRIMARY KEY,
  playlist_id TEXT NOT NULL REFERENCES playlist(id) ON DELETE CASCADE,
  track_id TEXT NOT NULL REFERENCES library_track(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  created_at_epoch_millis INTEGER NOT NULL,
  UNIQUE (playlist_id, position)
);
```

Configure `schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))`, put the existing pre-playlist schema in `1.db`, and add only additive migration SQL in `1.sqm`. Open JVM databases with `JdbcSqliteDriver(url = "jdbc:sqlite:${databaseFile.absolutePath}", properties = Properties().apply { put("foreign_keys", "true") }, schema = RhythHausDatabase.Schema)`. Use an `AndroidSqliteDriver.Callback` whose `onOpen(db)` invokes `db.setForeignKeyConstraintsEnabled(true)`. Construct `NativeSqliteDriver` with `onConfiguration = { config -> config.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)) }`. Add focused Android-host and iOS tests that use their production factory paths and prove an invalid playlist-entry foreign key is rejected. Implement playlist add/remove/reorder as one database transaction that rewrites the complete contiguous sequence after all statements succeed. Register `PlaylistRepository` in `rhythHausModule()`.

- [ ] **Step 4: Verify GREEN and migration behavior**

Run: `./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration :shared:jvmTest :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest' --configuration-cache`

Expected: migration verification succeeds; common tests cover repository semantics only; JVM tests upgrade a version-1 fixture and prove playlist-delete, `removeMissingTracks`, `removeSource`, and `clearAll` cascades from the shared database; Android-host and iOS tests each prove production-driver foreign-key enforcement. The iOS test runs with the Task 7 platform matrix.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/build.gradle.kts shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq shared/src/commonMain/sqldelight/migrations/1.sqm shared/src/commonMain/sqldelight/databases/1.db shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt shared/src/iosTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseIosTest.kt
GIT_MASTER=1 git commit -m "feat: add durable playlist repository"
```

Expected: one independently reviewable persistence slice. This explicit list cannot stage `ArtworkCollapse.kt`, `LibraryChrome.kt`, or `ArtworkCollapseTest.kt`.

### Task 2: Occurrence-Aware Playback and Ordered-Pair DataStore Compatibility

**Parallel rule:** Task 1 and Task 2 may run in parallel only after committing the invariant `PlaylistEntry.id == QueueOccurrence.id` for saved-playlist playback. This task does not depend on Task 1 implementation otherwise.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt`

**Interfaces:**
- Produces: `data class QueueOccurrence(val id: String, val track: PlayableTrack)` and `PlaybackState.queue: List<QueueOccurrence>` with `currentOccurrenceId: String?`.
- Produces: `PlaybackSessionSnapshot(queue: List<SessionQueueEntry>, currentOccurrenceId: String?, ...)`, where `SessionQueueEntry(occurrenceId: String, trackId: String)` preserves order and permits repeated `trackId` values.
- Consumes: `PlaylistEntry.id` as an occurrence ID only for saved playlist playback; generic visible lists generate fresh occurrence IDs.

- [ ] **Step 1: Write failing duplicate-occurrence and compatibility tests**

```kotlin
@Test
fun sessionRoundTripKeepsDuplicateTrackOccurrencesInOrder() {
    val snapshot = PlaybackSessionSnapshot(
        queue = listOf(SessionQueueEntry("entry-1", "track-a"), SessionQueueEntry("entry-2", "track-a")),
        currentOccurrenceId = "entry-2",
    )
    assertEquals(snapshot, PlaybackSessionCodec.decodeSnapshot(PlaybackSessionCodec.encodeSnapshot(snapshot)))
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache`

Expected: compilation fails because occurrence queue/session types and pair codec APIs do not exist.

- [ ] **Step 3: Implement occurrence identity without changing media identity**

```kotlin
data class QueueOccurrence(val id: String, val track: PlayableTrack)
data class SessionQueueEntry(val occurrenceId: String, val trackId: String)
```

Make selection, current lookup, shuffle order, skip order, checkpoint keys, restore, and reconciliation use `QueueOccurrence.id`. Pass `occurrence.track.id` to the engine. Store all ordered pairs in one framed preference string and keep legacy `queue_ids` and `current_id` readable: normalize valid legacy IDs into deterministic unique occurrence IDs, preserve membership, and restore paused without autoplay. Reject duplicate occurrence IDs but allow duplicate track IDs.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache`

Expected: selecting the second duplicate loads its occurrence, skip/shuffle distinguish duplicates, legacy snapshots normalize deterministically, duplicate sessions restore in order paused, and reconciliation preserves a surviving current occurrence without reload.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/{Playback.kt,library/LibraryPlaybackSelection.kt,session} shared/src/commonTest/kotlin/com/eterocell/rhythhaus/{PlaybackControllerTest.kt,library/LibraryPlaybackSelectionTest.kt,session} shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt
GIT_MASTER=1 git commit -m "feat: preserve queue occurrences in playback sessions"
```

### Task 3: Serialized Upcoming Queue Mutations

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt`

**Interfaces:**
- Produces: `sealed interface QueueMutationResult { data object Applied; data class Rejected(val reason: QueueMutationRejection) }`.
- Produces: controller suspend commands `reorderUpcoming(occurrenceId: String, targetUpcomingIndex: Int)`, `removeUpcoming(occurrenceId: String)`, and `clearUpcoming()` serialized on the controller command boundary.

- [ ] **Step 1: Write failing stale-command and preservation tests**

```kotlin
@Test
fun removeCurrentOccurrenceIsRejectedWithoutChangingPausedPosition() = runTest {
    controller.setQueue(occurrences, selectedOccurrenceId = "current")
    controller.seekTo(12_000)
    controller.pause()
    assertEquals(QueueMutationResult.Rejected(QueueMutationRejection.CurrentOccurrence), controller.removeUpcoming("current"))
    assertEquals(12_000, controller.state.value.positionMillis)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`

Expected: compilation fails because no queue mutation result or upcoming-only command exists.

- [ ] **Step 3: Implement serialized latest-state commands**

Each command validates its occurrence ID and target index after entering the controller serialization boundary. Accepted commands replace only the upcoming segment, publish exactly one complete state, regenerate runtime shuffle membership from occurrence IDs without changing modes, and emit an immediate checkpoint. Rejected stale IDs, current ID, and invalid positions leave state untouched and return a recoverable result.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`

Expected: concurrent command tests serialize, one duplicate occurrence moves independently, clear keeps the loaded current occurrence, and accepted edits retain position/status/modes/generation with a session checkpoint.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/{PlaybackControllerTest.kt,session/PlaybackSessionCoordinatorTest.kt}
GIT_MASTER=1 git commit -m "feat: add serialized upcoming queue commands"
```

### Task 4: Typed Routes, Playlist State, and Localization

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`

**Interfaces:**
- Produces: `LibraryRoute.PlaylistHub` and `LibraryRoute.PlaylistDetail(playlistId: String)`.
- Produces: `PlaylistTab { Saved, Queue }` and a state reducer that represents loading, confirmed data, retryable read failure, and recoverable mutation failure.
- Produces: App-owned `PlaylistRepository` injection, playlist snapshot refresh after confirmed writes and library-source mutations, and explicit playlist dependencies threaded through `LibraryHomeScreen`, `LibraryRouteContent`, and `LibraryRouteOverlays`.

- [ ] **Step 1: Write failing route and stale-detail tests**

```kotlin
@Test
fun unresolvedPlaylistDetailReturnsToHubWithRecoverableMessage() {
    val next = playlistDetailResolution(playlistId = "missing", resolvedPlaylist = null)
    assertEquals(PlaylistDetailResolution.ReturnToHub("playlist_changed"), next)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --configuration-cache`

Expected: compilation fails because playlist routes and playlist state do not exist.

- [ ] **Step 3: Implement typed state and complete localization**

Inject `PlaylistRepository` in `App.kt`; own the confirmed playlist snapshot beside `libraryTracks`; refresh it after successful playlist writes and after source removal, rescan cleanup, or clear-library completes database cascades and playback reconciliation. Thread the repository, snapshot, callbacks, and recoverable message state through `LibraryHomeScreen`, `LibraryRouteContent`, and `LibraryRouteOverlays`. Add hub/detail route cases exhaustively to navigation, shell, and content switches, plus the Library home Playlists entry and route callbacks. State owns UI selection and error presentation; repository/controller remain mutation owners. Add matching English and Chinese keys for Saved, Queue, creation, rename, delete confirmation, retry, queue changed, add workflows, current/upcoming labels, clear confirmation, move controls, and state descriptions.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --configuration-cache`

Expected: typed route transitions work, stale details return to the hub, non-playlist routes preserve their bar policy, saved playback consumes exact visible occurrence order, and every new string resolves in both locales.

Add a Task 4 lifecycle regression asserting that a source removal or clear-library operation refreshes the App-owned playlist snapshot only after the shared database cascades and playback reconciliation complete.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/{PlaylistState.kt,LibraryNavigation.kt,LibraryAppState.kt,LibraryAppShell.kt,LibraryRoutes.kt,LibraryHomeContent.kt} shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh/strings.xml shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/{LibraryNavigationTest.kt,PlaylistStateTest.kt} shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt
GIT_MASTER=1 git commit -m "feat: add playlist routes and state"
```

### Task 5: Saved UI, CRUD, and Both Add Workflows

**Ordering:** Task 5 begins after Tasks 1 through 4 are accepted and `PlaylistEntry.id == QueueOccurrence.id` is locked. Task 6 follows Task 5 because both modify `PlaylistScreens.kt` and `PlaylistScreensTest.kt`.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`

**Interfaces:**
- Consumes: `PlaylistRepository`, `PlaylistState`, `QueueOccurrence`, and typed playlist routes from Tasks 1, 2, and 4.
- Produces: `PlaylistHubScreen`, `PlaylistDetailScreen`, `AddToPlaylistPicker`, and `PlaylistTrackBrowser` with callbacks that pass IDs and ordered lists only.

- [ ] **Step 1: Write failing saved-workflow tests**

```kotlin
@Test
fun detailBrowserAppendsSelectedTracksInVisibleOrder() {
    val state = PlaylistTrackBrowserState(visibleTrackIds = listOf("b", "a", "c"), selectedTrackIds = setOf("a", "b"))
    assertEquals(listOf("b", "a"), state.confirmedTrackIds())
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`

Expected: compilation fails because saved screen and browser state APIs do not exist.

- [ ] **Step 3: Implement saved presentation and add workflows**

Build Saved tab/detailed rows from repository snapshots. Creation and rename reject blank trimmed values before mutation and retain entered text after failure. Delete requires confirmation and returns to hub on success. Add a row overflow action for home, Search, album, and artist screens that opens a picker with inline create. Add detail-side searchable multi-select from authoritative tracks and append selected IDs in visible order. Keep empty playlists visible after their final entry is removed; saved-row Compose keys are entry IDs.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`

Expected: duplicate entries remain independently addressable; CRUD failures retain confirmed state and text; both add paths append independent entries; accessible move controls and confirmations use localized labels containing names.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/{LibraryRows.kt,LibraryDetailContent.kt} shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt
GIT_MASTER=1 git commit -m "feat: add saved playlist workflows"
```

### Task 6: Queue UI and Accessibility

**Dependency:** Complete and review Task 5 first; Task 6 extends the same `PlaylistScreens.kt` and `PlaylistScreensTest.kt` files.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`

**Interfaces:**
- Consumes: `PlaybackState.queue`, `currentOccurrenceId`, `QueueMutationResult`, and upcoming-only command APIs from Tasks 2 and 3.
- Produces: `QueueTabScreen` where the current occurrence is rendered first, has accurate localized content/state descriptions without a false role when Compose lacks a suitable list-row role, and exposes no drag/remove/move controls.

- [ ] **Step 1: Write failing queue-tab tests**

```kotlin
@Test
fun currentOccurrenceIsPinnedAndHasNoMutationActions() {
    val model = queueTabModel(currentOccurrenceId = "current", queue = occurrences)
    assertEquals("current", model.rows.first().occurrenceId)
    assertFalse(model.rows.first().canRemove)
    assertFalse(model.rows.first().canMove)
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`

Expected: compilation fails because Queue tab model and semantics do not exist.

- [ ] **Step 3: Implement Queue tab and accessible mutations**

Render distinct empty, current, and upcoming states. Key rows by occurrence ID. Current row has a localized current state description and no edit affordances. Upcoming rows offer drag plus labeled move-up/move-down/remove semantics containing the track name. Clear upcoming uses explicit confirmation. On a rejected result, read current controller state again and show the localized queue-changed message. Reserve bottom content padding so the Now Playing bar does not obscure rows.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`

Expected: duplicate upcoming rows remain independent; only upcoming rows mutate; stale rejection refreshes and explains state; semantic labels plus accurate localized content/state descriptions expose current/upcoming status and controls without assigning a false list-row role.

- [ ] **Step 5: Commit the reviewed slice**

```bash
GIT_MASTER=1 git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/{PlaylistScreens.kt,LibraryRoutes.kt} shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt
GIT_MASTER=1 git commit -m "feat: add editable playback queue tab"
```

### Task 7: Integration, Verification, and Durable Evidence

**Ordering:** Task 7 runs last, after Tasks 1 through 6 receive two-stage review.

**Files:**
- Modify: `openspec/changes/playlist-screen/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Create: `.superpowers/sdd/playlist-screen-task-1-report.md` through `.superpowers/sdd/playlist-screen-task-7-report.md`

**Interfaces:**
- Consumes: ownership wiring and publication lifecycle from Task 4, repository cascades from Task 1, occurrence reconciliation from Task 2, and all UI/controller results from Tasks 3 through 6.
- Produces: verification and evidence that source removal and clear library completed database cascades before Task 4 published playlist state and invoked playback reconciliation, without mutating an active resolved queue from saved-playlist edits.

- [ ] **Step 1: Review Task 4's source-to-playlist cascade integration test**

```kotlin
@Test
fun task4ClearLibraryCascadesSavedEntriesBeforePlaybackReconciliation() = runTest {
    removeLibrarySource("source-1")
    assertEquals(emptyList(), playlistRepository.entries("playlist-1"))
    assertEquals(listOf("surviving-occurrence"), controller.state.value.queue.map(QueueOccurrence::id))
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`

Expected: the Task 4 regression first fails until its source-mutation lifecycle publishes playlist state only after database cascades and authoritative playback reconciliation, then passes before Task 7 begins.

- [ ] **Step 3: Implement integration without widening ownership**

Verify Task 4's source-removal, rescan-cleanup, and clear-library lifecycle deletes database tracks first, lets SQL foreign-key cascades complete, then reconciles controller occurrences before publishing refreshed library and playlist state. Verify saved playlist edits never modify the already-active controller queue. Record each task report using the change-specific paths listed above.

- [ ] **Step 4: Run strict verification and manual QA**

Run:

```bash
openspec validate playlist-screen --strict
./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration --configuration-cache
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

Expected: strict validation, migration verification, JVM, Android host, desktop compile, Android assembly, Xcode check, and diff hygiene pass. `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` must execute `LibraryDatabaseIosTest` and prove production-driver foreign-key enforcement before playlist completion may be claimed. iOS main compilation alone is insufficient. If the known common-test `Thread` references prevent the iOS test task from executing `LibraryDatabaseIosTest`, record `[blocked] iOS FK proof` with that command's exact output and stop before the final completion/evidence commit unless the user explicitly accepts the unverified iOS risk. Manually verify compact/wide and light/dark layouts, duplicate saved/queue rows, drag and accessible reorder, keyboard dialogs, current pinning, queue edits during playback, Now Playing non-overlap, and target-device audible playback.

- [ ] **Step 5: Final review, evidence, and commit**

```bash
GIT_MASTER=1 git add openspec/changes/playlist-screen/tasks.md progress.md roadmap.md .superpowers/sdd/playlist-screen-task-*-report.md
GIT_MASTER=1 git commit -m "docs: record playlist screen verification"
```

Expected: OpenSpec checklist, SDD reports, roadmap, and progress preserve actual results, manual-QA gaps, and iOS blocker status. Do not create this final completion/evidence commit while `[blocked] iOS FK proof` remains unless the user explicitly accepts the unverified iOS risk. The execution controller performs the final whole-branch review before this commit and stages only intended change evidence.

## Plan Self-Review

- Spec coverage: Tasks 1 to 6 map one-to-one to the approved seven slices, and Task 7 covers source lifecycle integration, strict validation, platform matrix, manual QA, report evidence, progress, roadmap, and final review.
- Placeholder scan: every task contains concrete paths, interfaces, test code, RED/GREEN commands, expected results, and commit commands.
- Type consistency: saved playlist playback uses `PlaylistEntry.id` as `QueueOccurrence.id`; `QueueOccurrence.track.id` remains the library media identity; `SessionQueueEntry` carries the same occurrence and track identifiers in order. Task 4 owns repository injection/publication and Task 6 follows Task 5 because their screen/test files overlap.

## Subagent-Driven Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-17-playlist-screen.md`. Execute with `superpowers:subagent-driven-development`: create or verify an isolated worktree first, dispatch one fresh implementer for each task, run a task-scoped spec and code-quality review after every task, and dispatch one final whole-branch review after Task 7. Tasks 1 and 2 may run in parallel only after the `PlaylistEntry.id == QueueOccurrence.id` invariant is committed; Task 5 begins after Tasks 1 through 4; Task 6 follows Task 5; Task 7 runs last.
