# Playlist Screen Task 1 Report

Status: DONE

## Scope

Implemented the Task 1 persistence slice only in the isolated `playlist-screen` worktree. Playback, session, navigation, UI, and Tasks 2-7 were not modified. `PlaylistEntry.id` remains the stable durable occurrence identity intended to become `QueueOccurrence.id` in Task 2.

## Files

- `shared/build.gradle.kts`: enabled SQLDelight schema output and the existing JDBC SQLite driver for Android host callback testing.
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq`: playlist tables, cascades, indexes, and ordered CRUD queries.
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`: non-destructive UPSERT preserving referenced playlist entries during metadata refresh.
- `shared/src/commonMain/sqldelight/migrations/1.sqm`: additive playlist migration without transaction delimiters.
- `shared/src/commonMain/sqldelight/databases/1.db`: real SQLite version-1 pre-playlist baseline.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`: contract, `PlaylistEntry`, and in-memory implementation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`: transaction-backed SQL implementation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`: `PlaylistRepository` registration.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`: schema-aware JDBC opening with foreign keys.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt`: production callback enabling foreign keys.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt`: native configuration enabling foreign keys.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`: in-memory semantics only.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`: migration, SQL parity, real rollback, cascades, and JVM FK tests.
- `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt`: production callback FK proof only; not a production factory execution claim.
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseIosTest.kt`: production iOS factory FK test prepared for Task 7.

The brief-listed `LibraryRepository.kt` and `SqlDelightLibraryRepository.kt` required no source change because their existing track deletions already share the database transaction domain; JVM tests prove the new FK cascades through `removeMissingTracks`, `removeSource`, and `clearAll`.

## Strict RED Evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --configuration-cache
```

First RED exposed one test-fixture typo (`Sequence::next`); after correcting only the test, the second RED failed at `:shared:compileTestKotlinJvm` exclusively on missing production symbols: `InMemoryPlaylistRepository`, `PlaylistEntry`, `SqlDelightPlaylistRepository`, and generated `playlistQueries`. Exact result: `BUILD FAILED in 1s`.

## GREEN and Verification Evidence

- Focused GREEN after implementation and real v1 baseline: the RED command passed, initially 11 tests; after review-strengthening SQL parity/cascade/rollback coverage, the focused suite passed again.
- Required focused gate:

```bash
./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration :shared:jvmTest :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest' --configuration-cache
```

Final result: `BUILD SUCCESSFUL in 7s`; 69 actionable tasks, 8 executed, 61 up-to-date.

- Fresh supported matrix:

```bash
./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: `BUILD SUCCESSFUL in 4s`; 126 actionable tasks, 5 executed, 121 up-to-date.

- iOS main compilation:

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Result: `BUILD SUCCESSFUL in 3s`.

- iOS test attempt:

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Result: `BUILD FAILED in 2s` at `:shared:compileTestKotlinIosSimulatorArm64`; unchanged `AppScanCancellationTest.kt:64:28` and `:340:27` report `Unresolved reference 'Thread'`. `LibraryDatabaseIosTest` did not execute. `[blocked] iOS FK proof`.

- Baseline integrity: SQLite `PRAGMA integrity_check` returned `ok`, `user_version` returned `1`, and the only tables are `library_source`, `library_track`, `scan_error`, and `scan_session`.
- `openspec validate playlist-screen --strict`: `Change 'playlist-screen' is valid`.
- `GIT_MASTER=1 git diff --check`: pass with no output.
- Kotlin LSP diagnostics unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable Kotlin checks.

### Oracle adjudication: non-destructive track UPSERT

Regression RED command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest.updatingExistingTrackMetadataPreservesReferencedPlaylistEntry' --configuration-cache
```

Against `INSERT OR REPLACE`, metadata assertions succeeded but the final playlist-entry assertion failed at `PlaylistSqlDelightRepositoryJvmTest.kt:54`; exact result: one test completed, one failed, `BUILD FAILED in 3s`. SQLite `REPLACE` deleted the referenced `library_track` row before insertion, activating `ON DELETE CASCADE`.

`LibraryTrack.sq` now uses `INSERT ... ON CONFLICT(id) DO UPDATE SET` and assigns every supplied non-ID column from `excluded`. `SqlDelightLibraryRepository` continues to submit the existing row ID and original creation timestamp for metadata refresh, so row identity and intended update behavior are preserved.

Regression GREEN: the same single-test command passed, `BUILD SUCCESSFUL in 10s`.

Post-adjudication focused gate, run sequentially after stopping Gradle daemons:

```bash
./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration :shared:jvmTest :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 5s`; 69 actionable tasks, 8 executed, 61 up-to-date.

Post-adjudication supported matrix:

```bash
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: `BUILD SUCCESSFUL in 2s`; 116 actionable tasks, 8 executed, 108 up-to-date; 299 JVM tests passed. An earlier attempt incorrectly ran the two Gradle commands concurrently and produced an output-tree cache packing race plus non-reproducible unchanged playback-test timing failures. After `./gradlew --stop`, both unchanged commands passed sequentially; no playback code was modified.

## Self-Review

- Schema and migration are additive and migration verification passes against a real pre-playlist database.
- Stable duplicate occurrence IDs are preserved; SQL and in-memory repositories both trim names, reject blanks, preserve duplicates, maintain contiguous positions, and retain empty playlists.
- SQL add/remove/reorder rewrite the complete sequence inside one transaction. The rollback test installs a trigger that fails the second insert after old rows are deleted, then proves the original sequence is restored.
- JVM production opening uses the required schema-aware constructor and proves FK rejection through the real factory.
- Android production code supplies the exact callback passed to `AndroidSqliteDriver`; the Android-host test executes that production callback and proves invalid playlist-entry foreign keys are rejected. Oracle adjudicated this exact production-callback evidence as sufficient for Task 1.
- The iOS production factory test is present and main compilation passes. Oracle adjudicated actual iOS FK execution as deferred to the explicit Task 7 completion gate, so the unchanged common-test blocker is not a Task 1 blocker and no iOS FK pass is claimed here.
- The track metadata UPSERT preserves the existing row identity and all intended mutable-field updates, preventing `ON DELETE CASCADE` from removing playlist entries during scans.
- No playback/session/UI route or Task 2-7 source was touched.

## Independent Review

- Security: PASS; no Critical/High findings.
- Focused QA: automated migration/JVM/Android gates passed, but its production-path claim was superseded by goal/code/context review.
- Goal verification: FAIL because Android factory execution and iOS FK execution are unproven.
- Code quality: FAIL on the same platform proof gates and initial SQL parity coverage; SQL parity coverage was added and reverified.
- Context mining: FAIL on Android/iOS proof. Its publication concern was resolved against the approved plan: Task 1 has synchronous confirmed reads, while observable snapshot ownership is explicitly Task 4.

## OpenSpec and Commit

OpenSpec checkboxes 1.1-1.4 are complete after Oracle adjudication, post-adjudication RED/GREEN verification, and self-review.

## Blockers

None for Task 1. Deferred Task 7 concern: `LibraryDatabaseIosTest` still requires actual execution after the unrelated `AppScanCancellationTest.kt` `Thread` compilation blocker is resolved or isolated; no iOS FK execution is claimed in Task 1.

## Commits

- `6fbdee9` — `feat: add playlist database schema`
- `92ba80b` — `feat: define playlist repository contract`
- `f846b3f` — `fix: open JVM library database through schema`
- `a6149b1` — `feat: persist playlist entries safely`
- `b70a8c4` — `fix: enforce playlist foreign keys on Android`
- `42c0477` — `fix: enforce playlist foreign keys on iOS`
- `8b54393` — `docs: complete playlist persistence tasks`

Every commit includes the required Sisyphus footer and co-author trailer. This report remains unstaged scratch evidence as required by the Task 1 brief.

## Fix: serialize authoritative playlist mutation reads

Review finding: `SqlDelightPlaylistRepository.append`, `removeEntry`, and `reorder` read authoritative state and built replacements before entering `database.transaction`, allowing a mutation interleaved after that read to be overwritten by a stale complete-sequence rewrite.

OpenSpec lifecycle: task 1.2 was changed from complete to incomplete before the regression/fix and returned to complete only after focused GREEN verification and self-review. Tasks 1.1, 1.3, 1.4 and Tasks 2-7 were unchanged.

Deterministic regression: `concurrentAppendsCannotOverwriteFromAStaleSnapshot` uses an internal no-op-by-default mutation-read observer and two repositories sharing one `LibraryDatabase`. The observer performs a complete second append at the controlled boundary. There are no sleeps, clocks, or scheduler-dependent races: under the old implementation, the first append had already read its snapshot, so its later full rewrite erased the second append.

RED command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest.concurrentAppendsCannotOverwriteFromAStaleSnapshot' --configuration-cache
```

Result against the pre-transaction-read implementation: one test completed, one failed at the final ordered-entry assertion (`PlaylistSqlDelightRepositoryJvmTest.kt:33`), `BUILD FAILED in 2s`. The resulting list omitted the interleaved `track-b` append.

Minimal fix: each `append`, `removeEntry`, and `reorder` now enters `database.transaction` before the controlled observer, authoritative read, existing validation/error behavior, replacement construction, clear, and contiguous reinsertion. `replaceEntries` is now a write-only helper called from those transaction scopes. The public `PlaylistRepository` API and error messages are unchanged.

GREEN command: the same single-test command passed, `BUILD SUCCESSFUL in 5s`.

Focused Task 1 gate:

```bash
./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration :shared:jvmTest :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 5s`; 69 actionable tasks, 10 executed, 59 up-to-date. Existing SQL semantics, rollback, migration, cascade, JVM FK, and Android callback tests passed.

Self-review:

- `append` reads playlist existence and entries, constructs appended entries, clears, and reinserts under one transaction.
- `removeEntry` resolves the entry, reads its playlist entries, constructs the filtered list, clears, and reinserts under one transaction while preserving the existing missing-entry exception text.
- `reorder` reads the current list, performs the existing size/uniqueness/membership validation, constructs the ordered replacement, clears, and reinserts under one transaction.
- The observer is `internal`, defaults to no operation, does not change the public repository contract, and exists only to make the stale-snapshot interleaving deterministic.
- No schema, dependency, playback/session/UI, or Task 2-7 change was made.

Commit: `1bfbf3e` — `fix: serialize playlist entry mutations`, with the required Sisyphus footer and co-author trailer. The OpenSpec 1.2 reopen/re-complete lifecycle produced no net task-file diff because the final reviewed state matches HEAD; this report records the lifecycle and remains unstaged scratch evidence.
