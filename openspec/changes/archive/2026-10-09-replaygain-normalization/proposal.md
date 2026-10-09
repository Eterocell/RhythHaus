# Proposal

## Why

RhythHaus currently plays tracks at their original gain, so albums mastered at different loudness levels require repeated manual volume adjustment. The next Phase 4 roadmap item introduces optional tag-driven ReplayGain attenuation without modifying media files or system volume.

## What Changes

- Read REPLAYGAIN_TRACK_GAIN, REPLAYGAIN_TRACK_PEAK, REPLAYGAIN_ALBUM_GAIN and REPLAYGAIN_ALBUM_PEAK through the existing TagLib metadata path on JVM, Android file descriptors/content URIs, and iOS.
- Persist nullable ReplayGain facts alongside scanned track metadata, with a corresponding SQLDelight migration; existing tracks remain unadjusted until rescanned.
- Add persistent Off, Track and Album modes, defaulting to Off. Album mode falls back to track gain when album gain is absent; absent or invalid selected gain leaves playback unadjusted.
- Apply attenuation only: positive gain never amplifies playback. A valid corresponding peak further limits gain to avoid clipping; missing peak does not invalidate a valid gain.
- Compose normalization and sleep-timer attenuation through the controller-owned playback-gain boundary. Changing modes or cancelling a fade must not overwrite the other factor, reload audio, alter the queue, or change system volume.
- Add localized, accessible Settings controls and explicit explanation that untagged tracks are unchanged and positive gain is not amplified.
- Preserve scanned facts across app-local metadata display overrides and refresh playback projections without changing track identity.

## Capabilities

### New Capabilities

- `replaygain-normalization`: optional persisted normalization mode, tag selection, attenuation/clipping policy, gain composition and uninterrupted playback behavior.

### Modified Capabilities

- `local-library-scanning`: preserve nullable ReplayGain metadata through scan persistence and rescans without modifying audio files.
- `sleep-timer`: sleep attenuation composes with normalization and restoration restores the current normalization factor rather than unconditional full volume.

## Impact

Affected areas: TagLib native metadata extraction and platform readers; Library metadata/API/repository mapping; Core database schema and migration; Core playback models/controller gain ownership; Shared preference persistence and composition; Settings resources and controls. Android, iOS and macOS use the existing application playback-gain adapters constrained to 0..1.

No dependencies or toolchains are added. No file writes, automatic loudness analysis, preamp controls, crossfade, equalizer, gapless playback, network operations or system-volume changes are included. Automated native-fixture, persistence, gain-composition and UI verification remains implementation-owned; physical listening/system-control acceptance remains user-owned and pending until reported.
