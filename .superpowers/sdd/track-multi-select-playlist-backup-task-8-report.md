# Task 8 Report: Settings Preview, Confirmation, and Results

## Scope and route

- Route: `openspec+superpowers`, strict RED/GREEN TDD.
- Input: `.superpowers/sdd/task-8-brief.md`, approved design/plan, and accepted Tasks 1-7 through `53df4d380d1cf46b4ec0d6090c00ee3aa02e16ec`.
- Scope: shared backup UI state/workflow, Settings actions, preview/result dialogs, App orchestration, authoritative library revision, playlist mutation ownership, English/Chinese resources, and focused JVM semantics/workflow tests.
- Preserved without staging or modification by Task 8 commits: controller-owned `.superpowers/sdd/progress.md`, generic `.superpowers/sdd/task-1-report.md`, generic `.superpowers/sdd/task-2-report.md`, and `openspec/changes/track-multi-select-playlist-backup/tasks.md`.
- Not changed: codec grammar, matching policy, repository transaction semantics, platform picker internals, database schema, dependencies/toolchain, selection UI, progress, roadmap, or OpenSpec task status.

## Implemented behavior

### Pure state and workflow

- Added Compose-free `PlaylistBackupUiState.kt` with explicit idle/export/save/open/plan/import operations, preview, result, recoverable error, busy, dismissal, panel cancellation, and coroutine-cancellation transitions.
- Export preparation runs on the supplied background dispatcher, builds complete canonical bytes, and fully decodes them before returning saveable bytes. Export validation failures never reach the document launcher.
- Import preparation strictly decodes bounded bytes and plans against the current authoritative tracks, existing confirmed playlist names, localized imported suffix, and captured library revision without mutation.
- Validation/UI outcomes distinguish unavailable, read failure, write failure, oversized, malformed, checksum, unsupported version/format, stale preview, missing export track/duration, invalid duration/data, and repository failure.
- Confirmation preserves ordered duplicate track IDs and converts every eligible planned playlist to one ordered `List<PlaylistImportMutation>`.
- No-restorable plans remain inspectable but are not confirmable; preview dismissal writes nothing.

### Authoritative revisions and mutation ownership

- Added one `AuthoritativeLibraryPublicationOwner` in `App.kt`. Every accepted authoritative `libraryTracks` publication—initial load, completed scan publication, source removal, and clear-library—goes through its single `publish` path and increments one app-owned monotonic library revision exactly once.
- Playlist publication revision remains separate and is not reused for stale checks.
- The owner uses one mutex for accepted library publication and the final expected-revision/import block. The revision check and exactly one `repository.importPlaylists(...)` call therefore cannot be interleaved by an accepted library publication.
- Confirmation captures the preview revision immutably before launching. A stale revision returns `StalePreview` without invoking the repository.
- `PlaylistStateOwner.importPlaylists` retains existing serialized mutation ownership, performs exactly one Task 6 atomic repository call, loads exactly one post-mutation snapshot under the same mutex, and returns the owner-issued playlist publication revision.
- Repository failure retains the preview and last confirmed playlist snapshot and performs no refresh/publication. Success closes preview, publishes the returned snapshot exactly once, and shows result totals.
- `CancellationException` is rethrown by workflow/owner helpers. App orchestration first reduces the operation to idle while retaining preview state, then rethrows, preventing a permanently busy Settings state.

### Settings and dialogs

- Added Settings export/import actions with unavailable and busy disablement to prevent duplicate system-panel launches.
- Added `PlaylistBackupDialogs.kt` using the existing `HausDialog`, `HausColors`, Miuix buttons, 44/48dp targets, and existing spacing/type hierarchy.
- Preview presents total and per-playlist restorable/unmatched/ambiguous counts, skipped/no-restorable playlists, and individually navigable merged semantic issue rows containing playlist, track, and issue kind.
- Shared `HausDialog` bounds the panel to 480dp and scrolls the body while keeping actions outside the scroll region.
- Settings and dialog action buttons use full-width vertical layouts to avoid compact-width, CJK, and larger-font compression.
- Result dialog reports playlists created/skipped and entries restored/unmatched/ambiguous, with exact English singular/plural variants.
- Added complete paired English and Simplified Chinese resources, including `Imported` / `导入` name suffixes.

