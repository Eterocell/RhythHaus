# Track Metadata Overrides Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let listeners edit and restore app-local title, artist, album, track number, and disc number without altering scanned tags or audio, with accurate Library and playback presentation.

**Architecture:** SQLDelight owns an override row keyed to stable track ID; Library repository projects effective fields but keeps scan writes raw. Shared serializes edits with publication and owns editor lifecycle. Core playback refreshes queue metadata without changing transport; platform adapters update system labels only when safe without reloading audio.

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Compose Multiplatform, coroutines, Media3, MediaPlayer/Now Playing, macOS native bridge.

**Spec:** `openspec/changes/track-metadata-overrides/specs/track-metadata-overrides/spec.md`; design: `openspec/changes/track-metadata-overrides/design.md`.

## Global Constraints

- Base is `feature/smart-playlists` commit `82eb8894`; do not branch from older `main` or ship the v5→v6 migration without smart-playlists v4→v5.
- Never write media tags, files, source handles, artwork, or duration. Nullable override fields mean the *latest* scanned values.
- Preserve track and queue occurrence IDs, engine generation, playback position, repeat/shuffle, active timer, and audio continuity.
- Metadata edits must not enter scan/destructive-operation admission; publish from the same authoritative owner lock used for favorites/history.
- No new dependency or change to playlist-backup JSON v1. User owns physical-device UI/listening acceptance.

## Review Focus

- Rescan changes raw tags beneath corrections: editor revert reveals the latest raw tag (Task 2).
- Remove-missing/source removal deletes override row; same filename on later import does not inherit it (Task 1/2).
- Delayed scan publication versus accepted edit must not revert visible metadata (Task 5).
- Concurrent queue replacement cannot receive a delayed metadata-only update (Task 3).
- Dismissing editor during a slow save must not close or corrupt another editor instance (Task 6).

---

### Task 1: SQLDelight storage and migration

**Files:** Create `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/TrackMetadataOverride.sq`, `core/database/src/commonMain/sqldelight/migrations/5.sqm`, `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/TrackMetadataOverrideDatabaseTest.kt`; modify `core/database/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt` and SQLDelight schema snapshots via the existing migration-generation task.

**Interfaces:** Table `track_metadata_override(trackId TEXT PRIMARY KEY REFERENCES library_track(id) ON DELETE CASCADE, title TEXT, artist TEXT, album TEXT, trackNumber INTEGER, discNumber INTEGER)`. Queries `selectAllOverrides`, `selectOverrideForTrack`, `upsertOverride`, `deleteOverride`, `selectTrackExists`.

- [ ] RED: add real SQLite upgrade test from v5, migration schema verification, cascade on remove-missing/source removal/clear, and version-zero table recognition; run `./gradlew :core:database:jvmTest --tests '*TrackMetadataOverrideDatabaseTest' --configuration-cache` and observe expected failures.
- [ ] GREEN: implement schema, migration and version-zero mapping; regenerate migration snapshot using repository SQLDelight tasks. Run focused database tests and `:core:database:verifySqlDelightMigration` if configured (check task availability rather than guessing).
- [ ] Review physical schema/migration parity and commit with `feat(database): persist track metadata overrides`.

### Task 2: Repository contract and effective projection

**Files:** Modify `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`, `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`, `SqlDelightLibraryRepository.kt`; add API value types in `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/TrackMetadataOverride.kt`; update `LibraryApiContractTest.kt`, `LibraryRepositoryContractTest.kt`, `SqlDelightLibraryRepositoryJvmTest.kt` and explicit test implementations discovered by `LibraryRepository` reference search.

**Interfaces:** `TrackMetadataOverride(title: String? = null, artist: String? = null, album: String? = null, trackNumber: Int? = null, discNumber: Int? = null)`; `TrackMetadataEditorData(scannedTrack: LibraryTrack, overrides: TrackMetadataOverride)`; `LibraryRepository.metadataForTrack(trackId: String): TrackMetadataEditorData?`; `LibraryRepository.setTrackMetadataOverride(trackId: String, overrides: TrackMetadataOverride): Boolean`. Repository `tracks()` and `tracksForSource()` expose effective metadata, `upsertTrack()` never writes effective fields into raw columns.

- [ ] RED: prove partial override/rescan/revert, stable ID, effective sorting/source reads, missing-track rejection and atomic multi-field replacement for both reference and SQLite implementations. Run focused Library tests and observe failures.
- [ ] GREEN: implement read/write transaction, normalization and effective mapping without loading artwork. Keep `selectTrackBySourceKey` raw and preserve scan-only fields; migrate concrete test doubles without fake success responses.
- [ ] Run `./gradlew :feature:library:impl:jvmTest :feature:library:api:jvmTest --configuration-cache`; review and commit `feat(library): project app-local track corrections`.

### Task 3: Queue metadata-only update

