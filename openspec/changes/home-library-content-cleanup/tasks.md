## 1. Home manager-section removal

- [x] 1.1 Add failing home tests: import card hidden with tracks, scanning card hidden with tracks, import card visible when empty, scanning card visible only when empty and active.
- [x] 1.2 Remove `LibraryManagerCard`, `SourceManagerRow`, and their lazy item from `LibraryHomeContent`; remove now-unused parameters from `LibraryHomeContent` and both `LibraryAppShell` call sites; remove dead source-management strings from library-impl resources.
- [x] 1.3 Remove obsolete home manager/scan-outcome tests and helpers from `LibraryHomeContentJvmTest`.
- [x] 1.4 Run focused library home JVM tests and record RED/GREEN evidence.

## 2. Scan-outcome panel moved to Settings

- [x] 2.1 Make `ScanOutcomePanel` a public stateless composable in `:feature:library:impl` with KDoc and `@param` entries.
- [x] 2.2 Add a failing Settings test for the `scanOutcomeContent` slot rendering after `activeScanContent`.
- [x] 2.3 Add the `scanOutcomeContent` slot parameter to `SettingsScreen` and render it after the active-scan slot.
- [x] 2.4 Add failing route-adapter tests: outcome panel renders in Settings with summary/report/remove-missing callbacks, report expansion survives item recreation, report collapses on session change, disabled coordinator disables mutation controls.
- [x] 2.5 Wire the Shared route layer: report-expansion state keyed by session ID, source resolution, `onRemoveMissingTracks` parameter, and slot supply through `LibraryRouteOverlays`.
- [x] 2.6 Run focused Settings and route-adapter tests and record RED/GREEN evidence.

## 3. Verification and durable evidence

- [x] 3.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, architectureCheck, Spotless, Detekt, and `git diff --check`.
- [x] 3.2 Review the scoped diff for home, Settings, accessibility, scan-state, navigation, and callback regressions.
- [x] 3.3 Update `roadmap.md`, `progress.md`, and this task checklist with verification evidence and remaining manual visual-QA limits; commit with a semantic message.

## Verification Evidence

Task 4 ran every command on `main` at HEAD `dc1764c` (pre-Task-4), 2026-08-18, from the repo root. All commands exited 0; every claim below is the actual command output.

- `openspec validate home-library-content-cleanup --strict` → `Change 'home-library-content-cleanup' is valid` (exit 0).
- `./gradlew :shared:jvmTest --configuration-cache` → `BUILD SUCCESSFUL in 7s`; 133 actionable tasks (23 executed, 110 up-to-date), `Configuration cache entry reused`.
- `./gradlew :desktopApp:compileKotlin --configuration-cache` → `BUILD SUCCESSFUL in 1s`; 138 actionable (22 executed, 116 up-to-date), `Configuration cache entry stored`.
- `./gradlew :androidApp:assembleDebug --configuration-cache` → `BUILD SUCCESSFUL in 7s`; 307 actionable (41 executed, 266 up-to-date), `Configuration cache entry stored`.
- `/usr/bin/xcrun xcodebuild -version` → `Xcode 26.6` / `Build version 17F113` (exit 0; iOS toolchain available, so the iOS test leg was run, not recorded as a blocker).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` → `BUILD SUCCESSFUL in 1m 37s`; 180 actionable (47 executed, 133 up-to-date), `Configuration cache entry stored`.
- `./gradlew architectureCheck --configuration-cache` → `BUILD SUCCESSFUL in 2s`; 10 actionable (1 executed, 9 up-to-date).
- `./gradlew spotlessApply --configuration-cache` → `BUILD SUCCESSFUL in 15s`; 273 actionable (12 executed, 261 up-to-date). Applied formatting-only changes to 3 files (line-wrap + trailing-newline normalization in `LibraryHomeContent.kt`, `LibraryRows.kt`, `LibraryHomeContentJvmTest.kt`).
- `./gradlew spotlessCheck --configuration-cache` (separate command) → `BUILD SUCCESSFUL in 13s`; 273 actionable (2 executed, 271 up-to-date).
- `./gradlew detekt --configuration-cache` (separate command, independently proven) → `BUILD SUCCESSFUL in 2s`; 12 actionable, all up-to-date.
- `git diff --check` → clean (exit 0, no output).

Diff review (Task 4 Step 3): full `7a0ddb1..HEAD` scoped diff reviewed against every spec scenario and non-goal. Home hides all folder/scan/outcome UI when tracks exist (compact 420dp test `populatedLibraryHidesImportScanAndOutcomeUi`; wide branch shares the identical `LibraryHomeContent` gating); import card renders only when `tracks.isEmpty() && sourcePickerActionVisible`; scanning card only when `tracks.isEmpty() && scanProgress?.isActive == true`; terminal outcome, report toggle, rescan/retry, remove-missing, and lost-access recovery render in Settings via `LibraryRouteOverlays` (`reportVisible` keyed on `scanProgress?.session?.id`), with `settingsOutcomeReportSurvivesListItemRecreation`, `settingsOutcomeReportCollapsesWhenDisplayedSessionChanges`, `disabledCoordinatorDisablesSettingsOutcomeMutationControls`, `lostAccessSourceRecoveryLaunchesFolderPicker`, and `idleSessionDoesNotRenderSettingsOutcomePanel` covering the state machine. The 21-file scoped diff contains no dependency, database, scanner, playback-engine, navigation-model, or platform-integration change, and no Windows/Linux support. `spotlessApply`-only diffs are whitespace.

Remaining manual limits: no runtime desktop/Android/iOS visual QA, screen-reader pass, or physical-device scan/picker interaction was performed in this evidence task; those remain manual visual-QA follow-ups.
