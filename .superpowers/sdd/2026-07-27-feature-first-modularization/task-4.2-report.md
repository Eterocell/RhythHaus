# Task 4.2 Report

## Ownership and scope

- Worktree: `/Users/eterocell/Git/self/RhythHaus/.worktrees/feature-first-modularization`.
- Baseline: `1f35473b322b430f8d5332178f010314d4280422` (`docs: detail core playback extraction`). No files are staged and no commit was created.
- Route: approved OpenSpec + Superpowers Task 4.2.
- Generated `.superpowers/sdd/2026-07-27-feature-first-modularization/task-4.2-brief.md` exists as the SDD execution handoff. Its unchecked task boxes are not the canonical completion ledger; the approved Superpowers plan, OpenSpec, and progress/evidence reports are canonical.

## Reconciled implementation

- `:core:playback` owns the moved common/controller/session contracts, Android engine/service/transport, iOS engine/audio/artwork/Now Playing bridges, JVM/macOS engine, native helper, matching tests, and the preserved `com.eterocell.rhythhaus` package/FQCN/JNI/resource identities.
- `:shared` remains the composition owner: its package-stable `createPlatformPlaybackEngine()` expect/actual facade builds one core engine for Koin injection into the explicit-engine `PlaybackController`; session coordinator/store/lifecycle remain shared-owned.
- `:core:playback` depends only on `:core:model` and `:core:platform` among project modules. `:shared` exposes it through `api` and exports exactly it from the Shared iOS framework. Android Media3 dependencies and the macOS native-helper/resource task moved from `:shared` to core playback.
- Review correction 1: the processor-backed `corePlaybackPositivePolicyAcceptsPreservedPackagesAndIosExport` fixture compiles production roots in both `com.eterocell.rhythhaus` and `.session`, then verifies the real architecture policy. The allow-list remains limited to Core Playback -> Model/Platform and Shared -> Core Playback export.
- Review correction 2: `IOSRelativeFilePathResolver` is defined only at `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSRelativeFilePathResolver.kt` with `resolve(relativePath: String): String`; the retained Shared iOS factory supplies it to `createIOSPlaybackEngine`.
- Review correction 3: the strict explicit-API surface has explicit visibility and KDoc at the specified public boundaries. `normalizeLegacyQueue` is `internal`; Shared retains its private legacy queue mapper.
- Review correction 4: `shared/build.gradle.kts` has no Media3 dependencies; Core Playback Android owns ExoPlayer and Session.

## Retained RED evidence

- The original absent-module RED removed only `include(":core:playback")`; `./gradlew :core:playback:allTests :core:playback:compileKotlinJvm --configuration-cache` then failed on the retained Shared `projects.core.playback` references. The include was restored before accepted GREEN checks.
- The original policy RED removed only Core Playback's Model/Platform allow-list edge and produced the expected `ARCH-EDGE` diagnostics. The export RED changed only the corresponding export policy and produced `ARCH-IOS-EXPORT :shared -> :core:playback`. Both mutations were restored.
- The prior correction lane's package-root RED changed only the fixture Core Playback root to `com.eterocell.rhythhaus.playback`; the processor-backed positive selector failed, and the root was immediately restored. These destructive policy mutations were not rerun by this lane.

## Root-cause correction

- The prior report incorrectly interpreted `ARCH-PACKAGE :core:playback:com/eterocell/rhythhaus/PlaybackEngineFactory.kt ()` as missing KSP package-root propagation. The parentheses are the source package name.
- Controller inspection identified `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackEngineFactory.kt` as a zero-byte stray artifact. It was deleted and was not recreated. No KSP option propagation or approved allow-list rule was changed.
- Controller verification after that deletion: `./gradlew :core:playback:compileKotlinJvm --no-configuration-cache --no-build-cache --rerun-tasks` passed in 4s with 23 actionable tasks, all executed.

## Current verification

