# Task 2 Report - Shared Library playback-selection policy

Status: DONE_WITH_CONCERNS

## Scope

- Added `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt` with the brief-specified `selectLibraryTrackForPlayback(...)` helper.
- Added `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt` with direct common tests for current selection, different selection, mode preservation, invalid selection, and empty queues.
- Did not modify Task 1 controller behavior, UI, Search, OpenSpec, roadmap, progress, plans/specs, platform engines, dependencies, or unrelated files.

## Strict TDD evidence

### RED

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache
```

Result: `BUILD FAILED` during `:shared:compileTestKotlinJvm` because all five test calls reported `Unresolved reference 'selectLibraryTrackForPlayback'`, matching the expected missing-helper failure.

### GREEN

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 3s`; `:shared:jvmTest` passed with the five selection-policy tests.

The async assertions use `CompletableDeferred`, an unlimited `Channel`, and `withTimeout`; no sleeps are used.

## Self-review

- Current-track selection checks membership in the visible queue, calls `restartCurrentTrack()`, and leaves the controller's existing queue intact even when it differs from the visible queue.
- Different-track selection passes the exact visible list to `setQueue(...)`, selects the requested ID, and calls `play()` so loading autoplays.
- Existing repeat and shuffle mode values remain unchanged through queue replacement.
- Invalid IDs and empty visible queues return before calling controller mutations, so they cannot fall back to the first visible track.
- The helper implementation matches the task brief verbatim and introduces no extra behavior.

## Commit

- `b52295f` - `feat: centralize library track selection playback`

## Concerns

- Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined. The focused Gradle compile/test command passed and therefore provided compiler validation for both changed Kotlin files.
- This report is intentionally a separate documentation commit so the brief-required helper/tests commit remains exactly atomic.
