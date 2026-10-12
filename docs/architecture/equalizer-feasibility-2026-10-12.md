# Equalizer feasibility — 2026-10-12

## Decision

Do not add a shared equalizer control or preference against the current playback engines. That would either be a silent no-op on Apple platforms or an Android-only, device-dependent effect presented as cross-platform behavior.

## Observed playback boundaries

- Android playback is hosted by `RhythHausPlaybackService` through Media3 `ExoPlayer` instances. `AndroidServicePlayback` owns separate players for the current, prepared-incoming, and retiring voices; `MediaSession` is rebound when logical current changes. An Android `android.media.audiofx.Equalizer` attaches to an audio session and may be unavailable or unsupported by the device. Applying it consistently to two overlapping voices and across player replacement/reconnection requires explicit ownership and capability handling.
- iOS uses `AVAudioPlayer` in `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift`, with one AVAudioSession and up to three logical voices during reversible crossfade. `AVAudioPlayer` has no attachable `AVAudioUnitEQ` graph. The Kotlin engine delegates route/interruption and remote transport/session metadata through the existing provider bridge.
- macOS uses native `AVAudioPlayer` voices in `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`, including its own remote-command integration, route listeners, and run-owned completion callbacks. It likewise has no attachable multiband EQ graph.
- Apple provides `AVAudioEngine` and `AVAudioUnitEQ`, confirmed available in the installed SDK without a third-party dependency. Adopting that path is not a local filter insertion: current file-backed decoding, multiple crossfade voices, gain composition, route/interruption management, and remote-session behavior must be preserved or deliberately re-established.
- App playback gain is already composed in the controller for ReplayGain, sleep attenuation, and per-voice crossfade. An EQ path must preserve each voice's existing gain and must not alter system volume.

## Viable future paths

1. Replace both Apple AVAudioPlayer implementations with an explicitly specified PCM/audio-engine playback graph, then add the same multiband policy and lifecycle behavior across Android and Apple. This enables a true shared EQ contract but is a broad playback-engine migration.
2. Approve a platform-capability contract that reports effect availability and offers distinct native implementations where supported. This is a narrower product but explicitly not equivalent DSP on all devices/platforms; unavailable systems must show unavailable state rather than claim an active EQ.

## Deferred

Band count/frequencies, gain range/steps, preset behavior, persistence, defaults, and the chosen availability promise remain unspecified. No code or fake control was added. The existing decorative `EqualizerStrip` remains visual-only until one path receives an approved OpenSpec design and executable implementation plan.
