# Tasks

- [ ] 1. Add shared repeat/shuffle playback mode model and tests.
  - [ ] Add `RepeatMode` and `ShuffleMode` to shared playback common code.
  - [ ] Expose `repeatMode` and `shuffleMode` on `PlaybackState` with defaults `StopAfterQueue` and `Off`.
  - [ ] Add deterministic common tests for default state and mode mutation APIs.
  - [ ] Verify the focused tests fail before implementation and pass after implementation.

- [ ] 2. Centralize effective queue-order navigation in `PlaybackController`.
  - [ ] Add a shared effective-order decision path for automatic completion, manual next, and manual previous.
  - [ ] Implement completion behavior for `RepeatOne`, `RepeatPlaylist`, `StopAfterCurrent`, and `StopAfterQueue`.
  - [ ] Implement shuffle order generation that keeps the current track active and contains every queue track exactly once.
  - [ ] Implement shuffle disable and queue replacement behavior.
  - [ ] Replace ad-hoc next/previous selection paths with controller APIs so UI and platform transport commands share semantics.
  - [ ] Add common tests for repeat boundaries, shuffle order, previous/next, completion, and queue replacement.

- [ ] 3. Add NowPlayingScreen mode controls.
  - [ ] Add repeat cycle and shuffle toggle buttons to `NowPlayingScreen` near the existing transport controls.
  - [ ] Use Material vector icons and existing `HausColors`/`hausClickable` styling; do not use emoji icon glyphs.
  - [ ] Add clear content descriptions for accessibility/testability.
  - [ ] Wire controls to shared controller methods.
  - [ ] Preserve existing artwork, status, scrubber, play/pause, previous, next, and back behavior.

- [ ] 4. Verify and record evidence.
  - [ ] Run `openspec validate playback-repeat-shuffle --strict`.
  - [ ] Run focused common playback tests.
  - [ ] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
  - [ ] Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, or record exact blockers.
  - [ ] Update this checklist and `progress.md` with exact command outcomes, changed files, risks, next owner, and commit status.
