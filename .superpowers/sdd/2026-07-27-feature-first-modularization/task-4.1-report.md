# Task 4.1 Implementation Report

Base SHA: `066e592d22a392fc84b19b9fd37110469c14ca87`

## Changed Paths

- `settings.gradle.kts`
- `shared/build.gradle.kts`
- `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt`

Untracked paths:

- `feature/library/api/build.gradle.kts`
- `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanModels.kt`
- `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibrarySource.kt`
- `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt`
- `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryApiContractTest.kt`
- `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryApiModelsTest.kt`
- `feature/playlists/api/build.gradle.kts`
- `feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`
- `feature/playlists/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistApiContractTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryImplementationModule.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistsImplementationModule.kt`

## RED Evidence

1. `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`
   - Failed as expected before project registration: `Cannot locate tasks that match ':feature:library:api:allTests' as project 'feature' not found in root project 'RhythHaus'.`
2. `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.playlistsApiCannotDependOnCoreModel' --configuration-cache`
   - Failed as expected with one failed test: `UnexpectedBuildSuccess`; the fixture permitted `:feature:playlists:api -> :core:model` before removal from the allow-list.
3. `./gradlew :shared:jvmTest --tests '*RhythHausDiTest' --configuration-cache`
   - Failed as expected while the implementation factory functions were absent: unresolved `libraryImplementationModule` and `playlistsImplementationModule` references. The first test draft also lacked `assertTrue`; this was corrected before production implementation.

## GREEN And Verification

