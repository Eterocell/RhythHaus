# Tasks

- [x] 1. Add tested scroll visibility decision helper.
  - [x] Add a pure common helper for deciding whether the `NowPlayingBar` should be visible based on previous/current `LazyListState` positions and current visibility.
  - [x] Cover hide-on-down-scroll, show-on-up-scroll, index-boundary movement, and jitter-threshold no-op behavior in common tests.
  - [x] Verify the focused test fails before implementation and passes after implementation.

- [x] 2. Wire scroll state to every screen that renders a NowPlayingBar.
  - [x] Add a Home `LazyListState` in `LibraryHomeScreen` and pass it to the main Library/Home `LazyColumn`.
  - [x] Observe Home list scroll position and update hoisted `isNowPlayingBarVisible` using the tested helper.
  - [x] Observe Search result list scroll position and update the same hoisted visibility state for the root fixed bar.
  - [x] Observe album/artist `DrillDownView` track-list scroll position and update the same hoisted visibility state.
  - [x] Wrap the root-level `NowPlayingBar` in a bottom enter/exit animation so downward scrolling hides it and upward scrolling shows it.
  - [x] Wrap the drill-down `NowPlayingBar` in the same bottom enter/exit animation.
  - [x] Ensure hidden bars do not intercept pointer input.
  - [x] Preserve existing tap-to-expand, drag-up-to-expand, playback controls, Search, Settings, route transitions, and Now Playing overlay behavior.

- [x] 3. Verify and record evidence.
  - [x] Run `openspec validate library-scroll-bar-visibility --strict`: pass (`Change 'library-scroll-bar-visibility' is valid`).
  - [x] Run focused common tests for the visibility helper and existing navigation tests: pass (`./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`, `BUILD SUCCESSFUL`).
  - [x] Run relevant JVM/desktop/Android verification: pass (`./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `BUILD SUCCESSFUL`).
  - [x] Run iOS simulator verification or record the exact blocker: Xcode 26.6 / Build 17F113; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` pass (`BUILD SUCCESSFUL`).
  - [x] Update this checklist and `progress.md` with exact command outcomes, changed files, risks, next owner, and commit status.
