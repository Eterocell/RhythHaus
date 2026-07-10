# Task 1 Report: Source-Scoped Repository Removal

## Status

DONE

## Commit

- `0bc1881e00f589a4db0db747929430d13de47f13` — `feat: add source-scoped repository removal`

## Implementation

- Added `LibraryRepository.removeSource(sourceId)`.
- Implemented in-memory removal by resolving the source's scan IDs first, then removing dependent errors, sessions, tracks, and the source.
- Added query-only SQLDelight deletes for source-scoped errors, sessions, tracks, and source rows; no schema shape or migration changes.
- Implemented SQLDelight removal inside one `database.transaction`, deleting dependent rows before the source.
- Updated the required `ThreadCapturingRepository` test fake for interface compilation.
- Added `removeSourceDeletesOnlySelectedSourceData`, persisting two independent sources with one track/session/error each and proving removal preserves the second source and its data.

## Strict RED-GREEN Evidence

### RED

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.removeSourceDeletesOnlySelectedSourceData' --configuration-cache
```

Expected failure observed before production edits:

```text
e: .../SqlDelightLibraryRepositoryJvmTest.kt:129:29 Unresolved reference 'removeSource'.
FAILURE: Build failed with an exception.
BUILD FAILED in 1s
```

### GREEN

Focused method:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.removeSourceDeletesOnlySelectedSourceData' --configuration-cache
BUILD SUCCESSFUL in 8s
```

Focused repository test class:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache
BUILD SUCCESSFUL in 992ms
```

SQLDelight generation ran as part of the focused GREEN command (`:shared:generateCommonMainRhythHausDatabaseInterface`) and completed successfully.

## Review and Scope

- `git diff --check`: pass.
- Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined; Gradle Kotlin compilation and focused tests passed instead.
- Reviewed the scoped diff before commit.
- Commit contains only the eight Task 1 implementation/test files.
- Pre-existing `roadmap.md`, Superpowers planning docs, and OpenSpec artifacts were not staged or edited by this task.

## Concerns

- None for Task 1 behavior or focused JVM verification.
- The report is intentionally written after the atomic implementation commit so it can record the final commit hash; it is not included in that implementation commit.

## Review Fix: Observable Scan Session Removal

### Fix Summary

- Added the narrow query-only SQLDelight `selectScanSessionById` query so the repository regression test can observe persisted scan-session rows directly.
- Extended `removeSourceDeletesOnlySelectedSourceData` to assert `scan-1` is absent and `scan-2` remains after removing `source-1`.
- Updated the JVM `testTrack` helper to accept `lastSeenScanId`; the source-1 track now references `scan-1`, and the source-2 track references `scan-2`.
- No schema shape or migration changes were made.

### Review Fix RED

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.removeSourceDeletesOnlySelectedSourceData' --configuration-cache
```

Output before adding the observation query:

```text
e: .../SqlDelightLibraryRepositoryJvmTest.kt:133:65 Unresolved reference 'selectScanSessionById'.
e: .../SqlDelightLibraryRepositoryJvmTest.kt:134:69 Unresolved reference 'selectScanSessionById'.
FAILURE: Build failed with an exception.
BUILD FAILED in 1s
```

### Review Fix GREEN

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache
```

Output:

```text
BUILD SUCCESSFUL in 7s
25 actionable tasks: 9 executed, 16 up-to-date
Configuration cache entry reused.
```
