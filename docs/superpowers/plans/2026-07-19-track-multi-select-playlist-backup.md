# Track Multi-Select and Playlist Backup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace eligible per-track playlist buttons with page-scoped long-press multi-selection and add safe cross-platform logical playlist export/import.

**Architecture:** `LibraryAppShell` owns one route-scoped selection reducer and one mutually exclusive bottom-bar slot. Common backup code owns a strict bounded version-1 codec, unique metadata matching, stale-preview planning, and one repository transaction; Android, iOS, and JVM adapters own only system document presentation and bounded byte I/O.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, SQLDelight 2.3.2, Kotlin coroutines/test, JVM Compose UI tests, Android document contracts, UIKit document picker, AWT FileDialog.

## Global Constraints

- Implement only `openspec/changes/track-multi-select-playlist-backup/` and the approved design at `docs/superpowers/specs/2026-07-19-track-multi-select-playlist-backup-design.md`.
- Selection surfaces are Library home Songs, album detail, artist detail, and Search only; selection never crosses a route/page boundary.
- Preserve ordinary visible-list playback, the playlist-detail searchable browser, duplicate occurrences, and ordered submission.
- Now Playing and the contextual selection bar must never both be visible or interactive.
- Import always creates new playlists, never guesses ambiguous tracks, and commits every eligible playlist in one transaction or none.
- Do not copy, inspect, checkpoint, close, reopen, replace, or migrate `rhythhaus.db` for backup.
- Version 1 is canonical compact UTF-8 JSON: discriminator `rhythhaus-playlist-backup`; root keys `format`, `version`, `exportedAtEpochMillis`, `playlists`, `checksumCrc32`; CRC32 is eight lowercase hex characters over the canonical payload without checksum.
- Enforce 4 MiB, 1,000 playlists, 10,000 entries per playlist, 100,000 total entries, 1,024 Unicode code points per string, and duration `0..604800` seconds.
- Match normalized title/artist/album plus a known duration within inclusive ±2 seconds; zero is unmatched and multiple is ambiguous.
- No new JSON dependency, broad Android storage permission, Windows/Linux support, or SQLDelight schema change.
- Add every English string in Chinese and distinguish cancellation from unavailable integration and failure.
- Use strict TDD, task-scoped review, explicit staging, and Conventional Commits. Do not use `git add -A`.

---

### Task 1: Pure Page-Scoped Selection State

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionState.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionStateTest.kt`

**Interfaces:**

```kotlin
sealed interface TrackSelectionPageKey {
    data object HomeSongs : TrackSelectionPageKey
    data class Album(val album: String) : TrackSelectionPageKey
    data class Artist(val artist: String) : TrackSelectionPageKey
    data object Search : TrackSelectionPageKey
}

data class TrackSelectionState(
    val pageKey: TrackSelectionPageKey? = null,
    val selectedTrackIds: Set<String> = emptySet(),
)

