# Track Multi-Select Playlist Backup — Task 7 Report

## Scope

Implemented Task 7 only: common Compose playlist-backup document launcher outcomes and Android, JVM/macOS, and iOS document adapters. Settings workflow/orchestration remains Task 8. No backup codec, matcher, service, repository, database lifecycle, schema, dependency, toolchain, permission, OpenSpec task, `progress.md`, `roadmap.md`, or generic report was changed by this task.

## Contract

- Added `PlaylistBackupDocumentSaveResult`: `Success`, `Cancelled`, `Unavailable(message)`, and `Failure(message)`.
- Added `PlaylistBackupDocumentOpenResult`: `Success(bytes)`, `Cancelled`, `Unavailable(message)`, `TooLarge(maxBytes)`, and `Failure(message)`.
- Added `PlatformPlaylistBackupDocumentLauncher` and Compose `expect/actual rememberPlatformPlaylistBackupDocumentLauncher(onSaveResult, onOpenResult)`.
- Added the approved `.rhythhaus-playlists.json` extension, `application/vnd.rhythhaus.playlists+json` vendor MIME, JSON import fallback, and exact `4 * 1024 * 1024` byte limit.
- Suggested names are reduced to one safe path component, blank/dot names use `rhythhaus-playlists`, and the extension is appended only when absent, case-insensitively.
- Cancellation is a typed result and is not mapped to failure.

## Strict TDD Evidence

### Initial RED

Tests were added before production code:

- `shared/src/commonTest/.../PlatformPlaylistBackupDocumentsTest.kt`
- `shared/src/jvmTest/.../PlatformPlaylistBackupDocumentsJvmTest.kt`
- `shared/src/androidHostTest/.../PlatformPlaylistBackupDocumentsAndroidTest.kt`
- `shared/src/iosTest/.../PlatformPlaylistBackupDocumentsIosTest.kt`

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsJvmTest' :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest' :shared:compileTestKotlinIosSimulatorArm64 --configuration-cache
```

Result: expected `BUILD FAILED`; JVM, Android host, and iOS test compilation failed only on missing Task 7 symbols such as `playlistBackupFileName`, `PlaylistBackupDocument*Result`, platform adapter functions, and `IOSPlaylistBackupDocumentBridge`.

### First GREEN and host-test correction

The first implementation compiled on all targets. Android host tests initially failed before adapter execution because local JVM Android stubs throw from `Uri.parse`. The pure helper seam was changed to a generic injected selection token while production Activity Result callbacks remain real `Uri` values. Focused JVM, Android host, and actual iOS simulator tests then passed together (`BUILD SUCCESSFUL in 12s`).

### Review-fix RED/GREEN

Independent review found unsafe iOS suggested-path handling/cleanup, zero-progress stream loops, overlapping Android presentation state, and unavailable-presenter categorization. Regression tests were added first for safe single-component names, operation gating, zero-length stream reads, and iOS `UNAVAILABLE` mapping.

Review-fix RED failed only on the absent gate/status and inaccessible old readers. After the fixes, the forced focused matrix passed:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsJvmTest' :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest' :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsIosTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 23s`; 102/102 actionable tasks executed.

Covered behaviors include success, null selection/cancellation, unavailable resources/provider, exceptions, exact 4 MiB, 4 MiB+1 oversized, extension handling, path-component normalization, complete one-time writes, zero-length stream progress, operation overlap rejection, iOS status mapping, and provider retention.

## Platform Implementation

### Android

- Uses `ActivityResultContracts.CreateDocument(PlaylistBackupMimeType)` and `ActivityResultContracts.OpenDocument()`.
- Opens only `PlaylistBackupMimeType` or `application/json` imports.
- Uses `ContentResolver.openOutputStream(uri, "wt")` and `openInputStream(uri)` with `use` cleanup.
- Reads at most 4 MiB + 1 and handles zero-length stream reads without hanging.
- Uses one remembered operation gate and one pending save payload; overlapping save/open launches return `Unavailable`, synchronous launch failures clear ownership, and callbacks release the gate.
- Does not call `takePersistableUriPermission`, add storage permissions, or retain the selected import URI.

### JVM/macOS

- Uses separate native AWT `FileDialog.SAVE` and `FileDialog.LOAD` panels.
- Has injected file-selection and read/write seams for pure tests.
- Writes the complete payload once and appends the approved extension only when absent.
- Reads at most 4 MiB + 1 and handles zero-length reads with progress.
- Temporarily clears `apple.awt.fileDialogForDirectories` and restores its exact previous value in `finally`; dialogs are disposed in `finally`.
- Existing `openNativeFolderDialog` in `PlatformSourceAccess.jvm.kt` was not modified.

### iOS Kotlin bridge

- Defines `IOSPlaylistBackupDocumentProvider`, `IOSPlaylistBackupDocumentCompletion`, `IOSPlaylistBackupDocumentBridge`, and explicit success/cancelled/too-large/failure/unavailable status values.
- A missing provider returns `Unavailable`; provider statuses map to distinct common outcomes.
- Uses `rememberUpdatedState` for asynchronous Compose callbacks.

The framework was compiled before Swift implementation:

```text
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --configuration-cache
```

