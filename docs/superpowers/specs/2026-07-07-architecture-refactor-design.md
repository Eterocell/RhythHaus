# Architecture Refactor Design

## Summary

RhythHaus will refactor the shared Compose app architecture in a behavior-preserving, incremental way. The main target is `App.kt`, which currently combines dependency wiring, navigation state, scanner orchestration, playback actions, adaptive routing, Now Playing overlay state, chrome, dialogs, and presentational library UI in one large file. The refactor will introduce a named shared state/coordinator boundary and split UI code into focused files without changing product behavior.

## Current context

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` is currently about 1,850 lines.
- `App()` constructs shared dependencies such as `PlaybackController`, metadata readers, database/repository, platform source access, scanner, theme preference store, and folder picker launcher.
- `LibraryHomeScreen(...)` owns route stack mutation, transition classification, predictive-back handling, selected track state, browse mode, scan/import callbacks, Now Playing overlay state, bottom-bar visibility, adaptive list/detail routing, overlay rendering, and route content rendering.
- The file also contains many leaf UI pieces: home content, detail content, top chrome, scrollbar, clear-library dialog, import card, track rows, album cards, artist rows, and helpers.
- `LibraryNavigation.kt` already contains pure route/adaptive/scroll decision helpers with common tests in `LibraryNavigationTest.kt`; this is the pattern to extend.
- The project already depends on shared lifecycle ViewModel/runtime Compose libraries, but this refactor does not require adding dependencies.

## Goals

- Make `App.kt` a small composition/dependency entry point instead of the owner of most library internals.
- Introduce a shared app state/coordinator boundary for library navigation, selected track, scan/import status, bottom-bar visibility, and Now Playing overlay state.
- Keep behavior unchanged: routes, animations, adaptive thresholds, scanner behavior, playback queue behavior, Miuix/blur behavior, strings, content descriptions, and visual structure must remain equivalent.
- Split library UI into focused files by responsibility so future features can change smaller surfaces.
- Add or preserve common tests for extracted architecture decisions before and during movement.
- Keep all shared-first behavior in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus` unless platform integration already lives behind an existing seam.

## Non-goals

- No visual redesign.
- No playback engine rewrite.
- No scanner, repository, database schema, TagLib, source-access, or artwork-cache redesign.
- No platform-native navigation migration.
- No dependency or toolchain changes.
- No Windows/Linux product or packaging support.
- No broad package/module restructuring beyond focused common-source files.

## Design

### State/coordinator boundary

Add a small shared library app state/coordinator boundary that centralizes orchestration currently scattered through `LibraryHomeScreen(...)`.

The coordinator should own or expose:

- current `LibraryNavigationStack` and last `LibraryNavigationTransition`;
- selected track id derived from library/current playback state;
- browse mode;
- expanded Now Playing visibility;
- bottom bar visibility and previous scroll position;
- import message and scan progress when these remain in common state;
- action methods for opening detail routes, replacing detail routes in wide layout, popping routes, opening/dismissing overlays, selecting/playing tracks, updating scroll-derived bottom bar state, and clearing library.

The coordinator does not own platform playback implementation, database drivers, platform source access, or TagLib internals. Those remain injected services or existing platform seams.

The first implementation can be a Compose-remembered state holder rather than a full ViewModel if that is the lowest-risk way to preserve behavior. If a ViewModel is used, it must remain in common code and receive dependencies explicitly rather than constructing platform objects internally.

### Pure decision helpers

Keep pure decisions outside composables and cover them with common tests where practical. Existing helpers in `LibraryNavigation.kt` should remain the model. New or refined helpers may include:

- wide-detail route replacement decision;
- selected-track fallback decision;
- bottom-bar visibility update from previous/current scroll position;
- route transition update behavior when pushing/replacing/popping;
- Now Playing visibility decisions for back handling where practical.

These helpers make the refactor reviewable without relying only on visual testing.

### File split

Split `App.kt` into focused files while preserving package-private visibility where possible:

- `App.kt`: app entry point, dependency construction, theme wrapper call, top-level handoff to the library app shell.
- `RhythHausTheme.kt` or existing theme file: shared Miuix/Haus theme wrapper if moved from `App.kt`.
- `LibraryAppState.kt`: coordinator/state holder and pure orchestration helpers that do not belong in route models.
- `LibraryHomeScreen.kt` or `LibraryAppShell.kt`: root library shell, adaptive list/detail container, route transition host, bottom bar, Now Playing overlay.
- `LibraryRoutes.kt`: route-content composition for home/detail/settings/search/dialog routes.
- `LibraryHomeContent.kt`: home list, header, import card, browse mode picker, album/artist/song entry points.
- `LibraryDetailContent.kt`: drill-down detail screen and track list wiring.
- `LibraryChrome.kt`: nested-scroll top chrome, system-bar padding helper, scrollbar.
- `LibraryDialogs.kt`: clear-library dialog route.
- `LibraryCards.kt` / `LibraryRows.kt`: presentational album/artist/track UI pieces if splitting further keeps files focused.

Exact file names can be adjusted during planning to minimize churn, but each file must have one clear responsibility.

### Incremental behavior-preserving flow

The implementation should avoid a big-bang rewrite. Each task must keep the app compiling and should move one responsibility at a time:

1. Add tests around extracted pure decisions.
2. Introduce coordinator/state holder while still rendering through the existing UI structure.
3. Move route/adaptive shell code out of `App.kt`.
4. Move home/detail content out of `App.kt`.
5. Move chrome/dialog/leaf components out of `App.kt`.
6. Run final verification and record evidence.

## Acceptance criteria

- `App.kt` is reduced to a small app entry/dependency/theme handoff file, not the owner of most library UI internals.
- Library orchestration is represented through a named shared state/coordinator boundary with explicit actions.
- Existing route behavior, animation behavior, predictive/system back behavior, adaptive thresholds, bottom-bar behavior, scan/import behavior, clear-library behavior, and playback actions are preserved.
- Existing strings and content descriptions remain unchanged unless a moved call site requires equivalent relocation.
- No new dependencies or toolchain changes are introduced.
- Common tests cover newly extracted pure architecture decisions.
- Focused tests pass after each implementation slice.
- Final verification runs OpenSpec validation, common/JVM tests, desktop compile, Android debug assemble, iOS simulator tests, and diff hygiene unless an exact blocker is recorded.

## Risks and mitigations

- Risk: moving state out of `LibraryHomeScreen(...)` can subtly change Compose state retention. Mitigation: keep keys and initialization behavior explicit, add common tests for pure decisions, and refactor in small slices.
- Risk: route overlays and adaptive list/detail routing are intertwined. Mitigation: split route shell separately from leaf UI, and keep route behavior in existing `LibraryNavigationStack` terms.
- Risk: scan callbacks cross coroutine dispatchers and UI state. Mitigation: do not redesign scanning in this slice; move only ownership boundaries with equivalent callback behavior.
- Risk: visual regressions are hard to prove automatically. Mitigation: preserve presentational code verbatim during extraction, rely on compile/test verification, and record manual visual validation as a recommended follow-up.
