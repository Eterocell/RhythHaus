# iOS Files Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let iOS users import audio files or folders from Files.app into RhythHaus-managed app storage, while automatically registering and scanning the default `Documents/RhythHaus` source without self-copying its existing files.

**Architecture:** Preserve the common `PlatformFolderPickerLauncher` application seam and the existing `IosAppLocal` source identity. Add a narrow retained iOS provider ABI: Swift owns `UIDocumentPickerViewController`, security-scoped access, recursive enumeration, and atomic copying; Kotlin owns result mapping, Compose/orchestration state, source normalization, and the existing scan coordinator. No external URL is persisted and no database schema changes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Kotlin common/JVM tests, Swift/UIKit, Foundation, `UniformTypeIdentifiers`, existing SQLDelight library persistence and scanner.

**Spec:** `openspec/changes/ios-files-import/specs/ios-files-import/spec.md` and `openspec/changes/ios-files-import/specs/local-library-scanning/spec.md`

## Global Constraints

- Preserve `LibraryPlatformKind.IosAppLocal`, source handle, scanner, playback, and existing Android/JVM picker behavior.
- External security-scoped URLs are temporary import inputs only; never persist them as playable handles.
- Import copies only user-selected local documents; no network, MusicKit, cloud sync, tag writing, or transcoding.
- Cancellation is a distinct no-error terminal outcome and must not start a scan or mutate the database.
- Import operations are single-flight, cleanup is idempotent, and stale picker callbacks cannot finish a newer operation.
- Copy through a temporary destination and atomic rename; never overwrite a different-content managed file.
- `Documents/RhythHaus` is the existing app-local source root, not a child import directory; register it only when `ios-app-local` is absent, then scan it through the existing App-owned coordinator.
- A Files.app selection inside the managed directory is scan-only: no security-scoped copy, duplicate file, suffix, or imported count.
- SQLDelight schema and migration files are unchanged.
- Use TDD for production behavior: write and run a failing behavioral test before implementation, then run the focused test green.
- Run Spotless apply, Spotless check, and Detekt as separate commands before completion; do not infer iOS runtime behavior from compilation.

---

### Task 1: Shared iOS import ABI and pure destination policy

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- Create or modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/IOSLibraryImport.kt` (or the existing iOS bridge location selected by current source ownership)
- Modify: `feature/library/impl/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.ios.kt`
- Test: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccessTest.kt` or the nearest existing focused contract test

**Interfaces:**
- Produces `IOSLibraryImportProvider.importAudio(destinationPath: String, completion: IOSLibraryImportCompletion)`.
- Produces `IOSLibraryImportCompletion.complete(status: Int, imported: Int32, duplicates: Int32, unsupported: Int32, failed: Int32, message: String?)`.
- Produces retained `IOSLibraryImportBridge.provider` injection and stable status constants for success, cancelled, unavailable, overlap, and failure.
- Consumes existing `PlatformFolderPickResult`, `PlatformFolderPickerLauncher`, and `appLocalMusicFolderPath()`.

- [ ] **Step 1: Write failing tests for status mapping and destination policy**

  Cover: success and duplicate-only success return the existing `IosAppLocal` source; cancellation returns no source and no error; unavailable/overlap/failure return recoverable failures; filename sanitization removes path separators and traversal; same-content destination is duplicate; different content selects a stable suffixed destination.

- [ ] **Step 2: Run focused tests and confirm the expected RED state**

  Run: `./gradlew :feature:library:impl:jvmTest --tests '*PlatformSourceAccess*' --rerun-tasks`

  Expected: the new contract/policy symbols or behavior are missing, so the test fails for the intended reason rather than a compilation typo.

- [ ] **Step 3: Implement the narrow ABI, mapping, and pure policy**

  Keep the ABI primitive-only and Swift-compatible. Keep destination policy pure and platform-independent where possible. The iOS launcher should call the provider with the managed path, map terminal statuses, and only return the existing local source for success or duplicate-only completion.

- [ ] **Step 4: Run focused tests green**

  Run the same focused JVM test command. Expected: all new policy and mapping behaviors pass, with no Android/JVM source behavior changes.

---