Result: `BUILD SUCCESSFUL in 10s`. Generated `Shared.h` confirmed exact Swift spellings:

- `IOSPlaylistBackupDocumentProvider`
- `IOSPlaylistBackupDocumentCompletion.complete(status:bytes:message:)`
- `IOSPlaylistBackupDocumentBridge.shared.provider`
- `IOSPlaylistBackupDocumentStatus.shared.SUCCESS/CANCELLED/TOO_LARGE/FAILURE/UNAVAILABLE`
- `openDocument(maxBytes:completion:)`
- `saveDocument(fileName:bytes:completion:)`
- `KotlinByteArray(size:)`, `get(index:)`, and `set(index:value:)`

### iOS Swift provider

- `RhythHausPlaylistBackupDocumentProvider` retains the active picker, delegate ownership, completion, operation type, and export temp state.
- App bootstrap retains/registers the provider before Compose starts; `ContentView` registers the Compose controller through a weak presenter registry.
- Uses `UIDocumentPickerViewController(forExporting:asCopy:)` for save and `forOpeningContentTypes:asCopy:` for open.
- Uses vendor `UTType` with `.json` fallback.
- Exports to a UUID-owned cache directory, reduces the bridge name to `lastPathComponent`, writes atomically, and deletes exactly the stored UUID directory on success, cancellation, failure, unavailable presenter, or deallocation.
- Rejects overlapping operations and ignores stale delegate callbacks whose picker identity no longer matches.
- Imports with `FileHandle.read(upToCount: maxBytes + 1)`, closes the handle, returns `TooLarge` beyond the limit, and balances `startAccessingSecurityScopedResource`/`stopAccessingSecurityScopedResource` with `defer`.
- Does not retain the selected import URL.

## Verification

### Focused JVM

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsJvmTest' --configuration-cache --rerun-tasks
```

Pass: `BUILD SUCCESSFUL in 11s`; 35/35 tasks executed.

### Focused Android host

```text
./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest' --configuration-cache --rerun-tasks
```

Pass: `BUILD SUCCESSFUL in 9s`; 61/61 tasks executed.

### iOS simulator tests

The forced complete suite compiled and ran 425 tests, but failed 1 unchanged playback timing test:

```text
com.eterocell.rhythhaus.PlaybackControllerTest.autoAdvanceRemainsLoadingUntilEngineReportsPlaying
```

No complete-suite pass is claimed. The Task 7 iOS bridge suite passed forced in isolation (`BUILD SUCCESSFUL in 25s`), and the unchanged playback test passed forced in isolation immediately afterward (`BUILD SUCCESSFUL in 23s`), consistent with the documented intermittent playback-test context.

The final forced cross-platform Task 7 focused matrix also passed as recorded above.

### Generated framework and Swift/Kotlin bridge

Initial and post-hardening framework links passed. The clean unsigned generic-simulator build passed after the final changes:

```text
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO clean build
```

Result: `** BUILD SUCCEEDED **`. This proves real Swift sources compile against the generated Kotlin framework; it does not claim runtime picker presentation.

### JVM/desktop/Android compile

```text
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Pass: initial `BUILD SUCCESSFUL in 6s`; final post-review run `BUILD SUCCESSFUL in 2s`.

### Diagnostics and hygiene

- Kotlin LSP was unavailable because `kotlin-ls` was previously declined; Gradle target compilation/tests are the executable diagnostics.
- Standalone SourceKit could not resolve generated module `Shared`; full Xcode compilation passed and is authoritative.
- `GIT_MASTER=1 git diff --check`: pass.
- Source audit found no `rhythhaus.db`, raw database lifecycle, `readBytes()`, persistable URI permission, checkpoint, migration, or replacement calls in Task 7 adapters.

## Review

Initial independent requirements/code review and security review failed on the issues addressed in the review-fix TDD cycle. Two fresh narrow re-reviews then passed:

- Requirements/code blocker recheck: PASS; no remaining Critical or Important findings.
- Security re-audit: PASS; no remaining BLOCKER or HIGH findings.

Neither fake adapter tests nor build evidence is represented as UIKit/SAF/AWT runtime presentation. Manual system-panel presentation remains Task 9/runtime QA evidence.

## Commits

- `e2dad36 feat: add playlist backup document contract`
- `a7fc585 feat: add JVM playlist backup documents`
- `45a63cb feat: add Android playlist backup documents`
- `aa75e13 feat: add iOS playlist backup document bridge`
- `786101e feat: add iOS playlist backup document picker`

Each commit includes the required Sisyphus footer and co-author. Only explicit Task 7 paths were staged. Controller-owned `.superpowers/sdd/progress.md`, generic Task 1/2 reports, and `openspec/changes/track-multi-select-playlist-backup/tasks.md` remain untouched by Task 7 staging.

## Blockers and Deferrals

- Task 7 implementation/focused verification: no blocker.
- Complete forced iOS suite: one unrelated intermittent playback test failure; isolated rerun passed, so the report records the full-gate failure honestly rather than claiming a pass.
- Runtime system-panel interaction on Android/iOS/JVM: not exercised and not claimed; pure adapters and compilation do not prove actual UI presentation.
- Task 8 Settings orchestration and Task 9 integration/runtime QA remain deliberately unimplemented.
