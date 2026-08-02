# Task 3.1 Implementation Report

Route: openspec+superpowers
Owner: implementation
Input: approved Task 3.1 brief
Output: `:core:database` owns SQLDelight schema, drivers, and database tests
Next owner: controller acceptance verification
Blockers: none

## RED evidence

1. `./gradlew :core:database:jvmTest --tests '*ExistingDatabaseMigrationTest' --configuration-cache`
   before module registration failed as required because project `:core:database` did not exist.
2. The same focused command after module/test scaffolding and before production inputs failed at `:core:database:compileTestKotlinJvm` with unresolved `LibraryDatabase`, `RhythHausDatabase`, and `libraryDatabaseFileName`.
3. After moving the policy/TestKit expectation to `:core:database`, the focused selector
   `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.sqlDelightOwnershipViolationsFail' --configuration-cache --rerun-tasks`
   failed while the task still used its literal owner.
4. After `ArchitectureCheckTask` was changed to read the policy, before moving production ownership,
   `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail`
   failed with `ARCH-SQLDELIGHT expected=:core:database owners=:shared`.

## GREEN evidence

- `./gradlew :core:database:jvmTest --tests '*ExistingDatabaseMigrationTest' --configuration-cache`: passed. The retained XML reports 3 tests: v1 migration/rows/FKs/cascade, legacy-v0 bootstrap, and generated identity/filename.
- `./gradlew :core:database:jvmTest --configuration-cache`: passed; exact rerun reported `Reusing configuration cache`.
- `./gradlew :core:database:generateCommonMainRhythHausDatabaseInterface --configuration-cache`: passed.
- `./gradlew :core:database:testAndroidHostTest --configuration-cache`: passed.
- `./gradlew :core:database:iosSimulatorArm64Test --configuration-cache`: passed.
- `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.sqlDelightOwnershipViolationsFail' --configuration-cache`: passed.
- `./gradlew architectureCheck --configuration-cache-problems=fail --configuration-cache`: passed; exact rerun reported `Reusing configuration cache`.
- `./gradlew :shared:jvmTest --configuration-cache`: passed with 559 tests after repository-test rewiring.
- `git diff --check`: passed.

## Ownership and identity audit

- Registered `:core:database` with core KMP, Android-KMP library, and SQLDelight conventions; it has Android, JVM, `iosArm64`, and `iosSimulatorArm64` targets only.
- Moved the six SQL files, `migrations/1.sqm`, and `databases/1.db` to `core/database/src/commonMain/sqldelight/` unchanged. SHA-256 comparisons against `5dad051` verified all eight moved inputs byte-for-byte.
- SQLDelight remains `com.eterocell.rhythhaus.library.RhythHausDatabase`, SQLite 3.38, schema version 2, and `rhythhaus.db`.
- `LibraryDatabase` expect/actuals and Android/iOS host tests are core-owned. Public API is explicit and documented; the internal package-stable filename constant is tested in core.
- `LibraryDatabaseContext` remains public in shared and forwards Android application context to the documented public core setter. Core has no shared dependency; shared exposes core database with `api`, and no iOS framework export was added.
- Shared no longer applies SQLDelight or owns its runtime/drivers/coroutines extension. Repository implementations, DI, mapping, and repository behavior coverage remain in shared.
- SQLDelight owner policy and functional fixtures now use `:core:database`; the checker reads the expected owner from the policy and retains missing/two/arbitrary/spoofed ownership coverage.

## Changed paths

