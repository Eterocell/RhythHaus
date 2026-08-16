## Why

When the OS pauses or interrupts playback without a user command (an AirPods takeover, an incoming call, Siri, or an unplugged headphone), the iOS and macOS engines never emit a status change, so the UI keeps showing "pause" while nothing is actually playing. Playback state must track system play/pause broadcasts so the interface reflects reality.

## What Changes

- iOS engine observes `AVAudioSession.interruptionNotification` (`.began` / `.ended` + `shouldResume`) and `AVAudioSession.routeChangeNotification` (`oldDeviceUnavailable`), reflecting interruption and route loss as `Paused` and auto-resuming only on `.ended` + `shouldResume` when playback was active before the interruption.
- macOS engine observes Core Audio default-output-device loss (HAL property listener) and reflects it as `Paused`.
- New platform-layer bridge interface `IOSAudioInterruptionHandler` (began / ended / route-disconnected) carries these events from the Swift bridge to the iOS engine.
- No change to the public common playback contracts (`PlatformPlaybackEngine`, `PlaybackEngineListener`, `PlaybackStatus`, `PlaybackController`).
- Android is unchanged (already handles audio focus and becoming-noisy).

## Capabilities

### New Capabilities
- `playback-interruption-tracking`: system-initiated interruption and output-route loss are reflected in playback status (`Paused`), with iOS auto-resume on interruption-end when the system allows it and playback was active.

### Modified Capabilities
<!-- none -->

## Impact

- `core/playback/src/iosMain/.../IOSAudioPlayerBridge.kt` — new `IOSAudioInterruptionHandler` interface and `IOSAudioPlayerProvider.interruptionHandler` property.
- `core/playback/src/iosMain/.../PlaybackEngine.ios.kt` — interruption/route handling and `wasPlayingBeforeInterruption` state.
- `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift` — `NotificationCenter` observers for interruption and route-change, and an authoritative `isPlaying` flag.
- `core/playback/src/jvmMain/.../PlaybackEngine.jvm.kt` — route-disconnect consumption in the progress loop plus test seams and an injectable bridge.
- `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm` — Core Audio HAL property listener with an atomic route-disconnect flag and test hooks.
- `core/playback/build.gradle.kts` — link the macOS native helper against the CoreAudio framework.
- Tests: `core/playback/src/iosTest`, `core/playback/src/jvmTest`.
- iOS framework export already covers the bridge interfaces; no architecture allow-list change.