### Task 2: Kotlin orchestration and user-visible import result

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` only if active import state needs a callback/state parameter
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt` only for copy/import explanation and terminal summary
- Test: existing Shared/App and library UI state tests under `shared/src/commonTest` and `feature/library/impl/src/commonTest`

**Interfaces:**
- Consumes the Task 1 iOS launcher and its aggregate import result.
- Produces the existing source-scan request only after successful import completion.
- Preserves current source mutation gating and existing `importMessage` behavior for Android/JVM.

- [ ] **Step 1: Write failing orchestration tests**

  Prove cancellation and failure do not invoke scan; success invokes scan once with the normalized `IosAppLocal` source; duplicate-only success still scans; active import blocks competing source mutation; the terminal message includes imported/duplicate/unsupported/failed counts.

- [ ] **Step 2: Run the focused tests and confirm RED**

  Run the exact selected Shared/library test classes with `--rerun-tasks`. Expected: the new import-result behavior is absent.

- [ ] **Step 3: Implement minimal state propagation**

  Keep import and scan as sequential phases. Do not add a second coordinator or database state machine. Use the existing App-owned source-operation admission and transient message surface unless a focused test proves that a small callback extension is required.

- [ ] **Step 4: Run focused Shared/library tests green**

  Re-run the exact tests and confirm cancellation, failure, success, duplicate-only success, gating, and summary behavior.

---

### Task 3: Swift Files.app picker and managed copy

**Files:**
- Create: `iosApp/iosApp/Documents/RhythHausLibraryImportProvider.swift`
- Create: `iosApp/iosApp/Documents/LibraryImportPolicies.swift` for pure filename/content/destination policy if that ownership fits the existing document-provider tests
- Modify: `iosApp/iosApp/App/RhythHausAppBootstrapper.swift`
- Modify: `iosApp/iosApp/iOSApp.swift`
- Test: `iosApp/iosAppTests/Documents/LibraryImportPoliciesTests.swift` and provider operation-policy tests in the existing iOS host test target

**Interfaces:**
- Consumes `IOSLibraryImportProvider` and `IOSLibraryImportCompletion` from the generated Shared framework.
- Produces a single-flight `UIDocumentPickerViewController` for multiple `UTType.audio` and `UTType.folder` selections.
- Copies into the destination supplied by Kotlin; never returns selected URLs to Kotlin.

- [ ] **Step 1: Write failing pure Swift policy tests**

  Cover filename sanitization, nested-directory enumeration input classification, same-content duplicate detection, deterministic numeric collision suffixes, unsupported extension filtering, and aggregate count behavior.

- [ ] **Step 2: Run the iOS host policy tests and confirm RED**

  Run the focused `xcodebuild test` invocation for the iOS host test scheme. Expected: the new policy types or behaviors are absent.

- [ ] **Step 3: Implement picker lifecycle and copy policy**

  Present the picker from `RhythHausViewControllerRegistry.presenter`; set multiple selection; handle cancellation and stale delegate callbacks; call `startAccessingSecurityScopedResource()` per selected URL and balance it with `stopAccessingSecurityScopedResource()`; recursively enumerate directories; filter supported audio extensions; copy through a temporary file and atomic move; compare existing destination bytes before reporting duplicate or choosing a suffix; finish exactly once on the main queue.

- [ ] **Step 4: Register the provider before Compose startup**

  Add one retained provider property to `iOSApp`, pass it through `RhythHausAppBootstrapper.configure`, and assign it in the existing bridge-registration function. Keep playlist, artwork, and audio provider registration unchanged.

- [ ] **Step 5: Run focused Swift tests and iOS host compilation green**

  Re-run the pure policy/provider tests and the iOS host compilation. Expected: all policy tests pass and the generated Shared ABI resolves without duplicate symbols.

---

### Task 4: Import copy, onboarding text, and regression verification

**Files:**
- Modify: existing iOS visible Documents marker text in `iosApp/iosApp/App/RhythHausAppBootstrapper.swift`
- Modify: the owning Compose resource XML for the import card and import result messages
- Test: resource ownership/localization tests and existing library UI tests
- Modify: `openspec/changes/ios-files-import/tasks.md`, `progress.md`, and `roadmap.md` after verification only

