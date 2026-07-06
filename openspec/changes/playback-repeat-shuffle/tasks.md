# Tasks

- [x] 1. Add shared repeat/shuffle playback mode model and tests.
  - [x] Add `RepeatMode` and `ShuffleMode` to shared playback common code.
  - [x] Expose `repeatMode` and `shuffleMode` on `PlaybackState` with defaults `StopAfterQueue` and `Off`.
  - [x] Add deterministic common tests for default state and mode mutation APIs.
  - [x] Verify the focused tests fail before implementation and pass after implementation.
  - Evidence: implemented in `5f1225a feat: add playback mode state`; Task 1 review approved with no findings.

- [x] 2. Centralize effective queue-order navigation in `PlaybackController`.
  - [x] Add a shared effective-order decision path for automatic completion, manual next, and manual previous.
  - [x] Implement completion behavior for `RepeatOne`, `RepeatPlaylist`, `StopAfterCurrent`, and `StopAfterQueue`.
  - [x] Implement shuffle order generation that keeps the current track active and contains every queue track exactly once.
  - [x] Implement shuffle disable and queue replacement behavior.
  - [x] Replace ad-hoc next/previous selection paths with controller APIs so UI and platform transport commands share semantics.
  - [x] Add common tests for repeat boundaries, shuffle order, previous/next, completion, and queue replacement.
  - Evidence: implemented in `ac59554 feat: add repeat and shuffle queue navigation`; loading-state review finding fixed in `0c6f394 fix: preserve loading state during auto play transitions`; broad-suite async test race fixed in `cbfcfdc test: stabilize playback controller mode tests`; Task 2 re-review approved with no findings after the loading-state fix.

- [x] 3. Add NowPlayingScreen mode controls.
  - [x] Add repeat cycle and shuffle toggle buttons to `NowPlayingScreen` near the existing transport controls.
  - [x] Use Material vector icons and existing `HausColors`/`hausClickable` styling; do not use emoji icon glyphs.
  - [x] Add clear content descriptions for accessibility/testability.
  - [x] Wire controls to shared controller methods.
  - [x] Preserve existing artwork, status, scrubber, play/pause, previous, next, and back behavior.
  - Evidence: implemented in `906a00e feat: add now playing repeat shuffle controls`; Task 3 review approved with no findings.

- [x] 4. Verify and record evidence.
  - [x] Run `openspec validate playback-repeat-shuffle --strict`: `Change 'playback-repeat-shuffle' is valid`.
  - [x] Run focused common playback tests: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache` -> `BUILD SUCCESSFUL in 1s` after deterministic test hardening.
  - [x] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial two broad runs exposed an async controller-test race in `PlaybackControllerTest.disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack`; exact focused reruns passed, the tests were made deterministic in `cbfcfdc`, and the final broad rerun returned `BUILD SUCCESSFUL in 2s`.
  - [x] Run `/usr/bin/xcrun xcodebuild -version`: `Xcode 26.6`, `Build version 17F113`.
  - [x] Run `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: `BUILD SUCCESSFUL in 18s`.
  - [x] Update this checklist and `progress.md` with exact command outcomes, changed files, risks, next owner, and commit status.