- API modules: `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache` passed, exit 0.
- Architecture policy: focused functional test passed, exit 0. Its final rerun stored a configuration-cache entry.
- DI: `./gradlew :shared:jvmTest --tests '*RhythHausDiTest' --configuration-cache` passed, exit 0, 9 tests. Its final rerun reported `Reusing configuration cache`.
- Integration: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` was run once and failed only because an intermediate DI factory test still attempted to resolve a nonexistent `CoroutineScope`; 562 tests completed, 1 failed. The focused DI rerun passed after that fix. The complete integration command was not rerun.
- `git diff --check` passed with no output.
- Formatting commands were started in required order: `./gradlew spotlessApply --configuration-cache`, then `./gradlew ktlintFormat --configuration-cache`; each timed out after 120 seconds during Gradle configuration, so neither successful formatting result is claimed.
- No retained XML test-count extraction was performed.

## Verification Closure (Resume)

All commands below were run after the DI cleanup repair and formatter completion. Each Gradle invocation used a 600-second timeout.

- `./gradlew spotlessApply --configuration-cache` passed in 2m 58s; its configuration-cache entry was stored.
- `./gradlew ktlintFormat --configuration-cache` failed as expected because the required task does not exist: `Task 'ktlintFormat' not found in root project 'RhythHaus' and its subprojects.` No replacement was invented; the brief-approved Spotless check was used below.
- `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache` passed in 16s and reused its configuration cache. Retained JVM XML: library API 5 tests and playlists API 1 test, all with zero failures/errors.
- `./gradlew :shared:jvmTest --tests '*LibraryRepositoryContractTest' --tests '*PlaylistRepositoryContractTest' --tests '*RhythHausDiTest' --configuration-cache` passed in 27s. Retained XML: 4 library-contract, 11 playlist-contract, and 9 DI tests, all with zero failures/errors.
- `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache` passed in 1m 5s.
- `./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache` passed in 5s, then `./gradlew :build-logic:convention:test -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache` passed in 1m 9s.
- `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` passed in 1s and stored a cache entry; the immediately identical second run passed in 333ms and reported `Reusing configuration cache`.
- `./gradlew :shared:jvmTest :feature:library:api:allTests :feature:playlists:api:allTests :feature:library:api:testAndroidHostTest :feature:playlists:api:testAndroidHostTest :feature:library:api:iosSimulatorArm64Test :feature:playlists:api:iosSimulatorArm64Test :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileKotlinIosSimulatorArm64 --configuration-cache --configuration-cache-problems=fail --no-parallel` passed in 13s and stored a cache entry. Retained XML: shared JVM 562 tests, library API JVM/Android-host/iOS-simulator 5/5/5 tests, and playlists API JVM/Android-host/iOS-simulator 1/1/1 tests; all show zero failures/errors.
- `./gradlew spotlessCheck --configuration-cache` passed in 2m 57s; `./gradlew detekt --configuration-cache` passed in 2s.
- `openspec validate feature-first-modularization --strict` passed: `Change 'feature-first-modularization' is valid`.
- Final `git diff --check` passed with no output. Final status contains only the approved Task 4.1 source/build/test paths listed above; formatter output did not introduce out-of-scope paths.

## Scope And Self-Review

The change registers only API modules, preserves the existing shared implementation ownership, and leaves iOS framework exports unchanged. The playlist public API now returns `PlaylistSummary`; SQLDelight row mapping remains internal. The shared facade includes internal library and playlist Koin factory modules. The architecture fixture removes the obsolete playlists-API-to-model baseline edge and tests its rejection.

## Explicit Deferrals

- `./init.sh` was not run.
- Runtime Android, desktop, and iOS UI launches were not run.
- OpenSpec 4.4 was not run.
- No commit was created.

## Concerns

- `ktlintFormat` is not registered in this repository; its exact failure is retained above. `spotlessApply` and the required standalone `spotlessCheck` both passed.
- `./init.sh`, runtime Android/desktop/iOS UI launches, and OpenSpec 4.4 remain deferred and were not claimed.
- No commit was created.

## Independent-Review Corrections (2026-08-03)

This section preserves the prior RED/GREEN history above and records the follow-up corrections requested by independent review. No controller-owned ledger, OpenSpec status, plan, or design artifact was changed.

### Regression-First RED Evidence

- New architecture selectors for API-to-database, API-to-shared, API-to-implementation, and implementation-to-shared initially failed with `UnexpectedBuildSuccess` because the fixture did not yet provide those mutation cases.
- `./gradlew :shared:jvmTest --tests '*RhythHausDiTest.playlistFactoryOwnsPlaylistRepositoryBinding' --tests '*RhythHausDiTest.sharedCompositionContainsExactlyOneDefinitionForEachRepositoryInterface' --configuration-cache` failed as intended: `RhythHausDiTest.playlistFactoryOwnsPlaylistRepositoryBinding` raised Koin `NoDefinitionFoundException` while resolving the missing `LibraryDatabase` support dependency.
- `./gradlew :shared:jvmTest --tests '*PlaylistSqlDelightRepositoryJvmTest.sqlRepositoryUsesLegacyMissingPlaylistAndEntryMessages' --configuration-cache` failed as intended with `org.junit.ComparisonFailure`, exposing `PlaylistSummary not found` rather than the legacy `Playlist not found` message.

### Corrections

- Expanded the architecture fixture with four dedicated invalid edges and valid shared-to-feature-API/shared-to-feature-implementation composition edges. Existing playlist-API-to-model and implementation-to-other-implementation controls remain. The only removed allow-list edge is the accepted obsolete `:feature:playlists:api -> :core:model` edge.
- Removed duplicate `LibraryTrack.toPlayableTrack()` checks from shared `LibraryModelsTest`; it now retains only the extension case-insensitivity test. API model coverage remains in `feature:library:api`.
- Added isolated Koin ownership coverage. The playlist factory test is JVM-specific because the public `LibraryDatabase` expect class has no common constructor; it provides a test-only SQLite database binding and does not load the library factory. Root composition asserts one typed definition each for `LibraryRepository` and `PlaylistRepository` using Koin resolution APIs.
- Restored four SQLDelight legacy missing-playlist/entry messages and added SQLDelight-path assertions. Restored user-facing fixture labels in playlist screen, state, and edit-mode tests while retaining `PlaylistSummary` as the Kotlin type.

### GREEN And Final Verification

All Gradle invocations below used a 600-second timeout unless noted otherwise.

- Focused new architecture selectors passed: `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.apiCannotDependOnCoreDatabase' --tests '*ArchitectureCheckPluginFunctionalTest.apiCannotDependOnShared' --tests '*ArchitectureCheckPluginFunctionalTest.apiCannotDependOnImplementation' --tests '*ArchitectureCheckPluginFunctionalTest.implementationCannotDependOnShared' --configuration-cache` (18s, cache reused).
- Focused DI, repository-message, and retained shared model tests passed: `./gradlew :shared:jvmTest --tests '*RhythHausDiTest' --tests '*RhythHausDiFactoryJvmTest' --tests '*PlaylistSqlDelightRepositoryJvmTest.sqlRepositoryUsesLegacyMissingPlaylistAndEntryMessages' --tests '*LibraryModelsTest' --configuration-cache` (20s).
- `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache` passed with cache reuse. Retained XML counts: library API 15 across JVM/Android-host/iOS-simulator; playlists API 3 across the same targets; zero failures/errors.
- `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache` passed (1m 16s, cache reused). Retained XML has 57 fixture tests; zero failures/errors.
- `./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache` passed, followed by the canonical `./gradlew :build-logic:convention:test -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache` (1m 24s, cache reused).
- Strict `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` passed twice; the second identical run completed in 330ms and reported `Reusing configuration cache`.
- The exact cross-target integration sequence passed: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version` (Xcode 26.6, build 17F113), and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
- `./gradlew spotlessApply --configuration-cache` passed (3m 10s), then standalone `./gradlew spotlessCheck --configuration-cache` passed (2m 57s). `ktlintFormat` was not retried because its earlier exact unavailable-task result is retained above.
- `./gradlew detekt --configuration-cache` passed. An earlier parallel attempt failed only with `Timeout waiting to lock Configuration Cache`; the serial retry passed in 1s with cache reuse.
- `openspec validate feature-first-modularization --strict` passed: `Change 'feature-first-modularization' is valid`.
- Final `git diff --check` passed with no output. Retained shared JVM XML reports contain 562 tests with zero failures/errors. Status contains only Task 4.1 implementation, test, module, and architecture-fixture paths; the report itself is ignored task evidence.

### Remaining Concerns

- No remaining implementation or verification concern. The existing explicit deferrals remain: `./init.sh`, runtime UI launches, OpenSpec 4.4, and committing are intentionally not performed.
