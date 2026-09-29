# Proposal

## Why

Listeners need a bounded listening session without having to manually stop playback. The existing repeat modes stop at the end of a track or queue, but cannot stop after a time interval or a chosen number of natural track completions, and they provide no optional gentle fade.

## What Changes

- Add a process-lifetime sleep timer to playback with a timed deadline or a count of natural track completions, including stop after current and stop after N tracks.
- Add optional ten-second output-gain fade before the stop boundary; restore full playback gain on cancellation, rearming, or completion. Do not change system volume.
- Expose an accessible, localized timer panel and active-state summary in Now Playing on Android, iOS, and macOS.
- Preserve queue, repeat, shuffle, history, and user files; expiration stops playback through the existing controller and does not alter repeat mode. Manual/device acceptance belongs to the user; automated checks belong to the implementation owner.

## Capabilities

### New Capabilities

- `sleep-timer`: session-local timed and natural-completion stopping, fade behavior, state, and UI control.

### Modified Capabilities

- None. Playback transport and repeat-mode requirements remain unchanged; the timer is a separate process-local overlay on playback.

## Impact

- `:core:playback` controller and engine seam; Android Media3, iOS Swift `AVAudioPlayer`, and macOS JNI/Objective-C++ gain adapters.
- `:feature:nowplaying` timer presentation, English/Chinese resources, `:shared` composition and DI.
- No database migration, new runtime dependency, audio-file mutation, or persistent timer state. Background execution must be tested under each platform's existing playback process/session behavior.
