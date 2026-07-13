## 1. Source-scoped persistence lifecycle

- [x] 1.1 Add failing in-memory and SQLDelight tests for two independent sources and source-scoped removal.
- [x] 1.2 Add source-scoped SQL delete queries and `LibraryRepository.removeSource(sourceId)` transaction semantics.
- [x] 1.3 Verify repository tests and SQLDelight generation/compilation.

## 2. Platform capability and shared orchestration

- [x] 2.1 Add failing tests for first-source/additional-source action visibility and source-state refresh behavior.
- [x] 2.2 Add explicit additional-source capability to platform folder launchers: Android/JVM enabled, iOS disabled after its first source.
- [x] 2.3 Generalize `App()` scan launch for picker additions and source rescans, maintain accessible source state, and refresh sources/tracks after scan, removal, and clear.
- [x] 2.4 Verify focused shared tests and Android/JVM/iOS compilation.

## 3. Shared source management UI

- [x] 3.1 Add localized English and Chinese source-management copy.
- [x] 3.2 Show configured source rows in Settings with access/last-scan state and source-scoped rescan/remove actions.
- [x] 3.3 Add source-removal confirmation and disable source mutations while a scan is active.
- [x] 3.4 Preserve the empty-library add card and iOS first-source setup without exposing iOS additional-folder selection.
- [x] 3.5 Verify focused UI decision tests and shared/desktop/Android compilation.

## 4. Verification and durable handoff

- [x] 4.1 Run `openspec validate multi-library-folders --strict`.
  - Pass: `Change 'multi-library-folders' is valid`.
- [x] 4.2 Run focused repository/orchestration tests and the JVM/desktop/Android verification command.
  - Pass: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`, `BUILD SUCCESSFUL in 2s` (`34 actionable tasks: 5 executed, 29 up-to-date`).
  - Pass: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `BUILD SUCCESSFUL in 4s` (`108 actionable tasks: 5 executed, 103 up-to-date`).
- [x] 4.3 Verify Xcode availability and run iOS simulator tests.
  - Pass: `/usr/bin/xcrun xcodebuild -version` reported `Xcode 26.6` and `Build version 17F113`.
  - Blocked by pre-existing common-test incompatibility: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` failed in `:shared:compileTestKotlinIosSimulatorArm64` because `AppScanCancellationTest.kt:56` and `:99` reference JVM-only `Thread`; the same calls exist at base `f2eede0d60545bdbe00f1bad64cfe67df9a1f108`.
- [x] 4.4 Run `git diff --check` and review the complete scoped diff.
  - Pass: `git diff --check` returned no output; status and scoped planning/evidence diffs were reviewed with no production/test implementation changes after the Task 4 base.
- [x] 4.5 Update this task list, `progress.md`, and `roadmap.md` with exact evidence and remaining manual QA.
  - Recorded below and in the repository handoff; live Android/desktop multi-folder behavior and iOS app-local rescan remain manual QA, and no device or visual pass is claimed.
- [x] 4.6 Commit the completed OpenSpec + Superpowers workflow with semantic commit messages.
  - Commits succeeded: `f8621b9` (Superpowers plan), `d1d33cc` (OpenSpec artifacts), `2dcf856` (completion evidence), and `e507b30` (`docs: complete multiple library folders workflow`, final workflow state and schema marker).