## TDD evidence

### Initial RED

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest.authoritativeLibraryRevisionAdvancesExactlyOncePerAcceptedPublication' \
  --configuration-cache --rerun-tasks
```

Expected result: `:shared:compileTestKotlinJvm FAILED` only on missing Task 8 symbols such as `PlaylistBackupUiState`, reducer/workflow functions, and `AuthoritativeLibraryPublicationOwner`.

### Serialized-owner RED

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest.serializedConfirmationUsesOneOwnerCallThatReturnsTheRefreshedSnapshot' \
  --configuration-cache --rerun-tasks
```

Expected result: compilation failed on missing `confirmPlaylistBackupImportSerialized`.

### UI semantics RED

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDialogsSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest.backupActionsRequireAvailableLauncherAndIdleWorkflow' \
  --configuration-cache --rerun-tasks
```

Expected result: production/test compilation failed on missing dialogs, Settings parameters, policy API, and resources.

### Review-blocker RED

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest.authoritativePublicationCannotInterleaveBetweenRevisionCheckAndMutation' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest.cancellationCleanupReturnsIdleAndRetainsPreviewWithoutError' \
  --configuration-cache --rerun-tasks
```

Expected result: compilation failed on missing shared authoritative revision guard and cancellation action.

### Focused GREEN

Final focused command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.*' \
  --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' \
  --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' \
  --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' \
  --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 11s`; all Task 8 workflow/UI semantics, all playlistbackup JVM tests, Settings tests, playlist state tests, repository contract tests, and real SQLDelight playlist repository tests passed.

Post-review focused command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDialogsSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' \
  --configuration-cache
```

Result: `BUILD SUCCESSFUL in 8s`.

## Full verification

Sequential final gates after all review fixes:

```bash
./gradlew :shared:jvmTest --configuration-cache
./gradlew :desktopApp:compileKotlin --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
```

Results:

- `:shared:jvmTest`: pass, `BUILD SUCCESSFUL in 6s`.
- `:desktopApp:compileKotlin`: pass, `BUILD SUCCESSFUL in 525ms`.
- `:androidApp:assembleDebug`: pass, `BUILD SUCCESSFUL in 10s`; only the unchanged Android `MediaMetadata.Builder.setArtworkData` deprecation warning appeared.
- `GIT_MASTER=1 git diff --check`: pass with no output.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` installation was previously declined; forced Gradle compilation/tests are the executable Kotlin diagnostics.

## Review and visual QA

- Initial multi-lane review passed focused QA but found two real blockers: a revision check-to-mutation interleaving risk/mutable preview reread, and cancellation leaving UI busy. Both received RED regressions and were fixed with the shared authoritative mutex, immutable revision capture, and cancellation cleanup.
- Final narrow re-review: PASS. It verified one mutex spans every accepted publication and final compare/import block, immutable expected revision, idle/retained-preview cancellation behavior, responsive actions, exact singular/plural resources, and passing focused tests.
- Security review confirmed strict bounded input, validation-before-save/mutation, generic localized errors, no raw path/error leakage, and Task 6 transaction integrity after blocker correction.
- Source/semantics visual QA confirms real design-system reuse, bounded scrolling, 44/48dp targets, full-width responsive actions, complete issue semantics, and paired resources.
- Pixel-level compact/wide, light/dark, English/Chinese font metrics, focus rings, and target-device rendering remain unverified because no screenshot/runtime capture was produced in this Task 8 session. No pixel pass is claimed; Task 9 owns broader runtime/visual QA.
- The preview body is bounded and scrollable but composes all valid reports/issues eagerly. This matches the requested inspectable complete preview and is accepted for Task 8; large-dataset runtime performance remains a Task 9 QA observation, not a codec/matcher policy change.

## Commits

All commits include the required Sisyphus footer and co-author trailer:

- `8e8f647 feat: add playlist backup workflow state`
- `f3debbc feat: serialize playlist backup import`
- `2e8be10 feat: add playlist backup dialogs`
- `2b01901 feat: add playlist backup settings actions`
- `904c17d feat: add playlist backup settings workflow`

## Acceptance and next owner

- Task 8 requirements: implemented and verified.
- Files staged/committed: Task 8 source, direct tests, and paired resources only.
- Controller-owned evidence and OpenSpec task status: preserved and intentionally unstaged per user instruction.
- Next owner: controller/Task 9 for real JVM end-to-end integration, platform panel exercise, target runtime visual QA, OpenSpec lifecycle updates, and final change-wide evidence.
- Blockers: none for Task 8 focused implementation. Pixel/runtime and Task 9 integration evidence remain explicitly deferred to their owning task.

## Independent-review follow-up - 2026-07-19

This appendix supersedes the earlier line that accepted eager preview composition. The preview now uses a bounded lazy body and no pixel/runtime QA claim is added.

### Findings resolved

- Decoder recovery now maps every `PlaylistBackupValidationError` exhaustively. Playlist-count, per-playlist-entry-count, total-entry-count, string-length, blank-name, and invalid-duration errors map to the dedicated `PlaylistBackupUiError.InvalidData`; malformed UTF-8/JSON/shape/numeric/canonical errors remain `Malformed`, while oversized/checksum/unsupported outcomes remain distinct. English and Chinese now use separate syntax/structure and invalid-field-value messages.
- `PlaylistBackupPreviewDialog` now uses `HausLazyDialog`: the body is one bounded `LazyColumn`, while actions remain fixed outside it. Reports and issues are keyed lazy items. Reports are indexed once with `associateBy(sourcePlaylistIndex)`, and every issue resolves its playlist name through O(1) map access.
- The maximum valid preview test constructs 1,000 reports and 100,000 issues, proves the final issue semantics node is absent before scrolling, jumps directly to lazy index 101,001, proves the final issue becomes accessible, and proves the fixed confirm action remains present.
- Stale/no-call, ordered duplicate mutation conversion, repository failure retention/no publication, success single snapshot publication/result totals, and cancellation propagation now all exercise `confirmPlaylistBackupImportSerialized`, the production path. The unused parallel `confirmPlaylistBackupImport` helper was removed.
- App export preparation, import planning, and confirmation now share `runPlaylistBackupOperation`. The orchestration-level regression proves cancellation publishes idle state with the same preview before rethrowing `CancellationException`.

### Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDialogsSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest.backupOrchestrationPublishesIdleRetainedPreviewBeforeRethrowingCancellation' \
  --configuration-cache --rerun-tasks
```

Result: expected `:shared:compileTestKotlinJvm FAILED` on missing `PlaylistBackupUiError.InvalidData`, `PlaylistBackupPreviewListTag`, lazy preview API, and `runPlaylistBackupOperation`. No production test executed before those required APIs existed.

The first lazy maximum-plan GREEN attempt compiled production and passed 18/19 focused tests, but `performScrollToNode` linearly searched 100,000 virtual targets and ended after 8m47s with `UncompletedCoroutinesError`. Root-cause correction changed the test to indexed `performScrollToIndex(101_001)` without weakening the pre-scroll absence/final-row accessibility assertions.

### GREEN and gates

Focused production-path command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiStateTest' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDialogsSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest.backupOrchestrationPublishesIdleRetainedPreviewBeforeRethrowingCancellation' \
  --configuration-cache --rerun-tasks
```

Result: pass, `BUILD SUCCESSFUL in 11s`.

Complete focused group:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.*' \
  --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' \
  --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' \
  --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' \
  --configuration-cache --rerun-tasks
```

Result: pass, `BUILD SUCCESSFUL in 11s`.

Sequential full gates:

```bash
./gradlew :shared:jvmTest --configuration-cache
./gradlew :desktopApp:compileKotlin --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
```

Results: JVM pass in 12s; desktop pass in 517ms; Android pass in 8s with only the unchanged artwork metadata deprecation warning. `GIT_MASTER=1 git diff --check` passed. Kotlin LSP remains unavailable because installation was previously declined.

Pixel/runtime QA remains unexecuted and unclaimed. The four controller-owned progress/generic-report/OpenSpec files remain preserved and unstaged.
