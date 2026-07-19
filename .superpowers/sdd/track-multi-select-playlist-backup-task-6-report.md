# Track Multi-Select Playlist Backup — Task 6 Report

## Scope

Implemented atomic ordered multi-playlist import for both `InMemoryPlaylistRepository` and `SqlDelightPlaylistRepository`. Existing `createWithEntries` behavior remains available for picker flows. No backup codec/matcher/planning, UI, platform document, Settings, dependency, toolchain, SQLDelight query/schema/migration, database baseline, or database version file changed.

## RED / GREEN TDD

### RED

Added common repository contract tests and a real JVM SQLDelight transaction test before production code, then ran:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' \
  --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' \
  --configuration-cache --rerun-tasks
```

Result: expected `:shared:compileTestKotlinJvm` failure in 20 seconds. The defining errors were unresolved `PlaylistImportMutation` and `importPlaylists`; downstream type-inference errors were consequences of those missing contracts. No Task 6 production implementation existed.

### GREEN

Implemented the minimum contract and repositories, updated the compiler-identified explicit delegate, and reran the exact focused command.

The first runtime run compiled successfully and exposed two test-only assertions that incorrectly imposed global repository order while all fixture timestamps were equal; rollback assertions already passed. The tests were corrected to assert exact returned request order and exact repository membership/count without changing production behavior.

Final focused result: `BUILD SUCCESSFUL in 8s`; 26 actionable tasks, all executed; configuration cache reused. The focused group contained 27 passing tests.

## Delivered Behavior

- Added `PlaylistImportMutation(name: String, trackIds: List<String>)` and `PlaylistRepository.importPlaylists(playlists): List<Playlist>`.
- Empty requests return an empty list without calling the ID/time factories or mutating state.
- Every request is validated before any ID/time call or mutation: names are trimmed and must remain nonblank; each track-ID list must be non-empty; every track ID must be nonblank.
- Request order, per-playlist track order, duplicate track occurrences, and independent entry IDs are preserved. Returned playlists follow request order.
- The in-memory repository builds playlists and entries in temporary copied maps. Only after every ID/time call and row construction succeeds are both complete maps published; staged failure on the second playlist entry leaves existing maps unchanged, and retry imports both playlists exactly once.
- The SQLDelight repository uses exactly one outer `database.transaction` around all playlist and entry inserts. It does not call or loop through separately transactional `createWithEntries`.
- A real JDBC SQLite trigger aborts insertion of the second imported playlist's first entry. The JVM test proves no imported playlist or entry survives, the pre-existing playlist and entry are unchanged, and successful retry creates both ordered playlists once with duplicate/order preservation.

## Verification

Commands ran sequentially:

```bash
./gradlew :shared:jvmTest --configuration-cache --rerun-tasks
```

Pass: `BUILD SUCCESSFUL in 13s`; 26/26 actionable tasks executed.

```bash
./gradlew :shared:testAndroidHostTest --configuration-cache --rerun-tasks
```

Task 6 common/main and Android host test sources compiled. The first complete run executed 426 tests and failed only unchanged `PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint` (425 passed). A first isolation command used an incorrect package and found no tests; the corrected forced isolated command passed:

```bash
./gradlew :shared:testAndroidHostTest \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint' \
  --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 9s`; 61/61 actionable tasks executed.

A second fresh complete Android host run again executed 426 tests and failed only a different unchanged playback timing test, `PlaybackControllerTest.repeatOneReplaysCurrentTrackButManualTransportCanMoveWithoutWrapping` (425 passed). Therefore the full Android host gate is reported as blocked by pre-existing intermittent playback tests, not as a pass; no unrelated playback edits were made.

```bash
/usr/bin/xcrun xcodebuild -version
```

Pass: Xcode 26.6, build 17F113.

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache --rerun-tasks
```

Pass: `BUILD SUCCESSFUL in 51s`; 44/44 actionable tasks executed. Existing unrelated iOS test-source warnings remained.

Kotlin LSP diagnostics were requested for all five modified Kotlin files, but `kotlin-ls` is not installed and installation was previously declined. Forced Gradle compilation/tests are the executable language checks.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`
- `.superpowers/sdd/track-multi-select-playlist-backup-task-6-report.md`

Controller-owned `.superpowers/sdd/progress.md`, generic Task 1/2 reports, and `openspec/changes/track-multi-select-playlist-backup/tasks.md` were pre-modified and were neither edited nor staged by Task 6.

## Diff / Scope Audit

- `GIT_MASTER=1 git diff --check`: pass with no output before report creation; final staged diff check is performed before commit.
- Task 6 changes contain no `.sq`, `.sqm`, schema, migration, database baseline/version, dependency, version-catalog, Gradle, toolchain, platform document, UI, Settings, backup codec/matcher/planning, OpenSpec, progress, roadmap, or generic report file.
- The SQL implementation contains one transaction block for the new import method and direct generated query inserts only.

## Commit

Planned atomic commit: `feat: add transactional playlist import`, with the required Sisyphus footer and co-author. The interface, both implementations, compiler-required delegate, direct tests, and this report form one compile- and acceptance-atomic Task 6 change.

## Handoff

Task 8 can convert stale-revision-validated ordered planned playlists into `PlaylistImportMutation` values and invoke this single atomic repository API. The full Android host gate remains blocked only by the two documented unchanged intermittent playback tests; focused repository, full JVM, Xcode, and iOS simulator verification passed.