- `settings.gradle.kts`
- `core/database/build.gradle.kts`
- `core/database/src/commonMain/{kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt,sqldelight/**}`
- `core/database/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.*.kt`
- `core/database/src/{androidHostTest,iosTest,jvmTest}/kotlin/com/eterocell/rhythhaus/library/*.kt`
- `shared/build.gradle.kts`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`
- the corresponding removed shared database source, SQLDelight, and host/iOS test paths
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/{PlaylistSqlDelightRepositoryJvmTest,SqlDelightLibraryRepositoryJvmTest}.kt`
- `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/{ArchitectureAllowList,ArchitectureCheckTask}.kt`
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`

## Scope and risk review

No plan, OpenSpec, progress, roadmap, prior report, or controller-owned amendment was edited. No app-to-core dependency or shared iOS export was introduced. No schema/query/migration/FK/name change was made. No commit was created.

## Bounded architecture-fixture repair addendum

### Controller RED evidence before this lane

- `externalSqlDelightRootIsReportedWithoutConfigurationFailure` failed because it appended `sqldelight {}` to `:shared`, which no longer applies SQLDelight.
- `qualityCheckRunsChildDetektAndSpotlessChecks` failed with `ARCH-SQLDELIGHT expected=:core:database owners=:shared` from its nested `architectureCheck`.
- `unapprovedIosFrameworkExportFails` was already GREEN. Its stale shared SQLDelight/driver setup was removed without changing the exact `ARCH-IOS-EXPORT` assertion.

### Local RED and GREEN evidence

1. Assertion-only ownership RED:
   `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.sqlDelightOwnershipViolationsFail' --configuration-cache --rerun-tasks`
   failed after changing only the arbitrary-owner expectation to
   `ARCH-SQLDELIGHT expected=:core:database owners=:shared`; the fixture still discovered no shared owner.
2. Fixture repair configured the external root on `:core:database`; made the arbitrary mutation remove core ownership and create the sole recognized shared SQLDelight owner; kept shared SQLDelight-free in the quality and iOS-export fixtures; and configured canonical core ownership in quality aggregation.
3. Legacy-v0 RED:
   `./gradlew :core:database:jvmTest --tests '*ExistingDatabaseMigrationTest' --configuration-cache --rerun-tasks`
   failed at the newly added generated-query assertion because the v0 bootstrap had no `legacy-track` row. The seed now inserts that matching track before reopening.
4. GREEN commands, run serially:
   - `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.externalSqlDelightRootIsReportedWithoutConfigurationFailure' --configuration-cache --rerun-tasks`: pass.
   - `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.qualityCheckRunsChildDetektAndSpotlessChecks' --configuration-cache --rerun-tasks`: pass, including the test's second-build `Reusing configuration cache.` assertion.
   - `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.unapprovedIosFrameworkExportFails' --configuration-cache --rerun-tasks`: pass; this selector was already GREEN before this lane.
   - `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.sqlDelightOwnershipViolationsFail' --configuration-cache --rerun-tasks`: pass.
   - `./gradlew :core:database:jvmTest --tests '*ExistingDatabaseMigrationTest' --configuration-cache`: pass; retained JVM XML is 3 tests, 0 failures, 0 errors, 0 skipped.

### Broad verification status

Superseded by the second bounded fixture repair below. The former seven-fixture blocker is resolved.

### Exact changed paths

- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`
- `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt`
- `.superpowers/sdd/2026-07-27-feature-first-modularization/task-3.1-implementation-report.md`

### Residual risks and status

- No commit was created.

### `git status --short`

```text
 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md
 M .superpowers/sdd/2026-07-27-feature-first-modularization/task-2.2-final-acceptance-report.md
 M build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
 M build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt
 M build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
 M docs/superpowers/plans/2026-07-27-feature-first-modularization.md
 M openspec/changes/feature-first-modularization/tasks.md
 M progress.md
 M roadmap.md
 M settings.gradle.kts
 M shared/build.gradle.kts
 D shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt
 D shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt
 D shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibrarySource.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanError.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanSession.sq
 D shared/src/commonMain/sqldelight/databases/1.db
 D shared/src/commonMain/sqldelight/migrations/1.sqm
 D shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt
 D shared/src/iosTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseIosTest.kt
 D shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt
 M shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt
 M shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt
