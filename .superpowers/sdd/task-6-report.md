# Task 6 Report: Process-owned playback session and post-reconcile publication

## Outcome

Task 6 wires playback persistence ownership to the process, restores the session once after the first authoritative library snapshot is available, removes Compose disposal ownership, moves Android initialization into `RhythHausApplication`, and publishes initial/scan/remove/clear library content only after playback reconciliation completes.

## RED evidence

1. Lifecycle tests before production implementation:
   - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`
   - Result: `BUILD FAILED`; `PlaybackProcessLifecycle` was unresolved, proving the process-owned restore boundary was absent.
2. Initial restore/publication tests before App orchestration:
   - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`
   - Result: `BUILD FAILED`; `publishInitialLibraryContent` and the lifecycle test seam were unresolved.
3. Reconcile-before-publication tests before helper changes:
   - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`
   - Result: `BUILD FAILED`; `publishLibraryContentAfterReconcile` was unresolved and remove/clear helpers had no reconciler parameter.

## GREEN evidence

- Exact focused Task 6 suites:
  - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 8s`.
- JVM, desktop, and Android compilation:
  - Command: `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 6s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only.
- Xcode availability:
  - Command: `/usr/bin/xcrun xcodebuild -version`
  - Result: `Xcode 26.6`, build `17F113`.
- iOS main compilation:
  - Initial command: `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`
  - Initial result: failed because the existing common coordinator helper used JVM-only `Throwable.initCause`.
  - Root-cause fix: use multiplatform `CancellationException(message, cause)` with unchanged cancellation semantics.
  - Final result: `BUILD SUCCESSFUL in 3s`.
- Kotlin LSP:
  - `lsp_diagnostics` could not run because `kotlin-ls` is not installed and installation was previously declined; Gradle compiler gates are authoritative.
- Diff hygiene:
  - Command: `GIT_MASTER=1 git diff --check`
  - Result: pass, no output.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt`
  - Adds one mutex-created process-scope deferred restore attempt; caller cancellation cannot cancel, reset, or replace it.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
  - Binds one process scope, engine, controller, session store, coordinator, reconciler alias, and lifecycle as Koin singletons while preserving repository/artwork wiring.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - Uses lifecycle-safe `LaunchedEffect` for initial restore/reconcile/publication, blocks playback-affecting source mutations until ready, removes Compose release ownership, and reconciles before initial/scan/remove/clear publication.
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/RhythHausApplication.kt`
  - Initializes database/playback Android contexts and idempotent Koin at process startup.
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`
  - Retains notification permission and rendering only; removes context/Koin startup.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
  - Uses the multiplatform cancellation-exception cause constructor required for iOS main compilation.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
  - Covers concurrent/repeated restore, cancelled first waiter, exactly one coordinator restore, production singleton bindings, and interface aliases.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`
  - Covers initial restore/reconcile/publish order, publication blocking while restore is active, scan publication order, FailedSafeApplied publication, and retained background clear behavior.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`
  - Covers remove/clear reconcile-before-publish ordering and preserves access-release and failure ordering.

## Self-review

- The first caller creates the restore attempt under a mutex in a `SupervisorJob` process scope; all callers await the same deferred.
- No waiter owns or cancels the deferred. The cancelled-first-waiter test proves a second waiter completes the same one coordinator restore.
- The production Koin module resolves one engine/controller/store/coordinator/lifecycle and aliases controller/reconciler interfaces to those same instances.
- Android manifest already names `.RhythHausApplication`; no manifest change was necessary.
- Desktop and iOS startup remain idempotent calls to `startRhythHausKoin()` and do not acquire Compose ownership.
- Initial library state is withheld until restore and reconciliation finish, preventing user playback/source mutations against unreconciled state.
- Scan repository/scanner work remains on `Dispatchers.Default`; reconciliation completes before the main-thread publication block.
- Remove/clear repository mutation and access release ordering remain inside the supplied background dispatcher; reconciliation occurs after that work and before publication.
- Both `Applied` and `FailedSafeApplied` publish because publication follows any successful reconcile return and there is no rejected result.
- `DisposableEffect` controller release and Compose flush/release ownership are absent.
- Existing unrelated edits to Task 1 and Task 2 reports were not staged or modified by Task 6.

## Concerns

- Android Application startup is validated by source review plus successful Android assembly; there is no dedicated androidApp Application test harness in this repository.
- Full iOS tests were not required for Task 6 and retain the known common-test `Thread` blocker; iOS main compilation passed.
- Kotlin LSP remains unavailable by prior user choice.

## Commits

- `9d6da3a` `fix: keep coordinator cancellation multiplatform`
- `8f91102` `feat: own playback restore by process`
- `788e689` `feat: publish library after playback reconcile`
- `08caffd` `feat: initialize playback graph in application`
- `2a17c1c` `docs: record process playback integration`
