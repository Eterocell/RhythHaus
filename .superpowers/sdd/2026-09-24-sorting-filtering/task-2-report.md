# Task 2 report: Shared browse-query wiring

## Scope

Wired the accepted feature-owned `LibraryBrowseQuery` API through the Shared Library composition boundary. The query is ephemeral `LibraryAppState` state; no repository, scanner, engine, platform, or persistence contract changed.

## Implementation

- Added default `LibraryBrowseQuery` state plus narrow sort, direction, favorite-only, artwork-only, and source-ID setters to `LibraryAppState`.
- Added immutable source-ID and modified-time projections to `LibraryContentState`, populated with the authoritative `LibraryTrack` values during content loading.
- Passed source, created, modified, favorite, and play-history projections from `App.kt` through both compact and wide `LibraryHomeScreen` branches into `LibraryHomeContent`.
- Passed query state and the narrow state-update callbacks into the feature public Home boundary without rendering controls.
- Updated `LibraryHomeContent` to derive its flat Home sequence through `visibleTracksForBrowseQuery`; Albums and Artists remain passed through unchanged.
- Updated existing Home tests for the specified default title-ascending Songs ordering and added a composable assertion that source filtering plus modified-time ordering receive the Shared-style projections.
- Added common Shared tests covering default state, independent updates, new-shell reset, authoritative source/modified projections, and playback-state isolation.

## Verification evidence

Shared focused selector:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseQueryStateTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 7s
155 actionable tasks: 27 executed, 128 up-to-date
```

Generated test results:

- `LibraryBrowseQueryStateTest[jvm]`: 5 tests, 0 skipped, 0 failures, 0 errors.
- `LibraryAppShellJvmTest[jvm]`: 9 tests, 0 skipped, 0 failures, 0 errors.

Library focused selector:

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 339ms
57 actionable tasks: 10 executed, 47 up-to-date
```

Generated test results:

- `LibraryHomeContentJvmTest[jvm]`: 18 tests, 0 skipped, 0 failures, 0 errors.

No project-wide formatter, linter, or full test suite was run, per the task boundary.

## Follow-up boundary

Task 3 remains responsible for rendering localized accessible controls and filtered-empty behavior. Task 4 remains responsible for selection reconciliation and queue visibility changes after query updates.
