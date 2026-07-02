## 1. Re-probe AVAudioPlayer.duration in play()

- [x] 1.1 In `PlaybackEngine.ios.kt` `play()`, after `audioPlayer.play()` succeeds, re-probe `audioPlayer.duration`. If `durationMillis` is null AND the probed duration > 0, update `durationMillis` and call `updateNowPlayingInfo()` with the new duration.
- [x] 1.2 Log the re-probe result for real-device verification (`log.d { "Re-probed duration after play(): ..." }`).

## 2. Add IsLiveStream fallback in updateNowPlayingInfo()

- [x] 2.1 In `PlaybackEngine.ios.kt` `updateNowPlayingInfo()`, when `durationMillis` is null, set `MPNowPlayingInfoPropertyIsLiveStream = true` in the dictionary instead of omitting the duration key entirely. (Handled via `buildIOSNowPlayingDictionary` change)
- [x] 2.2 Import `platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream` at the top of the file.
- [x] 2.3 When `durationMillis` is non-null, ensure `MPNowPlayingInfoPropertyIsLiveStream` is NOT set (or set to false) so the slider renders normally. (Handled via `buildIOSNowPlayingDictionary` change)

## 3. Update buildIOSNowPlayingDictionary to support IsLiveStream

- [x] 3.1 Add a `durationMillis: Long?` parameter handling to `buildIOSNowPlayingDictionary`: when non-null, put `MPMediaItemPropertyPlaybackDuration`; when null, put `MPNowPlayingInfoPropertyIsLiveStream = true`. This keeps the dictionary builder as the single source of truth for dict construction.
- [x] 3.2 Remove the `durationMillis?.let` conditional from `buildIOSNowPlayingDictionary` and replace with the IsLiveStream-aware logic.

## 4. Add diagnostic test for duration key presence after play path

- [x] 4.1 In `IOSNowPlayingDiagnosticTest.kt`, add a test that calls `buildIOSNowPlayingDictionary` with `durationMillis = null` and asserts that `MPNowPlayingInfoPropertyIsLiveStream` is present and `true`.
- [x] 4.2 Add a test that calls `buildIOSNowPlayingDictionary` with `durationMillis = 181_000L` and asserts that `MPMediaItemPropertyPlaybackDuration` is present AND `MPNowPlayingInfoPropertyIsLiveStream` is absent (or false).

## 5. Verify and clean up

- [x] 5.1 Run `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` — all tests must pass. BUILD SUCCESSFUL.
- [x] 5.2 Run `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache` — compile must succeed. BUILD SUCCESSFUL.
- [x] 5.3 Remove the `[NP-DBG]` diagnostic log lines from `PlaybackEngine.ios.kt` and `AudioMetadata.ios.kt` (they were added for root-cause investigation and should not ship in the fix).
- [x] 5.4 Commit with message: `fix: ensure iOS nowPlayingInfo always has duration or live-stream indicator`