- PASS: the same clean correction compile command above; this lane also reran it successfully in 3s, 23 executed tasks.
- PASS: `./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache` (1s, 13 actionable tasks), followed by `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.corePlaybackPositivePolicyAcceptsPreservedPackagesAndIosExport' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache` (34s).
- PASS: `./gradlew :core:playback:allTests :core:playback:compileKotlinJvm --configuration-cache` (17s, 72 tasks); `:core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest'` (2s); `:core:playback:testAndroidHostTest` (1s); `:core:playback:iosSimulatorArm64Test` (1s); and `./gradlew :shared:jvmTest --tests '*PlaybackSessionCoordinatorTest' --tests '*PlaybackSessionStoreJvmTest' --tests '*RhythHausDiTest' --tests '*RhythHausDiFactoryJvmTest' --tests '*PlaylistLifecycleIntegrationJvmTest' --tests '*AppDispatcherJvmTest' --tests '*AppScanCancellationTest' --tests '*LibraryPlaybackSelectionTest' --configuration-cache` (6s, 76 tasks).
- PASS: `./gradlew :build-logic:convention:test -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache` (2m22s). `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` passed in 740ms and the identical second run passed in 283ms with `Reusing configuration cache.`
- PASS: `./gradlew :core:playback:allTests :shared:jvmTest :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileKotlinIosSimulatorArm64 :core:playback:compileKotlinIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 --configuration-cache` (2s, 96 tasks). This proves Android packaging, desktop compilation, native helper/resource processing, and Shared simulator/device framework linkage.
- PASS: `/usr/bin/xcrun xcodebuild -version`, then `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` (`BUILD SUCCEEDED`), then `./gradlew :core:playback:iosSimulatorArm64Test :shared:iosSimulatorArm64Test --configuration-cache` (20s, 104 tasks).
- PASS: `./gradlew spotlessApply --configuration-cache` (9s), standalone `./gradlew spotlessCheck --configuration-cache` (8s), standalone `./gradlew detekt --configuration-cache` (710ms), `openspec validate feature-first-modularization --strict`, `test -s "core/playback/build/generated/nativeAudioResources/jvmMain/native/$(case "$(uname -m)" in arm64|aarch64) printf 'macos-aarch64' ;; *) printf 'macos-x64' ;; esac)/librhythhaus_audio.dylib"`, and `git diff --check`.

## Hygiene and deferrals

- Earlier implementation-verification snapshot status: only Task 4.2 implementation/move/policy files and the untracked `core/playback/**` plus Shared factory files were present; no staging and no commit.
- Expected warnings remain: Gradle 9 deprecations, Kotlin expect/actual beta warnings, Android Media3 `setArtworkData` deprecation, test-only iOS casts/non-null assertions, signing configuration notice, TagLib Android-host source-set notice, and Xcode AppIntents metadata skip. No verification gate failed.
- At this earlier implementation-verification snapshot, `./init.sh` and runtime UI/app launches were not run or claimed, and no OpenSpec, progress, roadmap, acceptance report, or plan checkbox had been modified.
- Earlier implementation-verification snapshot commit status: none. The actual implementation-only commit is now `ab1768c` (`refactor: extract playback contracts`); the accompanying separate docs closeout commit remains SHA-unknown and carries tracked ledgers plus force-added ignored evidence files.

## KDoc re-review repair

