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

## Review-finding follow-up: deterministic failure publication

### Root causes

- Initial orchestration only published from the success path. A terminal exception from the shared restore deferred or reconciliation left `initialLibraryReady` false and the UI on its initial empty snapshot indefinitely.
- Scan reconciliation happened before the main-thread terminal publication block. A reconciliation exception or cancellation could therefore leave scan progress at `Scanning`/`Cancelling`, keep the newly scanned repository state hidden, and block future source mutations.
- Remove/clear helpers correctly completed repository mutation and access release before reconciliation, but a later reconciliation exception prevented publication of the now-authoritative repository snapshot and left removed/cleared sources visible.
- The lifecycle retained one terminal deferred by design, but tests covered only success and individual waiter cancellation, not shared failure or process-scope cancellation observed by later callers.

### Failure policy

- `InitialLibraryPublicationState` is the production App gate and owns pending/ready content plus error state. Before a terminal result it blocks mutations and publishes no library content. Normal restore/reconcile completion and non-cancellation terminal failure both publish the authoritative snapshot exactly once and make mutations available. Failure also flows through the existing `importMessage` UI mechanism.
- The process lifecycle retains the same failed or cancelled shared deferred. Later callers observe the same outcome and no automatic restore retry occurs.
- Cancellation from a gone Compose/job owner is rethrown without error publication. Scan cancellation with a still-live owner first publishes authoritative content with a non-active terminal scan state, then rethrows.
- Scan/remove/clear reconciliation exceptions publish their already-authoritative repository content and an error message. Repository mutation and source-access release failures still propagate and never publish a false success.

### Follow-up RED evidence

1. Initial policy and lifecycle failures:
   - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`
   - Result: `BUILD FAILED`; `InitialLibraryPublicationState`/`updateState` were absent. After adding the state seam, the retained-failure test exposed that the real coordinator intentionally converts controller restore failure to failed-safe success, so lifecycle terminal-failure behavior was isolated through the lifecycle's real shared restore action seam.
2. Scan failure cleanup:
   - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`
   - Result: `BUILD FAILED`; `ScanPublicationState` and `publishScanContentAfterReconcile` were absent.
3. Remove/clear review cases were added before running their focused suite; they exercise the newly required error callback and failure-safe publication path, plus access-release failure propagation.

### Follow-up GREEN and platform evidence

- Focused suites:
  - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 1s`.
- JVM/desktop/Android:
  - Command: `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 11s`; existing Android artwork deprecation warning only.
- iOS main:
  - Command: `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 12s`.
- Diff hygiene:
  - Command: `GIT_MASTER=1 git diff --check`
  - Result: pass, no output.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.

### Follow-up self-review

- Normal returns still reconcile before publication, including `Applied` and `FailedSafeApplied`.
- Non-cancellation initial restore/reconcile failures publish once, set ready, unlock mutations, and expose an error.
- Ordinary cancellation is never converted into an error publication.
- Scan cleanup retains an existing terminal session outcome; only active/cancelling sessions are converted to deterministic `Cancelled` or `Failed` terminal states.
- Remove/clear only enter failure-safe publication after repository mutation, access release, and authoritative reload all succeeded. Earlier failures propagate unchanged.
- No engine, codec/store, coordinator behavior, dependency, SQL, UI layout, product docs, OpenSpec, progress, roadmap, or Task 1-5 report was changed.

## Final review finding: remove/clear cancellation owner liveness

### Root cause and policy

- After remove/clear mutation, access release, and authoritative reload completed, `publishLibraryContentAfterReconcileFailureSafe` rethrew every reconciliation `CancellationException` before publication. Unlike scan, it did not distinguish a disposed owner from an independently cancelled reconciliation while the mutation owner remained live.
- Remove/clear now receive the same owner-liveness seam used by scan. A gone owner gets cancellation rethrown with no publication or error. A live owner first receives the authoritative post-mutation content and cancellation message, then cancellation is rethrown so the launch wrapper continues to treat cancellation distinctly.
- Repository mutation, access release, and authoritative reload remain outside this failure-safe publication boundary. Their failures or cancellations still propagate without false publication.

### Final RED/GREEN evidence

- RED:
  - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`
  - Result: `BUILD FAILED`; the new active/gone owner remove/clear tests could not compile because `ownerIsActive` was absent from the helpers.
