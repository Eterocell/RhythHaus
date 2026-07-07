# Design: Architecture Refactor

## Overview

The refactor will split the current shared app monolith into a small root entry point, a named common state/coordinator boundary, and focused UI files. It preserves current behavior while making future changes easier to isolate and test.

## State/coordinator boundary

Introduce a shared coordinator/state holder for library app orchestration. It should centralize responsibilities currently scattered through `LibraryHomeScreen(...)`:

- current `LibraryNavigationStack` and last `LibraryNavigationTransition`;
- selected track id and selected-track fallback behavior;
- browse mode;
- expanded Now Playing visibility;
- bottom bar visibility and scroll-derived updates;
- import/scan state where still owned by common UI;
- actions for push/pop/replace route, opening overlays, dismissing overlays, playing selected tracks, clearing library, and updating scroll state.

The state holder may be a Compose-remembered common state object or a common ViewModel-style object. The implementation should choose the lowest-risk option that preserves behavior and avoids new dependencies. Platform dependencies stay injected; the state holder must not construct platform playback engines, database drivers, or platform source access directly.

## Pure decisions

Move decisions out of composable bodies where practical and test them in common tests. Existing `LibraryNavigation.kt` helpers are the model. Candidate decisions include:

- whether a wide detail route should replace the current detail route or push;
- selected-track fallback when playback state or library content changes;
- bottom-bar visibility state update from previous/current scroll position;
- route transition update for push/pop/replace/root;
- back handling decisions where they can be represented without Compose APIs.

## File organization

Target file responsibilities:

- `App.kt`: `App()` entry point, dependency construction, theme preference collection, handoff to the library shell.
- `RhythHausTheme.kt` or existing theme file: shared Haus/Miuix theme wrapper if moved.
- `LibraryAppState.kt`: coordinator/state holder and pure orchestration helpers.
- `LibraryHomeScreen.kt` or `LibraryAppShell.kt`: root library screen shell, adaptive list/detail container, transition host, fixed bottom bar, Now Playing overlay.
- `LibraryRoutes.kt`: route-content selection for home/detail/settings/search/dialog routes.
- `LibraryHomeContent.kt`: home list content and browse sections.
- `LibraryDetailContent.kt`: drill-down album/artist detail screens.
- `LibraryChrome.kt`: nested-scroll chrome, scrollbar, system-bar padding helper.
- `LibraryDialogs.kt`: clear-library dialog route.
- `LibraryCards.kt` / `LibraryRows.kt`: album, artist, track, import, and section presentational components if needed to keep files focused.

The exact file names may be adjusted in the implementation plan to minimize churn, but the responsibility split must remain clear.

## Incremental plan shape

Each implementation task should keep the project compiling and preserve behavior:

1. Add tests for pure extracted decisions.
2. Introduce the state/coordinator boundary while leaving UI layout mostly in place.
3. Extract the route/adaptive shell.
4. Extract home/detail content.
5. Extract chrome/dialog/leaf UI components.
6. Run final verification and record evidence.

## Verification

Focused verification should include common tests covering extracted decisions after each slice. Final verification should include:

- `openspec validate architecture-refactor --strict`;
- `./gradlew :shared:jvmTest --configuration-cache` or a justified focused equivalent during intermediate tasks;
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`;
- `/usr/bin/xcrun xcodebuild -version`;
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`;
- `git diff --check`.