- Independent re-review rejected 98 exact `/** Playback API. */` placeholders: 65 in `Playback.kt`, 9 in `PlaybackSessionController.kt`, and 24 in `PlaybackSessionSnapshot.kt`.
- Replaced all public placeholders with declaration-specific contract KDoc. The audit covered engine callbacks/operations, controller state/checkpoints/commands/callbacks, `FakePlaybackEngine`, the session behavioral port and revisioned value, snapshot/codec/checkpoint/progress properties, every public enum entry, and all legacy snapshot constructor parameters. The four `RevisionedShuffleOrder` helper properties are explicitly internal within their private holder, so they are not public API and carry no placeholder KDoc.
- Structural regression guard PASS (before: 98; after: 0): `! rg -n -F '/** Playback API. */' core/playback/src --glob '*.kt'`. The companion inventory grep verified all four public enums and the legacy constructor/`legacyTrackIds` parameter.
- First narrow compile run correctly exposed missing KDoc processing on the four effectively-private shuffle-order properties after their placeholders were removed. Marking those members `internal` preserves their private enclosing-type boundary and satisfies the public-contract gate. The rerun PASS: `./gradlew :core:playback:compileKotlinJvm :core:playback:allTests --configuration-cache` (configuration cache reused).
- PASS after `spotlessApply`: standalone `./gradlew spotlessCheck --configuration-cache`; standalone `./gradlew detekt --configuration-cache`; `git diff --check`.
- This repair changes only Task 4.2 Core Playback KDoc and the Task 4.2 report. No behavior, dependencies, architecture policy, tests, consumer modules, staging, or commits were changed.

## Controller acceptance test-race repair

- Controller acceptance RED retained before this test-only repair: `./gradlew :core:playback:allTests :shared:jvmTest :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileKotlinIosSimulatorArm64 :core:playback:compileKotlinIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 --configuration-cache` failed with one JVM, one Android-host, and one iOS failure. `./gradlew :core:playback:allTests --rerun-tasks --no-configuration-cache` then failed first-run with one JVM and two iOS failures; isolated JVM selectors and the full JVM class passed.
- Root cause: `setQueue()` publishes synchronously but starts `loadPaused()` asynchronously. `DelayedStatusPlaybackEngine.complete()` could observe generation 0 before initial load assigned generation 1, which production correctly rejects as stale. `RecordingPlaybackEngine.awaitLoadCount()` also polled mutable lists across dispatchers, which is a Kotlin/Native data race.
- Test-only fix: both fake engines now append a cumulative load-sequence notification to an unlimited `Channel<Int>` immediately after recording the load. `awaitLoadCount(count)` receives buffered notifications under a timeout until the cumulative count reaches `count`, establishing happens-before for recorded tracks/generations. All nine `DelayedStatusPlaybackEngine` callers are `runBlocking` tests with readiness barriers before completion, dependent transport, or load-state reads; no sleeps, yields, expected values, or production behavior changed.
- Focused GREEN: the previously failing stop-after-current JVM selector passed three consecutive `--no-configuration-cache` runs. The corresponding iOS simulator selector command passed (native test filtering executes the simulator test task).
- Repeated GREEN: `./gradlew :core:playback:allTests --rerun-tasks --no-configuration-cache` passed three consecutive runs, covering JVM, Android host, and iOS simulator tests. `./gradlew :shared:jvmTest --configuration-cache` passed and stored a configuration-cache entry. `git diff --check` passed.
- Final retained XML inspection PASS: `PlaybackControllerTest` reported 53 tests, 0 failures, and 0 errors on JVM, Android host, and iOS simulator.
- Scope: only `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt` changed for this repair, plus this report. No production, Gradle, architecture, staging, or commit changes were made.

## Final acceptance closeout

- Documentation-only acceptance accepted Task 4.2 and OpenSpec 5.3/5.4. The implementation-only commit is `ab1768c` (`refactor: extract playback contracts`); the accompanying separate docs closeout commit remains current, carries the tracked ledgers and force-added ignored brief/reports, and has no claimed SHA.
- See `task-4.2-final-acceptance-report.md` for separated delegated, independent-review, and controller evidence, including the explicit runtime/init deferrals. OpenSpec 4.4 remains open.
- The later documentation-only closeout modified the Superpowers plan checklist, OpenSpec tasks, root `progress.md`, `roadmap.md`, SDD `progress.md`, this report, and the final acceptance report. Staging and commit still had not occurred at that point.
