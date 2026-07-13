# Final security review fixes: multi-library folders

## Scope

Base HEAD: `4812308` (`docs: finalize source mutation race commit evidence`)

Implemented only the final review findings requested for `multi-library-folders`:

- empty-library Add Folder mutation gating;
- Android SAF least-privilege persisted permission;
- collision-resistant deterministic Android source identities with existing-source compatibility;
- privacy-safe picker errors and unnamed source labels.

No schema, dependency, iOS capability, source-removal design, OpenSpec, `progress.md`, or `roadmap.md` changes were made. The pre-existing dirty `.superpowers/sdd/task-2-report.md` was not edited or staged.

## Root-cause investigation

1. `LibraryHomeContent` passed `scanProgress?.isActive != true` to the empty import card, unlike Settings and App callbacks, so the job-active/progress-not-yet-published startup window left Add Folder enabled.
2. Android first requested persisted read and write access, only falling back to read. Scanning opens documents read-only, so write permission was unnecessary.
3. Android source IDs used Java/Kotlin 32-bit `String.hashCode()`, allowing distinct URI handles to collide. Picker success was scanned directly, so changing the ID algorithm would fail to reuse an already persisted source created under the legacy ID.
4. Picker failures carried throwable messages/class names and `App` concatenated them into visible UI text.
5. Blank source display names fell back to the raw source handle in configured rows, action accessibility descriptions, and removal-dialog semantics.

## RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Expected failure: `BUILD FAILED in 1s` during `:shared:compileTestKotlinJvm` because the regression tests referenced missing behavior:

- unresolved `androidSafSourceId`;
- unresolved `emptyLibrarySourceMutationsAllowed`;
- unresolved `normalizePickedSource`;
- missing `sourceDialogName(..., unnamedLabel = ...)` parameter.

The tests cover:

- job-active-only empty-card mutation blocking;
- reuse of persisted `id` and `createdAtEpochMillis` when picker handle matches;
- retention of a new identity when handles differ;
- distinct identities for known Java hash-collision URI strings ending in `Aa` and `BB`;
- neutral unnamed-folder labeling without exposing the handle.

## Implementation

- Added common pure `normalizePickedSource` and applied it to picker success against current `librarySources` before scan launch.
- Added deterministic full-stable-URI hex encoding for new Android SAF source IDs, avoiding 32-bit hash collisions and unsafe raw URI characters in IDs.
- Reused persisted ID and creation time for matching handles, preserving legacy source compatibility.
- Changed Android persisted access to `FLAG_GRANT_READ_URI_PERMISSION` only.
- Routed the empty import card through the shared mutation gate using both progress and job activity.
- Stopped platform pickers from attaching throwable details and stopped `App` from rendering failure causes.
- Added localized `unnamed_folder` strings in English and Chinese and used the neutral label for visible and accessibility source names.

## GREEN and focused verification

Focused regression test:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 4s`; 25 actionable tasks, 14 executed, 11 up-to-date.

Source-management, scan-cancellation, and scanner suites:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' \
  --configuration-cache
```

Result: `BUILD SUCCESSFUL in 2s`; 34 actionable tasks, 5 executed, 29 up-to-date.

## Build verification

Command:

```bash
./gradlew :shared:compileKotlinJvm :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache
```

Result: `BUILD SUCCESSFUL in 6s`; 103 actionable tasks, 15 executed, 88 up-to-date. The only source warning was the existing Android `MediaMetadata.Builder.setArtworkData` deprecation.

Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation covered common, Android, JVM, and desktop changed code.

## Remaining concerns

- Android SAF permission and picker behavior were compile/test verified but not exercised on a physical device or emulator in this session.
- The new Android ID is longer than the previous hash ID by design; it remains deterministic and collision-free for distinct UTF-8 URI strings without adding dependencies.
- Existing persisted sources remain compatible only through handle normalization before scanning, as required; no migration is performed.