- [ ] **Step 1: Write failing copy/onboarding assertions**

  Assert that the visible iOS instruction explains Files.app selection and that selected audio is copied into RhythHaus storage; assert cancellation remains silent while copy failures produce a recoverable message.

- [ ] **Step 2: Implement localized copy and result summary**

  Use existing English/Chinese resource conventions. Do not claim persistent external-file access. Ensure all user-facing count labels distinguish imported, duplicate, unsupported, and failed items.

- [ ] **Step 3: Run the focused regression tests**

  Run the selected resource, library, and Shared tests. Expected: existing Android/JVM picker and scan behavior remains green.

---

### Task 5: Default iOS source and managed-directory selection

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPickerFacade.android.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPickerFacade.jvm.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.ios.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/IOSLibraryImport.kt`
- Modify: `iosApp/iosApp/Documents/LibraryImportPolicies.swift`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppLibraryImportTest.kt`
- Test: `iosApp/iosAppTests/Documents/LibraryImportPoliciesTests.swift`

**Interfaces:**
- Produces `defaultPlatformLibrarySource(): LibrarySource?`; iOS returns the stable `ios-app-local` source rooted at `Documents/RhythHaus`; Android/JVM return `null`.
- Adds a stable `already-managed` terminal status to `IOSLibraryImportStatus` and maps it to `PlatformFolderPickResult.Success` even with zero copy counts.
- Consumes the existing App source coordinator and `LibraryImportCopyRunner`.

- [ ] **Step 1: Write failing behavioral tests**

Kotlin: prove a missing default iOS source is persisted once and selected for the initial scan, while an existing `ios-app-local` source is untouched. Prove already-managed status yields scan success with no summary error.

Swift: prove the managed root, a descendant folder, and an audio file beneath the managed root are excluded from a copy run; a sibling path with the same prefix is not excluded.

- [ ] **Step 2: Run focused tests and confirm RED**

Run the selected Shared JVM test methods and focused `LibraryImportPoliciesTests` XCTest invocation. Expected: missing default-source and already-managed symbols/behavior fail for the intended assertions.

- [ ] **Step 3: Implement the minimum lifecycle and bypass**

Provide actual default-source values through the existing Shared platform facade. In `App`, seed only a missing default source before initial publication and launch its scan through the existing coordinator after that publication. In Swift, classify paths inside the managed root before security-scoped access or recursive enumeration. Return already-managed only when no external file was copied or recognized as a duplicate; mixed selections retain normal external-import counts and completion.

- [ ] **Step 4: Run focused tests green**

Re-run the exact Kotlin and Swift selections. Expected: the source is registered/scanned exactly once, managed selections copy nothing but scan, and external sibling selections remain importable.

### Task 6: Full verification and OpenSpec acceptance

**Files:**
- Modify: `openspec/changes/ios-files-import/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

- [ ] **Step 1: Run quality and platform checks separately**

  Run:

  ```bash
  ./gradlew spotlessApply --configuration-cache
  ./gradlew spotlessCheck --configuration-cache
  ./gradlew detekt --configuration-cache
  ./gradlew :shared:jvmTest --configuration-cache
  ./gradlew :feature:library:impl:jvmTest --configuration-cache
  ./gradlew :desktopApp:compileKotlin --configuration-cache
  ./gradlew :androidApp:assembleDebug --configuration-cache
  /usr/bin/xcrun xcodebuild -version
  ./gradlew :shared:iosSimulatorArm64Test --configuration-cache
  openspec validate ios-files-import --strict
  git diff --check
  ```

- [ ] **Step 2: Exercise available iOS runtime behavior**

  Verify Files.app file selection, folder recursion, cancellation, repeated import, scan, restart, and playback from copied files on the available simulator/device. Record unavailable physical-device evidence explicitly rather than inferring it from compilation.

- [ ] **Step 3: Review the final diff and close the change**

  Confirm only the approved iOS import/common seam, tests, resources, OpenSpec artifacts, and required handoff records changed. Mark OpenSpec tasks complete only when command output and reviewed-diff evidence exist; commit with `feat: import iOS music from Files` after acceptance.
