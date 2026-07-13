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

## Follow-up: JVM folder source ID collision fix

### Scope and root cause

Base HEAD: `564822b` (`docs: record final security fix evidence`).

The JVM folder picker still generated IDs from `canonicalPath.hashCode()`. Kotlin/JVM `String.hashCode()` is a 32-bit Java hash, so distinct canonical paths can produce the same persisted source ID. This was isolated to new JVM folder picks: `normalizePickedSource` already compares exact handles and restores the existing persisted `id` and `createdAtEpochMillis`, so compatibility required keeping that normalization flow unchanged rather than migrating stored rows.

No schema, dependency, iOS, OpenSpec, `progress.md`, or `roadmap.md` changes were made. `.superpowers/sdd/task-2-report.md` was not touched.

### RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Result: `BUILD FAILED in 1s` during `:shared:compileTestKotlinJvm` with unresolved `jvmFolderSourceId` references at the new regression assertions.

The RED regression proves:

- canonical-like paths `/music/Aa` and `/music/BB` have the same legacy Java/Kotlin hash;
- the required new JVM identity helper must return distinct IDs for those paths;
- re-picking an exact JVM handle must retain its persisted legacy ID and original creation time through `normalizePickedSource`.

### Implementation

- Added common pure `jvmFolderSourceId(stableCanonicalPath)`, encoding every UTF-8 path byte as lowercase hexadecimal after the `jvm-folder-path-` prefix.
- Changed `File.toJvmFolderSource()` to pass its full `canonicalPath` to the helper instead of truncating identity to a 32-bit hash.
- Left exact-handle normalization unchanged, preserving existing stored IDs and `createdAtEpochMillis` when a folder is picked again.

### GREEN evidence

Focused command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 4s`; 25 actionable tasks, 8 executed, 17 up-to-date.

Required regression suites:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' \
  --configuration-cache
```

Result: `BUILD SUCCESSFUL in 1s`; 25 actionable tasks, 5 executed, 20 up-to-date.

JVM/desktop/Android verification and diff check:

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
GIT_MASTER=1 git diff --check
```

Result: Gradle `BUILD SUCCESSFUL in 4s`; 94 actionable tasks, 10 executed, 84 up-to-date. `git diff --check` produced no output. The only compiler warning was the existing Android `MediaMetadata.Builder.setArtworkData` deprecation.

Kotlin LSP diagnostics were requested for all three changed Kotlin files, but `kotlin-ls` is not installed and installation was previously declined. Successful common/JVM test compilation plus JVM, desktop, and Android builds provide the available compiler validation.

### Remaining concerns

- New JVM IDs are longer than legacy hash IDs because they losslessly encode the full canonical path; this is intentional and dependency-free.
- The encoded ID does not expose a raw path in UI. It remains internal persistence identity, while existing display-name behavior is unchanged.
- Persisted legacy IDs are retained only when the exact canonical handle matches, which is the existing required normalization contract; no migration or fuzzy path matching was added.

## Follow-up: source-access lifecycle, transactional clear, and cancellation cleanup

### Scope and root cause

Base HEAD: `f08e810` (`docs: record JVM source ID collision fix evidence`).

Implemented only the three requested direct multi-library lifecycle/cancellation findings:

1. `removeSourceInBackground` and `clearLibraryInBackground` deleted persisted library state but never released Android's persisted SAF read grant.
2. SQLDelight `clearAll()` issued four independent deletes and deleted parents before some children, so a later failure could leave a partially cleared repository and foreign-key enforcement could reject the order.
3. `LibraryScanner` checked cooperative cancellation after obtaining each scan event. When that event was an `AudioCandidate`, Android had already opened its metadata descriptor, but cancellation returned before `toLibraryTrack()` reached its existing `finally` cleanup.

No symlink traversal, source-local-key normalization, terminal-message sanitization, schema, dependency, UI, iOS expansion, OpenSpec, `progress.md`, or `roadmap.md` changes were made.

### RED evidence

Initial focused command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' \
  --configuration-cache
```

First RED result: `BUILD FAILED in 1s` during test compilation because `FakePlatformSourceAccess.releaseAccess` overrode no method, proving the platform release seam was absent.

After adding only the default no-op seam so behavioral tests could compile, the same focused command failed three regressions:

- `sourceRemovalReleasesAccessOnlyAfterRepositoryDeletion`;
- `clearLibraryReleasesEverySnapshottedSourceAfterRepositoryClear`;
- `cancellationBeforeCandidateImportCleansUpMetadataAudioSource`.

Transactional RED command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.clearAllRollsBackEveryTableWhenSourceDeletionFails' \
  --configuration-cache
```

Result: `BUILD FAILED in 2s`; the source-delete trigger raised as intended, but the earlier child deletes remained committed, proving `clearAll()` was not atomic.

### Implementation

- Added `PlatformSourceAccess.releaseAccess(source)` with a default no-op, preserving JVM/iOS behavior without new actual implementations.
- Android releases only `AndroidSafTree` persisted read access through `releasePersistableUriPermission(Uri.parse(handle), FLAG_GRANT_READ_URI_PERMISSION)` inside `runCatching`, so stale/missing grants do not turn a successful repository mutation into a failed UI operation.
- `removeSourceInBackground` snapshots the matching source, successfully removes repository data, then releases access and refreshes content. Failed removal does not release access.
- `clearLibraryInBackground` snapshots all sources, successfully clears the repository, then releases each removed source and refreshes content. Failed clear does not release access.
- SQLDelight `clearAll()` now uses one database transaction and child-first order: scan errors, scan sessions, tracks, sources.
- `LibraryScanner` invokes an `AudioCandidate`'s metadata cleanup before returning from the pre-import cancellation branch. Imported candidates still use the existing `toLibraryTrack()` `finally`, so each path closes exactly once.

### GREEN evidence

Focused source-management, cancellation, scanner, and repository suites:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' \
  --configuration-cache
```

Result: `BUILD SUCCESSFUL in 4s`; 34 actionable tasks, 7 executed, 27 up-to-date.

JVM tests plus desktop and Android build:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: `BUILD SUCCESSFUL in 3s`; 99 actionable tasks, 12 executed, 87 up-to-date. The only compiler warning was the existing Android `MediaMetadata.Builder.setArtworkData` deprecation.

Diff validation:

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output.

Kotlin LSP diagnostics were requested for every changed Kotlin file, but `kotlin-ls` is not installed and installation was previously declined. The focused tests and common/JVM/Android/desktop compilation provide the available compiler validation.

### Remaining concerns

- Persisted Android SAF grant release was compile-verified but not exercised on a device/emulator.
- Android release intentionally suppresses platform exceptions after a successful repository mutation; retrying or surfacing stale-grant cleanup is outside this scoped task.
- iOS and JVM inherit the default no-op release seam as requested.
