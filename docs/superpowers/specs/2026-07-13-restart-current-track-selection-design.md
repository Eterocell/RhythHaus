# Restart Current Track Selection Design

## Summary

RhythHaus will treat track rows as selection controls rather than play/pause controls. Selecting the active track from Library home, album detail, artist detail, or Search restarts it from position zero and ensures playback. Selecting another track builds the queue from the exact list currently shown and plays that track.

## Current State

Library home and drill-down track rows conditionally call `setQueue(...)` and then always call `togglePlayPause()`. A click on the active playing row therefore pauses it. Search always queues the complete library, even when the user selected from a filtered result subset. Drill-down rows and the drill-down Now Playing transport also share one callback, so their responsibilities must be separated.

## Design

### Playback controller

Add `PlaybackController.restartCurrentTrack()`. For a loaded playing, paused, stopped, or buffering track, it resets the public position to zero and performs `engine.seekTo(0L)` followed by `engine.play()` inside one serialized engine action. While loading, it resets the public position and requests autoplay after the pending load without seeking the engine's potentially stale item. Idle or error state reloads the current track with autoplay. With no current track, it does nothing.

The method does not replace the queue and does not change repeat or shuffle settings. `togglePlayPause()` remains unchanged for explicit transport controls.

### Shared selection policy

Add an internal shared helper that receives the controller, the visible `List<PlayableTrack>`, and selected ID. A missing selected ID is a no-op. Selecting the controller's current ID calls `restartCurrentTrack()` and preserves its existing queue. Selecting another ID calls `setQueue(visibleQueue, selectedId)` and requests playback.

The visible queue is the rendered Songs list for Library home, the rendered album or artist list for drill-down, and the current filtered result list for Search.

### UI wiring

Home, album, artist, and Search row clicks use the shared helper. Drill-down row selection is separated from the Now Playing bar callback; the bar keeps the existing play/pause toggle behavior. Existing selection highlights, equalizer indicators, navigation, dismissal, and visuals remain unchanged.

## Constraints

- Shared Kotlin Multiplatform behavior only.
- Preserve current queue membership and ordering when restarting the active track.
- Preserve repeat and shuffle mode values.
- Preserve dedicated play/pause, skip, completion, Now Playing, and media-session behavior.
- Do not change platform playback-engine interfaces, dependencies, persistence, database schemas, navigation, or styling.
- Do not add Windows or Linux product support.

## Error and Race Handling

The shared helper validates selected-ID membership to prevent `setQueue` from falling back to an unrelated first item. Loading-state restart does not seek the engine, avoiding a seek against the previous media item. Existing load cancellation and current-track guards continue to make the latest different-track selection win.

## Testing and Verification

- RED/GREEN controller tests cover event order and playing, paused, stopped, loading, idle/error, and no-current states.
- RED/GREEN shared selection tests cover queue preservation, visible ordering, invalid IDs, and repeat/shuffle preservation.
- Focused Library navigation and shared compilation checks cover callback separation and UI wiring.
- Strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, and `git diff --check` provide completion evidence.
- Manual playback QA remains required on an available target for audible restart and queue navigation behavior.
