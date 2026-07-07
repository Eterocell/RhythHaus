# Tasks

- [x] 1. Add tested Now Playing adaptive layout-mode helper.
  - [x] Add `NowPlayingAdaptiveLayoutMode { Compact, Split }` and `nowPlayingAdaptiveLayoutModeFor(widthDp, heightDp)` in common code.
  - [x] Cover phone portrait, narrow portrait tablet, wide tablet, medium landscape, and desktop-width cases in common tests.
  - [x] Verify the focused tests fail before implementation and pass after implementation.
  - Evidence: implementation commit `35b44ac` added the helper and tests; final focused verification `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 322ms`; 25 actionable tasks: 4 executed, 21 up-to-date; configuration cache entry reused).

- [x] 2. Refactor Now Playing screen into reusable artwork and controls panes.
  - [x] Keep `NowPlayingScreen(...)` compact output unchanged.
  - [x] Extract private artwork and controls composables in `NowPlayingScreen.kt`.
  - [x] Preserve all existing callbacks, content descriptions, status labels, artwork fallback, progress scrubber, and playback controls.
  - [x] Run focused compile/tests.
  - Evidence: implementation commit `e5f825e` split the Now Playing panes; final broad verification `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache entry reused).

- [x] 3. Add wide split Now Playing layout.
  - [x] Use `BoxWithConstraints` and `nowPlayingAdaptiveLayoutModeFor(...)` in `NowPlayingScreen`.
  - [x] Render compact mode with the preserved vertical layout.
  - [x] Render split mode with artwork/accent visual on the left and metadata/progress/controls on the right.
  - [x] Preserve overlay dismissal gestures/back behavior by keeping the root screen/back structure unchanged.
  - [x] Run focused compile/tests.
  - Evidence: implementation commit `9ac2af4` added the adaptive split layout; final broad verification `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache entry reused).

- [x] 4. Final verification and evidence.
  - [x] Run `openspec validate adaptive-now-playing-screen --strict`.
    - Evidence: pass (`Change 'adaptive-now-playing-screen' is valid`).
  - [x] Run focused Now Playing/helper tests.
    - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 322ms`; 25 actionable tasks: 4 executed, 21 up-to-date; configuration cache entry reused).
  - [x] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
    - Evidence: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache entry reused). Existing warning only: `PlaybackEngine.android.kt:252:17 'fun setArtworkData(p0: ByteArray?): MediaMetadata.Builder' is deprecated`.
  - [x] Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
    - Evidence: xcodebuild pass (`Xcode 26.6`, `Build version 17F113`); iOS simulator tests pass (`BUILD SUCCESSFUL in 22s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache entry reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest.kt` about unnecessary non-null assertions/no casts needed.
  - [x] Update `progress.md` with route, verification, changed files, blockers, and next owner.
    - Evidence: final handoff added to `progress.md`; hygiene checks passed: `git diff --check` had no output, and `grep -R "miuix-navigation3-adaptive\|ListDetailPaneScaffold\|androidx.navigation3.adaptive" -n gradle shared/src androidApp/src || true` had no output.
