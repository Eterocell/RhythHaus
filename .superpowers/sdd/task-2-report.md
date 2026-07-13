# Task 2 Report: Platform Additional-Source Capability and Shared Source Orchestration

## Scope

- Added explicit `PlatformFolderPickerLauncher.supportsAdditionalSources` capability.
- Added pure `sourcePickerActionVisible(supportsAdditionalSources, sourceCount)` decision helper.
- Added shared source/track refresh state and source access-status decoration in `App()`.
- Reused one guarded source-scan launcher for picker success and source rescan callbacks.
- Added background source removal and expanded clear-library refresh to update both sources and tracks.
- Threaded sources, picker visibility, rescan, and removal callbacks through `LibraryHomeScreen` and route overlays to `SettingsScreen`.
- Did not add source rows, dialogs, or new resources; those remain Task 3.

## TDD Evidence

### RED 1: capability visibility

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Expected and observed failure before production edits:

```text
Unresolved reference 'sourcePickerActionVisible'
BUILD FAILED
```

The failing tests covered:

- zero sources: picker visible regardless of capability;
- existing source + additional sources supported: picker visible;
- existing source + additional sources unsupported: picker hidden.

### GREEN 1

After adding the pure helper and platform capability values, the same focused command passed:

```text
BUILD SUCCESSFUL in 7s
```

### RED 2: shared source-state refresh

Added tests for current platform access-status decoration, source removal refreshing sources/tracks, and clear refreshing sources/tracks. Before orchestration production edits, the focused command failed with the expected missing API errors:

```text
Unresolved reference 'loadLibraryContent'
Unresolved reference 'LibraryContentState'
Unresolved reference 'removeSourceInBackground'
No parameter with name 'platformAccess' found
No parameter with name 'updateLibrary' found
BUILD FAILED
```

### GREEN 2

After the minimal orchestration implementation, the focused `LibrarySourceManagementTest` command passed:

```text
BUILD SUCCESSFUL in 6s
```

## Implementation Notes

- Android/JVM launchers return `supportsAdditionalSources = true`.
- iOS returns `supportsAdditionalSources = false`; `isAvailable` remains true.
- First-source visibility is exactly `supportsAdditionalSources || sourceCount == 0`.
- `App()` initializes `librarySources` from `repository.sources()` and copies `platformAccess.accessStatus(source)` onto each source.
- Picker success and `onRescanSource` share `launchSourceScan(source)`.
- Scan launch is ignored while current progress/job is active, preserving one active scan.
- Existing cooperative cancellation flag, live progress updates, terminal progress, and scan-complete message formatting remain intact.
- Terminal scan refresh reads both sources and tracks off Main, then updates Compose state on Main.
- Source removal runs `repository.removeSource(source.id)` on `Dispatchers.Default` and refreshes both lists afterward.
- Clear runs `repository.clearAll()` on `Dispatchers.Default` and refreshes both lists afterward.
- `onRemoveSource` is guarded while scan progress/job is active.
- Source data/callback parameters reach `SettingsScreen` for Task 3 consumption; no source-management rows/dialog/resources were implemented.

## Verification

Focused tests:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
BUILD SUCCESSFUL in 4s
```

Required compilation:

```text
./gradlew :shared:compileKotlinJvm :shared:compileKotlinIosSimulatorArm64 :androidApp:compileDebugKotlin --configuration-cache
BUILD SUCCESSFUL in 13s
```

Diff hygiene:

```text
git diff --check
pass (no output)
```

LSP diagnostics:

```text
Blocked: kotlin-ls is not installed and installation was previously declined.
```

Gradle compilation and focused tests supplied Kotlin compiler validation instead.

## Scope Review

- No dependency/toolchain/schema/playback changes.
- No Task 3 source rows/dialog/resources.
- No OpenSpec/spec/plan/progress/roadmap edits were made by this task.
- Existing unrelated planning files and `roadmap.md` changes remained unstaged.
- The only Task 1 persistence interaction is the existing `LibraryRepository.removeSource` API and test fake compatibility.

## Concerns

- `SettingsScreen` accepts `sources`, `onRescanSource`, and `onRemoveSource` before Task 3 renders/invokes them; Kotlin compilation permits the intentionally unused parameters.
- Live picker/rescan/removal behavior was not device-tested; automated shared tests and Android/JVM/iOS compilation passed.

## Final Review Race Fix

Root cause evidence confirmed that Settings derived mutation availability only from `scanProgress`, leaving a scan-start window where `scanJob` was active before progress reached Compose. Clear Library also remained enabled and `App.onClearLibrary` launched without checking either active signal, allowing the scanner and clear operation to write concurrently.

### RED

Added a pure shared decision test requiring mutations to be denied when either progress or job is active, including the job-only scan-start window.

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Observed before production edits:

```text
Unresolved reference 'sourceMutationsAllowed'
BUILD FAILED
```

### GREEN

Added `sourceMutationsAllowed(isProgressActive, isJobActive)` and used it consistently for:

- Settings add, rescan, remove, and clear controls;
- disabled Clear Library button state;
- `App()` scan-start, remove-source, and clear-library callback guards.

The job-active signal now closes the interval before active progress is published.

Focused gate test after implementation:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
BUILD SUCCESSFUL in 1s
```

Final requested verification:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
BUILD SUCCESSFUL in 831ms

./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
BUILD SUCCESSFUL in 4s

git diff --check
pass (no output)
```

Kotlin LSP diagnostics remained unavailable because `kotlin-ls` is not installed and installation was previously declined; the requested compiler/test commands passed.