sealed interface TrackSelectionAction {
    data class Start(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class Select(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class Toggle(val pageKey: TrackSelectionPageKey, val trackId: String) : TrackSelectionAction
    data class ReconcileVisible(val pageKey: TrackSelectionPageKey, val visibleTrackIds: List<String>) : TrackSelectionAction
    data class RouteChanged(val pageKey: TrackSelectionPageKey?) : TrackSelectionAction
    data object Cancel : TrackSelectionAction
    data object Completed : TrackSelectionAction
}

fun reduceTrackSelection(state: TrackSelectionState, action: TrackSelectionAction): TrackSelectionState
fun orderedSelectedTrackIds(state: TrackSelectionState, pageKey: TrackSelectionPageKey, visibleTrackIds: List<String>): List<String>
```

- [ ] Write failing tests for start, idempotent select, toggle, final-deselection exit, cancel/completion, route replacement, Search reconciliation, blank-ID rejection, visible ordering, and duplicate visible IDs.
- [ ] Run `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --configuration-cache`; expect compilation failure because the contracts do not exist.
- [ ] Implement a Compose-free reducer; normalize every empty selected set to the default inactive state and derive submission order only from `visibleTrackIds`.
- [ ] Rerun the focused test; expect `BUILD SUCCESSFUL` and zero skipped tests.
- [ ] Commit implementation and test together as `feat: add page-scoped track selection state`.

### Task 2: Long-Press Rows, Checkboxes, and Eligible Surfaces

**Dependencies:** Task 1.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausClickable.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionSemanticsJvmTest.kt`

**Interfaces:**

```kotlin
enum class TrackRowGesture { Click, LongClick }
enum class TrackRowActivation { Play, ToggleSelection, StartSelection }
fun trackRowActivation(selectionModeActive: Boolean, gesture: TrackRowGesture): TrackRowActivation

@Composable
fun Modifier.hausCombinedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
): Modifier
```

- [ ] Write RED dispatch and JVM semantics tests proving click plays only outside selection, long press starts selection without playback, selection clicks toggle exactly once, accessible selection entry exists, and selected/now-playing semantics coexist.
- [ ] Run the selection-state and semantics tests; expect missing activation/combined-click contracts.
- [ ] Add the focused combined-click modifier without changing unrelated `hausClickable` callers.
- [ ] Change `TrackRow` to separate `isNowPlaying`, `selectionModeActive`, and `isSelected`; show a real checkbox before artwork only in selection and remove `onAddToPlaylist`.
- [ ] Add equivalent behavior to Search’s row and thread exact page keys/visible IDs through Home Songs, album, artist, and Search only.
- [ ] Reconcile Search selection whenever filtered IDs change; clear Home selection when leaving Songs.
- [ ] Run focused selection, semantics, and `LibraryNavigationTest`; expect pass.
- [ ] Commit as `feat: add long-press selection to track lists`.

### Task 3: Contextual Bottom Bar and Multi-Track Picker

**Dependencies:** Tasks 1-2.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionBar.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt`
- Modify: English/Chinese `strings.xml`

**Interfaces:**

```kotlin
data class PlaylistPickerState(val trackIds: List<String>, val selectedPlaylistId: String? = null, val enteredName: String = "")
data class PlaylistAppendRequest(val playlistId: String, val trackIds: List<String>)
data class PlaylistInlineCreateRequest(val name: String, val trackIds: List<String>)

sealed interface LibraryBottomBarContent {
    data object Hidden : LibraryBottomBarContent
    data object NowPlaying : LibraryBottomBarContent
    data class Selection(val selectedCount: Int) : LibraryBottomBarContent
}
```

- [ ] Write RED tests for ordered append/create requests, non-empty picker input, picker dismissal retention, mutation-failure retention, successful cleanup, selection precedence, unsupported-route cleanup, and selected-count semantics.
- [ ] Run focused playlist/bottom/navigation tests; expect failure on list-valued picker and bottom policy.
- [ ] Own selection in the app shell, consume Back before route pop, and clear before every route/browse-mode transition.
- [ ] Render exactly one measured bottom slot; propagate the active measured clearance instead of retaining Search’s fixed footer assumption.
- [ ] Open the generalized picker with `orderedSelectedTrackIds`; dismissal closes only picker, failure retains both states, success closes both.
- [ ] Run focused picker, bottom, navigation, and semantics tests; expect pass.
- [ ] Commit as `feat: add contextual selection playlist flow`.

### Task 4: Strict Canonical Backup Codec

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupModels.kt`
- Create: `.../PlaylistBackupCodec.kt`, `.../StrictJsonParser.kt`, `.../Crc32.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodecTest.kt`

**Interfaces:** Define `PlaylistBackupPayload`, `PlaylistBackupDocument`, `PlaylistBackupPlaylist`, `PlaylistBackupEntry`, `PlaylistBackupDecodeResult`, and a typed validation-error enum. `PlaylistBackupCodec.encode(payload)` returns complete checksummed bytes; `decode(bytes)` returns only a fully bounded valid document or one typed invalid result.

- [ ] Write RED golden tests for exact compact bytes/key order, UTF-8 escaping, array/duplicate preservation, CRC32 known vector, checksum, and round trip.
- [ ] Add malformed tests for invalid UTF-8/JSON/surrogates, duplicate/unknown/missing fields, wrong format/version/checksum, fractional/exponential numbers, trailing data, and each exact limit plus one.
- [ ] Run `PlaylistBackupCodecTest`; expect missing contracts.
- [ ] Implement pre-decode byte rejection, strict UTF-8, a fixed-schema token parser with per-object duplicate tracking, integer-only numeric parsing, direct count bounds, canonical writer, and IEEE CRC32.
- [ ] Rerun; expect all golden and adversarial tests pass.
- [ ] Commit as `feat: add strict playlist backup codec`.

### Task 5: Export, Matching, and Import Planning

**Dependencies:** Task 4.

**Files:**
- Create: `.../playlistbackup/PlaylistBackupMatcher.kt`, `.../PlaylistBackupService.kt`
- Create: matching/service common tests.

**Interfaces:** Add typed export success/failure, `normalizePortableText`, unique/unmatched/ambiguous match results, `PlaylistImportPlan(libraryRevision, playlists, totals)`, issue records, and `planPlaylistImport(...)`.

- [ ] Write RED tests for Unicode whitespace rules, locale-independent lowercase, retained punctuation/diacritics, duration ±2 boundaries, null-duration exclusion, unique/unmatched/ambiguous results, duplicate occurrence order, conflict names, all-unmatched skip, totals, repeated import, deterministic export, and missing-duration export failure.
- [ ] Run matcher/service tests; expect missing APIs.
- [ ] Implement an indexed normalized metadata matcher, deterministic localized conflict naming, immutable preview plan, and export from confirmed playlist order plus authoritative track metadata.
- [ ] Rerun; expect pass.
- [ ] Commit as `feat: add playlist backup matching and planning`.

### Task 6: Atomic Multi-Playlist Repository Import

**Dependencies:** Task 5.

**Files:**
- Modify: `PlaylistRepository.kt`, `SqlDelightPlaylistRepository.kt`
- Modify: common repository contract test, JVM SQLDelight repository test, and every compiler-identified repository delegate.

**Interface:** `fun importPlaylists(playlists: List<PlaylistImportMutation>): List<Playlist>` where every mutation has a trimmed nonblank name and non-empty ordered track IDs.

- [ ] Write RED common tests for empty request, validation-before-publish, request order, duplicate entry order, and staged rollback.
- [ ] Write RED real SQLDelight tests injecting failure during the second playlist and proving no imported rows plus unchanged pre-existing rows.
- [ ] Run focused repository tests; expect missing interface.
- [ ] Implement temporary-map publication for memory and exactly one `database.transaction` for SQLDelight; do not loop over separately transactional `createWithEntries`.
- [ ] Run JVM, Android host, and iOS tests needed to compile every implementation/delegate; expect pass.
- [ ] Commit as `feat: add transactional playlist import`.

### Task 7: Android, iOS, and JVM Document Adapters

**Dependencies:** Task 4.

**Files:** Create common and per-platform `playlistbackup/PlatformPlaylistBackupDocuments*` files, platform tests, and the iOS Swift provider/registration files under the existing iOS app structure.

**Interfaces:** Common save results: Success/Cancelled/Unavailable/Failure. Open results additionally include `Success(bytes)` and `TooLarge(maxBytes)`. A Compose `expect/actual rememberPlatformPlaylistBackupDocumentLauncher(...)` owns presentation callbacks.

- [ ] Write RED pure adapter tests for success, null selection, unavailable provider, exceptions, exact 4 MiB, 4 MiB+1, extension handling, and full one-time write.
- [ ] Implement Android `CreateDocument`/`OpenDocument`, bounded streams, vendor/JSON filtering, no broad permission, and no retained import URI.
- [ ] Implement JVM AWT SAVE/LOAD with injected path seams and restoration of any temporary Apple directory property in `finally`.
- [ ] Define the Kotlin iOS completion/provider bridge, compile to discover exact generated Swift names, then implement retained UIKit presentation, bounded import, security-scope balancing, and temporary-export cleanup.
- [ ] Run JVM, Android-host, iOS simulator tests and unsigned simulator Xcode build; expect pass or record exact target blocker.
- [ ] Commit as `feat: add platform playlist backup documents`.

### Task 8: Settings Preview, Confirmation, and Results

**Dependencies:** Tasks 3, 5-7.

**Files:** Create `PlaylistBackupUiState.kt` and `PlaylistBackupDialogs.kt`; modify `App.kt`, `SettingsScreen.kt`, route wiring, resources, and Settings/JVM semantics tests.

- [ ] Write RED state/workflow tests proving build-before-save, silent cancellation, decode/plan-before-write, distinct validation errors, no-restorable disablement, preview dismissal, stale revision rejection, one import call, failure retention, one success refresh, and result counts.
- [ ] Add one centralized authoritative library revision incremented on every accepted track publication; do not reuse playlist publication revision.
- [ ] Build export on the background dispatcher before save; import bounded bytes into decode/plan; compare revision immediately before one `importPlaylists` mutation; rethrow cancellation.
- [ ] Add localized Settings actions, busy states, scrollable shared-dialog preview/result, per-playlist counts, accessible issue rows, and distinct recoverable messages.
- [ ] Run focused backup, repository, Settings state, and JVM semantics tests; expect pass.
- [ ] Commit as `feat: add playlist backup settings workflow`.

### Task 9: Integration, Verification, QA, and Evidence

**Dependencies:** Tasks 1-8 accepted by task-scoped spec and quality review.

**Files:** Create `PlaylistBackupIntegrationJvmTest.kt`; update OpenSpec tasks, `progress.md`, `roadmap.md`, and change-specific `.superpowers/sdd/` reports.

- [ ] Add a real temporary-database integration test proving visible-order selection, duplicate export, absence of local IDs/paths, valid decode, unique/unmatched/ambiguous plan, stale no-mutation, second-playlist rollback, successful ordered restore, all-unmatched skip, and deterministic repeated-import names.
- [ ] Run the integration test. If it exposes a defect, add a focused RED test in the owning slice, fix minimally, and commit the fix separately; never weaken the integration assertion.
- [ ] Run focused JVM selection/picker/codec/matcher/repository/Settings/integration tests, Android-host adapter tests, JVM adapter tests, and iOS bridge tests with no required skips.
- [ ] Run:

```bash
openspec validate track-multi-select-playlist-backup --strict
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

- [ ] Manually exercise every eligible/unsupported selection surface, tap versus long press, checkbox/accessibility action, current-track selection, Search/browse/navigation cleanup, bottom-bar hit targets/clearance, picker dismissal/failure/success, and ordered duplicate append.
- [ ] Exercise available platform panels and valid/cancelled/oversized/malformed/stale/conflicting/unmatched/ambiguous/repeated/failed imports. Record unavailable device or failure-injection paths as unverified, not passed.
- [ ] Invoke visual QA for compact/wide, light/dark, English/Chinese, text/CJK fit, focus order, bar mutual exclusion, preview scrolling, issue accessibility, and platform touch behavior.
- [ ] Invoke final code review and Oracle; require no open Critical or Important finding and verify no raw database access, unbounded reads, permissive JSON, locale matching, first-candidate ambiguity, set-order submission, split transactions, stale revision misuse, or schema-without-migration.
- [ ] Commit the integration test separately as `test: verify playlist backup integration`; then update exact evidence and commit lifecycle files as `docs: record track selection and playlist backup verification`.
- [ ] Do not archive the change without an explicit request.

## Dependency Waves

1. Task 1 and Task 4 can execute independently.
2. Task 2 follows Task 1; Tasks 5 and 7 follow Task 4 and can execute independently.
3. Task 3 follows Tasks 1-2; Task 6 follows Task 5.
4. Task 8 follows Tasks 3, 5, 6, and 7.
5. Task 9 follows acceptance of every prior task.

## Plan Self-Review

- Spec coverage: every approved selection, picker, format, validation, matching, transaction, platform, Settings, accessibility, QA, and evidence requirement maps to Tasks 1-9.
- Type consistency: selected identity remains `Set<String>` while every submission/import boundary uses ordered `List<String>`; backup files contain no local IDs; library revision is distinct from playlist publication revision.
- Scope: no playlist-detail selection rewrite, cloud sync, raw DB backup, schema change, dependency addition, or unsupported platform work.
- Execution risks: Task 3 must define the measured active-bottom-bar clearance explicitly; Task 7 must compile generated Swift bridge spellings before downstream integration.
- Placeholder scan: no TBD/TODO or deferred product decision remains. Platform availability is an evidence condition and must be recorded, not inferred.