?? core/database/
?? shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt
```

## Second bounded architecture-fixture repair addendum

### Retained RED and root cause

The controller's full-class run retained 52 tests, 7 failures, 0 errors, and 1 skipped test. The seven failures were:

- `composeConventionReportsInvalidNamespacesExactly`, `composeConventionSupportsRegisteredCustomRootsAndRejectsExternalRoots`, and `composeConventionConfiguresDeclaredCustomRootForResourceGeneration`: `Configuration with name 'commonMainImplementation' not found` because `:shared` no longer received incidental KMP setup from SQLDelight.
- `androidApplicationSyntheticTestSelfEdgesAreObservedWithoutControlledPublisher` and `androidApplicationSyntheticTestSelfEdgesAreExcludedWhileMainResourcesRemainCanonical`: `FileNotFoundException` for `core/database/build.gradle.kts` because the fixture included neither the core module nor its build file.
- `unregisteredProductionKspTargetsRemainArchitectureEdges` and `conventionOwnedProductionKspTargetsExcludeOnlyProcessorToolingEdges`: unexpected `ARCH-EXPLICIT-API :core:database` because the canonical core owner was not strict explicit API after the SQLDelight helper wrote its build file.

This was one fixture-setup root cause: SQLDelight ownership had previously supplied unrelated KMP/module setup incidentally. No production source, build file, schema input, migration test, or controller artifact was modified in this repair.

### Helper repairs and focused outcomes

- `fixture()` now explicitly configures `:shared` as non-strict KMP before shared dependencies and source mutations, without applying SQLDelight to shared. All three Compose selectors passed serially.
- `androidApplicationFixture()` now includes and creates `:core:database`, configures its SQLDelight driver/artifact and explicit API, and explicitly configures `:shared` as non-strict KMP. No app-to-core dependency was added. Both Android selectors passed serially. The canonical resource-record assertion now includes the core database's standard KMP resource roots.
- `targetRegistrationFixture()` now creates `:shared` before configuring its non-strict KMP setup and enables explicit API on canonical `:core:database` after the SQLDelight helper writes its build file. Both target-registration selectors passed serially.
- During focused retries, stale configuration-cache/test output first repeated an obsolete missing-file failure; `:build-logic:convention:cleanTest` before the same focused selector forced current fixture bytecode. The only subsequent diagnostics were the missing module-creation calls above, fixed within the allowed helper file.

### GREEN verification

- Seven focused selectors passed, run serially with `./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '<selector>' --configuration-cache --rerun-tasks` where output refresh was required.
- `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache --rerun-tasks`: passed. Retained XML: 52 tests, 0 failures, 0 errors, 1 skipped.
- `./gradlew :build-logic:convention:test --configuration-cache --rerun-tasks`: passed. Retained XML total: 102 tests, 0 failures, 0 errors, 1 skipped.
- `git diff --check`: passed after the repair.
- Stop condition: none; all requested broad gates passed.

### Exact changed paths for this repair

- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`
- `.superpowers/sdd/2026-07-27-feature-first-modularization/task-3.1-implementation-report.md`

### Residual risks and final status

- Gradle emitted pre-existing deprecation and environment warnings during test runs; no test failure or configuration-cache failure remains.
- No commit was created.

### Final `git status --short`

```text
 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md
 M .superpowers/sdd/2026-07-27-feature-first-modularization/task-2.2-final-acceptance-report.md
 M build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
 M build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt
 M build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
 M docs/superpowers/plans/2026-07-27-feature-first-modularization.md
 M openspec/changes/feature-first-modularization/tasks.md
 M progress.md
 M roadmap.md
 M settings.gradle.kts
 M shared/build.gradle.kts
 D shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt
 D shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt
 D shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibrarySource.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/Playlist.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanError.sq
 D shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanSession.sq
 D shared/src/commonMain/sqldelight/databases/1.db
 D shared/src/commonMain/sqldelight/migrations/1.sqm
 D shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt
 D shared/src/iosTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseIosTest.kt
 D shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt
 M shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt
 M shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt
?? core/database/
?? shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt
```