**Files:** Modify `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` and `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`.

**Interfaces:** `PlaybackController.refreshTrackMetadata(tracks: List<PlayableTrack>)` updates metadata for matching queued track IDs (including duplicate occurrences) without touching audio/transport; platform engine refresh is the Task 4 seam `PlatformPlaybackEngine.refreshLoadedMetadata(track: PlayableTrack, generation: Long)`.

- [ ] RED: prove playing and paused queue metadata changes, duplicate occurrences, active timer, no selection/position/repeat/shuffle change, no extra checkpoint for unchanged fields, and a concurrent selection that must win. Run focused `:core:playback:jvmTest --tests '*PlaybackControllerTest*'` and observe failures.
- [ ] GREEN: atomically compare-and-set queue metadata under selection ownership; notify engine for current generation only, without load/seek/play; preserve checkpoint order. Update test engines for the new method.
- [ ] Run Core JVM suite and commit `feat(playback): refresh queued track labels without reload`.

### Task 4: Native metadata continuity

**Files:** Modify `core/playback/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`, `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`, `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`, and native bridge only if needed. Test `core/playback/src/androidHostTest/.../AndroidPlaybackMediaSessionTest.kt`, `iosTest/.../IOSAudioPlayerBridgeTest.kt`, `jvmTest/.../JvmPlaybackEngineTest.kt`.

**Interfaces:** Implement `refreshLoadedMetadata(track, generation)` on the loaded generation. If the Android Media3 session cannot change labels without replacing/reloading the media item, retain the current native item and use the corrected label on the next load; **do not reset playback**.

- [ ] RED: add platform tests that exercise a loaded track, metadata update attempt, unchanged play/seek/load counts and generation, and next-load label; run focused platform tests to observe failure.
- [ ] GREEN: update in-place system metadata where supported; guard generation and document Android limitation if unavoidable. Run Android host/JVM/iOS test compilation and iOS Swift build where bridge changed; commit `feat(playback): refresh native metadata safely`.

### Task 5: Shared authoritative publication

**Files:** Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, Shared application tests in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt` and a new `AppMetadataOverrideTest.kt`.

**Interfaces:** `setTrackMetadataOverrideAndPublish(...)` uses the existing publication owner, repository, dispatcher, and Main-thread `AppLibraryContentState.apply`. `loadLibraryContent` always obtains effective track data. Stale scan publication re-reads effective tracks while holding the publication mutex, not only favorites/history.

- [ ] RED: prove edit during blocked scan, failed/missing write, cancellation after successful persistence, and a delayed publication cannot overwrite the new effective value; verify Core controller refresh after commit with no source scan interruption.
- [ ] GREEN: implement serialized metadata write plus fresh projection and integrate into `App` callbacks; avoid coordinator scan preemption. Run focused Shared tests; review repository/publication ownership and commit `feat(shared): publish metadata edits authoritatively`.

### Task 6: Editor, routing and localization

**Files:** Modify `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`, `LibraryHomeContent.kt`, resource `values/strings.xml` and `values-zh/strings.xml`; create `TrackMetadataEditor.kt` near those leaves. Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` and only necessary route/back files; test in feature `jvmTest/.../ui/TrackMetadataEditorJvmTest.kt` and Shared `jvmTest/.../library/ui/LibraryAppShellJvmTest.kt`.

**Interfaces:** Editor consumes `TrackMetadataEditorData`, local draft, save `TrackMetadataOverride`, cancel and restore actions; its host owns a destination/instance token and loading/error state. Songs-row edit action is a separate 44 dp target hidden in selection mode.

- [ ] RED: test partial edit, restore-one/all, invalid numbers, failed save retains draft, dismiss-without-save, 600×400 scroll and accessibility, EN/ZH labels, and stale completion after editor replacement. Observe failing focused feature and Shared UI tests.
- [ ] GREEN: implement editor leaf and Shared host; use latest scanned value on reopening and prevent stale callbacks from closing a newer editor. Run feature/Shared focused suites and a real desktop launch with a disposable library; commit `feat(library): edit app-local track metadata`.

### Task 7: Integration and closeout

**Files:** `openspec/changes/track-metadata-overrides/tasks.md`, canonical specs, `roadmap.md`, `progress.md`.

- [ ] Run database migration verification, Library/Core playback/Shared focused suites, `:desktopApp:compileKotlin`, `:androidApp:compileDebugKotlin`, `:shared:compileTestKotlinIosSimulatorArm64`, `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict change/spec validation, and `git diff --check`; record existing unrelated failures exactly and do not claim blocked platforms passed.
- [ ] Independently review the full diff against spec, correct findings with regressions, run actual desktop editor smoke (or state an exact control limitation), and leave user-owned physical-device verification open.
- [ ] Sync canonical specs, archive completed change and update roadmap/progress with evidence. Commit with a conventional message; do not merge or push without an explicit decision.