- GREEN focused suites:
  - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 7s`.
- JVM/desktop/Android:
  - Command: `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 16s`; existing Android artwork deprecation warning only.
- iOS main:
  - Command: `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 15s`.
- Diff hygiene:
  - Command: `GIT_MASTER=1 git diff --check`
  - Result: pass, no output.
- Kotlin LSP remained unavailable because `kotlin-ls` is not installed and installation was previously declined.

### Final self-review

- Active-owner remove cancellation publishes the repository state without the removed source/track, reports the cancellation, proves access was released after deletion, then rethrows.
- Active-owner clear cancellation publishes empty authoritative content, reports the cancellation, proves snapshotted access was released after clear, then rethrows.
- Gone-owner remove/clear cancellation publishes and reports nothing.
- Existing repository mutation and access-release failure tests continue to prove no false publication before authoritative reload.

## Release verification follow-up: Android-host DI identity isolation

### Root cause and test policy

- The common singleton/alias identity test loaded the real production module and resolved the Android `PlaybackSessionStore`. Android-host tests do not execute `RhythHausApplication.onCreate()`, so the real Android factory correctly failed on its required uninitialized `LibraryDatabaseContext.applicationContext`.
- This test verifies Koin singleton and alias identity, not platform DataStore file construction. It now loads the real `rhythHausModule()` and a later test module with Koin overrides explicitly enabled, replacing only `PlaybackSessionStore` with the existing `EmptySessionStore`.
- The assertion now proves the resolved store is exactly `EmptySessionStore`; engine/controller/coordinator/reconciler/lifecycle production identity assertions remain unchanged. Cleanup still cancels the production process scope and stops Koin.

### Release RED/GREEN evidence

- RED:
  - Command: `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest.playbackSessionBindingsAreProcessSingletonsAndInterfaceAliases' --configuration-cache`
  - Result: test failed with `InstanceCreationException` caused by `UninitializedPropertyAccessException` while the real Android store factory accessed `LibraryDatabaseContext.applicationContext`.
- GREEN Android host:
  - Same command.
  - Result: `BUILD SUCCESSFUL in 4s`.
- GREEN JVM:
  - Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`
  - Result: `BUILD SUCCESSFUL in 6s`.

---

# Playlist Dialog Polish Task 6 Report

## Scope

- Added one JVM-only Compose UI test for the existing `HausDialog` dismiss semantics.
- Added only the two approved `jvmTest` dependencies.
- Did not modify production source, common tests, Android/iOS source sets, production dependencies, OpenSpec artifacts, progress, or roadmap files.

## RED

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache
```

Result: `BUILD FAILED` at `:shared:compileTestKotlinJvm`. The expected Compose UI-test harness APIs were unresolved, including `androidx.compose.ui.test.*`, `ExperimentalTestApi`, `runComposeUiTest`, `setContent`, `onNode`, and `waitForIdle`.

This was a meaningful harness/configuration RED before adding any test dependency.

## GREEN

The test uses `androidx.compose.ui.test.v2.runComposeUiTest` and matches the dialog with `SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)`. It performs `SemanticsActions.Dismiss`, waits for recomposition, verifies the callback changed `visible` to `false`, and verifies the dismiss semantics node no longer exists.

The first post-dependency compile showed that Compose UI test 1.11.1 exposes `keyIsDefined` through `SemanticsMatcher` and exposes `assertExists`/`assertDoesNotExist` as interaction members. The test was adjusted to those supported APIs without suppressions or deprecated test APIs.

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 2s`; 26 actionable tasks, 6 executed and 20 up-to-date; configuration cache reused.

## Final Verification

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 9s`; all 26 actionable tasks executed; configuration cache reused).
- `GIT_MASTER=1 git diff --check`: pass with no output.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` is not installed and installation was previously declined; the forced Gradle compile/test run is the executable Kotlin check.

## Exact Dependencies

Added only under `jvmTest.dependencies` in `shared/build.gradle.kts`:

```kotlin
implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
implementation(compose.desktop.currentOs)
```

## Files

- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogSemanticsJvmTest.kt`
- `shared/build.gradle.kts`
- `.superpowers/sdd/task-6-report.md`

## Remaining Blocker

- Runtime visual QA remains a separate unavailable-environment blocker and is not claimed by this executable semantics test.

## Exact-once Review Follow-up

- Strengthened the real `onDismiss` callback to increment `dismissCount` before retaining `visible = false`.
- After performing the real `SemanticsActions.Dismiss` action, the test now asserts `dismissCount == 1` while preserving the visibility and semantics-node disappearance assertions.
- Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache --rerun-tasks`
- Result: `BUILD SUCCESSFUL in 7s`; all 26 actionable tasks executed; configuration cache reused.

## Final Matrix After Test Harness Addition

- `openspec validate playlist-dialog-polish --strict`: pass (`Change 'playlist-dialog-polish' is valid`).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL in 1m 21s`).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 9s`).
- `/usr/bin/xcrun xcodebuild -version`: pass (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `GIT_MASTER=1 git diff --check`: pass with no output.

The desktop runtime capture/accessibility blocker remains unchanged and is not represented as visual acceptance.

## Final Reverification After Localized Dismiss Label Fix

- The final whole-change review found that Remove Folder had lost its former localized `dismiss(label = Cancel)` semantics action. `HausDialog` now accepts an optional `dismissLabel`, Remove Folder supplies the existing localized Cancel resource, and the JVM semantics test asserts the action label plus exactly-one callback and node removal.
- `./gradlew :shared:jvmTest --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 1m 26s`; 26/26 tasks executed).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 12s`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 28s`).
- `openspec validate playlist-dialog-polish --strict`: pass; `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, build 17F113; `GIT_MASTER=1 git diff --check`: pass.
- One earlier full JVM attempt failed only at unchanged `PlaybackControllerTest.repeatPlaylistWrapsCompletionAndManualTransport`. The exact test passed three forced isolated executions, CodeGraph found no call path from this dialog/UI change, and the final forced full JVM suite passed. No unrelated playback change was made.
