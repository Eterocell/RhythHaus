## Why

The iOS system media panel (Lock Screen / Control Center) shows greyed-out previous/next track buttons and a greyed-out progress slider, even during active playback. AirPods gestures still work for track navigation. Systematic debugging (Phase 1–3) confirmed the root cause: `MPMediaItemPropertyPlaybackDuration` is omitted from the `nowPlayingInfo` dictionary when `durationMillis` is null, which causes iOS to render the slider and transport row as disabled. All five alternative root cause candidates (commands not enabled, `playbackState` property, threading, artwork clobber, rate=0 window) were ruled out via simulator diagnostic tests and git history analysis.

## What Changes

- Re-probe `AVAudioPlayer.duration` after `play()` starts — the duration may return 0 during `prepareToPlay()` but become valid once playback begins. Update `durationMillis` and re-emit `nowPlayingInfo` if a valid duration is found.
- Ensure `MPMediaItemPropertyPlaybackDuration` is always present in the `nowPlayingInfo` dictionary when a track is loaded and playing. If `durationMillis` remains null after all fallbacks, set `MPNowPlayingInfoPropertyIsLiveStream = true` so iOS renders a "live" indicator instead of a greyed-out slider.
- Add a diagnostic test that asserts the duration key is present in the dictionary after the `play()` code path, even when the initial `durationMillis` was null.

## Capabilities

### New Capabilities
- `ios-now-playing-info`: iOS Now Playing info dictionary management — ensuring `MPNowPlayingInfoCenter` always has valid duration, elapsed time, playback rate, and track metadata so the system media panel renders transport controls as enabled.

### Modified Capabilities
<!-- No existing specs to modify. -->

## Impact

- **Code**: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` — `load()`, `play()`, `buildIOSNowPlayingDictionary()`, `updateNowPlayingInfo()`
- **Tests**: `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingDiagnosticTest.kt` — new test for duration key presence after play path
- **No dependency changes**, no API changes, no breaking changes
- **Platform scope**: iOS only (macOS uses a separate Obj-C implementation in `rhythhaus_audio.mm` that already sets `playbackState` and handles duration differently)
