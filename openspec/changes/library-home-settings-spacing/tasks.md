## 1. Settings compact layout policy

- [x] 1.1 Add a failing common test for the exact 16 dp horizontal, 8 dp vertical, 12 dp item, and 8 dp bottom spacing policy.
- [x] 1.2 Add the internal immutable Settings layout policy and apply it to `SettingsScreen` while retaining `safeContentPadding()` and existing controls.
- [x] 1.3 Run the focused layout-policy and source-management tests and record RED/GREEN evidence.

## 2. Library home chrome removal

- [x] 2.1 Add or adjust common coverage for the retained Library home safe-top-content policy before changing production UI.
- [x] 2.2 Remove the Library home nested top-bar state and overlay without changing its header, content, or Now Playing scroll behavior.
- [x] 2.3 Remove dead home-only nested-chrome declarations and obsolete progression tests while preserving drill-down chrome helpers.
- [x] 2.4 Run focused Library navigation tests and compile the shared JVM target.

## 3. Verification and durable evidence

- [x] 3.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, and `git diff --check`.
- [x] 3.2 Review the scoped diff for safe-area, accessibility, drill-down, navigation, playback, scanning, and source-management regressions.
- [x] 3.3 Update `roadmap.md`, `progress.md`, and this task checklist with verification evidence and remaining manual visual-QA limits.

## Verification Evidence

- `openspec validate library-home-settings-spacing --strict`: pass (`Change 'library-home-settings-spacing' is valid`).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL in 21s`; 34 actionable tasks: 5 executed, 29 up-to-date).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`; 94 actionable tasks: 3 executed, 91 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked by pre-existing common-test compilation errors at `AppScanCancellationTest.kt:56:28` and `:99:27` (`Unresolved reference 'Thread'`; `BUILD FAILED in 1s`).
- `git diff --check`: pass.
- Task 1 and Task 2 independent reviews: spec compliant and quality approved with no findings.
- Two source-level visual QA Oracle passes: PASS with MEDIUM confidence; runtime screenshots unavailable because the desktop run exited before capture and the Orca runtime was not running.
- Final whole-branch Oracle review: PASS with no Critical, Important, or Minor findings.
