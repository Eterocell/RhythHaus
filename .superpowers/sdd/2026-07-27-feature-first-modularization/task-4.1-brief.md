## Task 4.1: Publish Library And Playlist APIs

**Scope:** Slice 4 contracts only. Create `:feature:library:api` and
`:feature:playlists:api`, but create no physical feature implementation module. Until later
migration tasks, repository implementations, persistence adapters/mappers, scanner/platform
seams, backup, UI state, validation helpers, playback-selection helpers, and Koin remain in
`:shared`.

**Existing files:** `settings.gradle.kts`; `shared/build.gradle.kts`;
`build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`;
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`;
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`;
`LibraryModels.kt`; `LibraryPlaybackSelection.kt`; `SqlDelightLibraryRepository.kt`; `PlaylistRepository.kt`;
`SqlDelightPlaylistRepository.kt`; and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`. Existing behavior
coverage remains in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`,
`LibraryModelsTest.kt`, `LibraryPlaybackSelectionTest.kt`, and `PlaylistRepositoryContractTest.kt`,
plus `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`
and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`.

**Target files:** `feature/library/api/build.gradle.kts`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibrarySource.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanModels.kt`,
and `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/{LibraryApiContractTest,LibraryApiModelsTest}.kt`;
`feature/playlists/api/build.gradle.kts`,
`feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`,
and `feature/playlists/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistApiContractTest.kt`;
updated `settings.gradle.kts`, `shared/build.gradle.kts`, `ArchitectureAllowList.kt`, and
`ArchitectureCheckPluginFunctionalTest.kt`; and the listed shared contracts, implementations,
call sites, and contract/DI tests. The affected PlaylistSummary production call sites are
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`, and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`. The affected tests are
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibrarySourceManagementTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`, and
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`.
Source directories are module-local, but all API declarations retain
`package com.eterocell.rhythhaus.library`; do not introduce a
`com.eterocell.rhythhaus.playlists.api` package.

