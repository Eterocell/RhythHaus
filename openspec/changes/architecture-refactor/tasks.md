# Tasks

- [x] 1. Add tests for pure architecture decisions.
  - [x] Identify the smallest pure helpers needed for route replacement, selected-track fallback, bottom-bar visibility update, and route transition actions.
  - [x] Add common tests before wiring moved UI code to the helpers.
  - [x] Run focused common tests and verify expected failures before implementation where new helpers do not exist yet.

- [x] 2. Introduce the shared library app state/coordinator boundary.
  - [x] Add a common state/coordinator file with explicit state and actions for navigation, selected track, browse mode, Now Playing overlay visibility, and bottom-bar scroll visibility.
  - [x] Keep platform services injected; do not construct playback engines, database drivers, platform source access, or TagLib internals in the coordinator.
  - [x] Wire `LibraryHomeScreen(...)` through the coordinator while preserving current behavior.
  - [x] Run focused tests and shared compile.

- [x] 3. Extract the route/adaptive shell from `App.kt`.
  - [x] Move adaptive list/detail root, route transition host, predictive/system back coordination, fixed bottom bar, and Now Playing overlay code into focused common files.
  - [x] Keep route behavior, animations, overlay dismissal, and adaptive thresholds unchanged.
  - [x] Run focused tests and shared compile.

- [x] 4. Extract home and detail library content from `App.kt`.
  - [x] Move home list content, header, import card, browse mode picker, album/artist/song list sections, and drill-down detail screen code into focused common files.
  - [x] Preserve strings, content descriptions, callbacks, scroll reporting, playback actions, and visual structure.
  - [x] Run focused tests and shared compile.

- [x] 5. Extract chrome, dialog, and remaining presentational components.
  - [x] Move nested-scroll top chrome, system-bar padding helper, scrollbar, clear-library dialog, rows/cards, and small helpers into focused files.
  - [x] Remove unused imports from `App.kt` and ensure `App.kt` is a small dependency/theme handoff file.
  - [x] Run focused tests and shared compile.

- [x] 6. Final verification and evidence.
  - [x] Run `openspec validate architecture-refactor --strict`.
  - [x] Run focused/common tests covering extracted decisions.
  - [x] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
  - [x] Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
  - [x] Run `git diff --check`.
  - [x] Update `progress.md` with route, verification, changed files, blockers, and next owner.

## Continuation: destination-scoped Library Back orchestration

- [x] 7. Add RED common tests for the pure Back resolution and identity contract.
  - [x] Define failing tests for modal -> edit -> active-page selection -> Now Playing -> route precedence and root unhandled results.
  - [x] Define failing tests proving only the active destination can publish a consumable modal/edit/selection target and that feature modal ordering remains feature-owned.
  - [x] Define failing tests for stable destination-plus-target identities, including a same-shaped replacement target that must not satisfy an earlier session.
  - [x] Run the focused common tests to record RED before adding the module.

- [x] 8. Implement the minimal `LibraryAppState` destination-scoped Back module.
  - [x] Add the pure resolution model and one module owned by `LibraryAppState`; keep modal/edit state within features and accept only their foremost dismissal/edit-exit publications.
  - [x] Implement `begin`, `complete`, and `cancel` session operations with one latched target and no global modal stack or app-wide selection ownership.
  - [x] Make root resolution explicitly unhandled rather than a consumed no-op.
  - [x] Make Task 7 tests GREEN and run the focused common suite.

- [x] 9. Add session settlement and adapter-equivalence tests before wiring adapters.
  - [x] Add failing tests proving repeated non-predictive input is suppressed until authoritative settlement removes the target or an explicit rejection releases it.
  - [x] Add failing tests proving system, desktop, and predictive adapters use equivalent module decisions for the same authoritative state.
  - [x] Add failing predictive tests for begin-time latching, no retargeting, cancellation with no transition, invalid completion with no fall-through, and completion of an unchanged valid target.
  - [x] Run the focused common tests to record RED for the new lifecycle behavior.

- [x] 10. Wire thin Back adapters incrementally and make lifecycle tests GREEN.
  - [x] Route system and desktop Back through the module's begin/dispatch/session-settlement contract without changing platform defaults for unhandled root input.
  - [x] Route predictive begin/progress/cancel/complete through the same latched session, without completion-time resolution or fall-through.
  - [x] Preserve existing feature callbacks as authoritative publications/rejections; do not move feature modal ordering or edit state into `LibraryAppState`.
  - [x] Run focused common/JVM tests and shared compilation after each adapter slice.

- [x] 11. Add RED/GREEN tests and wiring for displayed-playlist invalidation.
  - [x] Add failing tests that deletion of the exactly displayed active playlist atomically leaves that destination and clears only state owned by it.
  - [x] Add failing tests that deletion of an unrelated playlist, or of a matching playlist while its destination is inactive, preserves the active destination and unrelated Library state.
  - [x] Implement exact active-destination invalidation separately from Back sessions; it must not dispatch, settle, or fall through as a Back intent.
  - [x] Make the invalidation tests GREEN and run the focused common/JVM suite.

- [x] 12. Complete continuation verification and evidence.
  - [x] Run `openspec validate architecture-refactor --strict`.
  - [x] Run the focused Back and invalidation test matrix, then `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
  - [x] Run `/usr/bin/xcrun xcodebuild -version`, `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, and `git diff --check`.
  - [x] Update the OpenSpec task state and handoff evidence with the adapter/invalidation scope, exact commands, results, blockers, and next owner. Fresh final evidence is recorded in the 2026-07-27 `progress.md` handoff; automated gates pass, while runtime/manual interaction evidence remains unverified.
