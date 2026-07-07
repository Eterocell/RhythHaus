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