- [ ] RED API boundary/value tests first: author API-only tests in both new module test source sets before settings registration, then run `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`; expected RED: absent-project failure. The Library API inventory is exact: `LibraryRepository.kt` contains `LibraryRepository` and `TrackUpsertResult`; `LibrarySource.kt` contains `LibraryPlatformKind`, `LibrarySourceAccessStatus`, and `LibrarySource`; `LibraryTrack.kt` contains `LibraryTrack`, `TrackArtwork`, and `toPlayableTrack()`; `LibraryScanModels.kt` contains `ScanStatus`, `ScanSession`, and `ScanError`. `LibraryRepository` exposes exactly `upsertSource`, `sources`, `upsertTrack`, `tracks`, `tracksForSource`, `artworkForTrack`, `insertScanSession`, `updateScanSession`, `insertScanError`, `scanErrors`, `removeMissingTracks`, `removeSource`, and `clearAll`. Include `LibraryPlatformKind` wherever the model requires it; use `LibrarySourceAccessStatus`, never nonexistent `SourceAccessStatus`. Keep shared `LibraryModels.kt` for `AudioScanCandidate` and `ScanProgress`.
- [ ] `LibraryModelsTest` currently mixes shared scanner policy with API mappings. Keep `supportedAudioExtensionsAreCaseInsensitive` in shared. Move its two `LibraryTrack` mapping tests into API `LibraryApiModelsTest`, add `LibraryTrack` and `TrackArtwork` nullable-`ByteArray` content-equality/hash-code tests there, and do not duplicate those API-owned assertions in shared. Shared `LibraryRepositoryContractTest` and other repository contract tests remain shared because they exercise shared implementations. Playlist API tests must assert public `PlaylistEntry`, `PlaylistImportMutation`, `PlaylistSummary(id, name, createdAtEpochMillis, updatedAtEpochMillis)`, and all 11 `PlaylistRepository` methods; `playlists`, `playlist`, `create`, `createWithEntries`, and `importPlaylists` return `PlaylistSummary`, never generated `Playlist`.
- [ ] RED architecture policy next: make the fixture's valid baseline omit the unconditional `:feature:playlists:api -> :core:model` dependency. Replace its misleading `Playlist` source under `:feature:library:api` with representative Library API source, add `PlaylistSummary` under `:feature:playlists:api`, and retain only valid shared dependencies in the baseline. Add the edge only in a dedicated `playlistsApiCannotDependOnCoreModel` mutation. While the allow-list still permits that edge, assert its expected failure, run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.playlistsApiCannotDependOnCoreModel' --configuration-cache`, and record expected RED because the build succeeds and misses `ARCH-EDGE`; then remove only the `:feature:playlists:api -> :core:model` allow-list entry and rerun that exact selector for GREEN. Retain `:feature:library:api -> :core:model`.
- [ ] Keep fixture-only negative controls for API -> database/shared/implementation and implementation -> shared/other implementation, plus a positive shared -> implementation composition control; do not create production implementation modules. For API -> shared and API -> implementation reverse-edge controls, isolate construction of the tested edge or assert the exact accompanying `ARCH-CYCLE` diagnostics so the fixture is deterministic.
- [ ] RED shared DI tests in `RhythHausDiTest` for absent internal factories and composition. The tests must resolve `LibraryRepository` to `SqlDelightLibraryRepository` and `PlaylistRepository` to `SqlDelightPlaylistRepository`, asserting both singleton identities. Separately prove factory ownership: the library factory owns the existing `TagLibReader`, `AudioMetadataReader`, `LibraryDatabase`, `LibraryRepository`, `PlatformSourceAccess`, and `LibraryScanner` bindings; the playlist factory owns `PlaylistRepository`. Preserve existing override behavior and reliably cancel the Koin scope/stop Koin in cleanup.
- [ ] GREEN module wiring: register `:feature:library:api` and `:feature:playlists:api` in settings. Apply `build-logic.kmp.feature.api` and `build-logic.android.kmp.library`; configure JVM, Android host, `iosArm64`, and `iosSimulatorArm64` targets to match `:core:model`. Library API has only `:core:model` as a production project dependency. Playlist API has no production project dependency and exposes no `:core:database`, generated `Playlist`, `:core:model`, `:shared`, Koin, or SQLDelight type. Add public visibility and KDoc required by explicit API.
- [ ] Move only the complete stable contracts/models into the API modules. Library moves the four-file inventory above, including `LibraryPlatformKind` and `LibrarySourceAccessStatus`, preserving nullable-byte-array equality/hash code and `toPlayableTrack()`. Keep `AudioScanCandidate`, `ScanProgress`, scanner/platform seams, playback-selection helpers, SQLDelight and in-memory implementations in shared. Playlist moves `PlaylistEntry`, `PlaylistImportMutation`, `PlaylistSummary`, and the 11-method `PlaylistRepository`; keep `PlaylistSnapshot`, backup models/snapshots, UI state, validation helpers, and repository implementations in shared.
- [ ] Wire `:shared` through `api(projects.feature.library.api)` and `api(projects.feature.playlists.api)`, without exporting either API from the iOS framework. Adapt `SqlDelightPlaylistRepository` with a generated-`Playlist` -> `PlaylistSummary` mapper; update `InMemoryPlaylistRepository` and every listed production/test call site to the summary boundary while preserving persistence behavior.
- [ ] Refactor only shared DI composition: add internal `libraryImplementationModule()` and `playlistsImplementationModule()` in `RhythHausDi.kt`, compose both from public `rhythHausModule()`, and leave shared as the sole Koin assembly/startup owner. The library factory owns the current TagLibReader, AudioMetadataReader, LibraryDatabase, LibraryRepository, PlatformSourceAccess, and LibraryScanner bindings; the playlist factory owns PlaylistRepository. API modules have no Koin dependency.
- [ ] Run `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`, then `./gradlew :shared:jvmTest --tests '*LibraryRepositoryContractTest' --tests '*PlaylistRepositoryContractTest' --tests '*RhythHausDiTest' --configuration-cache`; expected GREEN: exact API signatures/value behavior compile on their declared targets and shared implementation behavior/override behavior remains shared and passes.
- [ ] Run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache`. For full processor/convention integration, run `./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache`, then `./gradlew :build-logic:convention:test -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache`; build logic maps that project property to its test system property. Then run `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` twice and require strict configuration-cache reuse on the second run. Run `./gradlew :shared:jvmTest :feature:library:api:allTests :feature:playlists:api:allTests :feature:library:api:testAndroidHostTest :feature:playlists:api:testAndroidHostTest :feature:library:api:iosSimulatorArm64Test :feature:playlists:api:iosSimulatorArm64Test :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileKotlinIosSimulatorArm64 --configuration-cache --configuration-cache-problems=fail --no-parallel`. Then run `./gradlew spotlessApply --configuration-cache`, followed by standalone `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt --configuration-cache`. Run `openspec validate feature-first-modularization --strict` and `git diff --check -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md` only for this planning update; task implementation must use its actual changed-file diff. Do not claim runtime UI or `./init.sh` unless run; OpenSpec 4.4 remains separate.
- [ ] Obtain independent review, close the evidence ledger, and only then stage the actual implementation paths and create the planned conventional commit `refactor: publish library and playlist APIs`. Publish no physical implementation modules in this task.
