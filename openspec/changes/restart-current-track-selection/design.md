## Context

Library home song rows and album/artist drill-down rows currently combine track selection with `togglePlayPause()`. Clicking the active row therefore pauses a playing track instead of restarting it. Search uses a separate path that always rebuilds the queue from the complete library, even though only filtered results are visible.

The change is shared Kotlin Multiplatform behavior. Platform playback engines already expose ordered `seekTo` and `play` operations and do not need new contracts.

## Goals / Non-Goals

**Goals:**

- Make selecting the current track restart it at zero and ensure playback.
- Make selecting another track replace the queue with the exact visible list and auto-play the selected track.
- Apply the contract consistently to Library home, album detail, artist detail, and Search.
- Preserve the current queue when the active track is selected from any list.
- Preserve dedicated play/pause controls, repeat mode, shuffle mode, and platform playback behavior.

**Non-Goals:**

- Persisting playback state or queue state across launches.
- Changing skip, completion, repeat, shuffle, media-session, or Now Playing semantics.
- Changing visual styling, navigation, database schemas, dependencies, or platform playback-engine interfaces.

## Decisions

### Add a focused controller restart operation

`PlaybackController.restartCurrentTrack()` will own the state-sensitive restart behavior. For a loaded track it will publish position zero, then issue `engine.seekTo(0L)` followed by `engine.play()` in one serialized engine action. Separate `seekTo()` and `play()` calls were rejected because independently launched coroutines do not express a strict seek-before-play contract.

While the current track is still loading, restart will reset the published position and set the existing `playWhenLoaded` request without seeking the engine, because the engine may still contain the previous track. Idle or error state will reload the current track with autoplay. With no current track, the operation is a no-op.

### Centralize Library selection policy

An internal shared helper will accept a `PlaybackController`, the exact visible `List<PlayableTrack>`, and the selected track ID. It will reject IDs absent from that list. If the ID already matches the controller's current track, it will call `restartCurrentTrack()` without replacing the queue. Otherwise it will call `setQueue` with the visible list and request playback.

Keeping this policy outside Composables prevents home, drill-down, and Search behavior from drifting. Reloading the current track through `setQueue` was rejected because it would unnecessarily replace queue membership, regenerate shuffle order, and reload media/artwork.

### Define visible queue per surface

- Library home Songs uses `snapshot.tracks` in rendered order.
- Album detail uses `albumTracks` in rendered order.
- Artist detail uses `artistTracks` in rendered order.
- Search uses the current filtered results in rendered order, not the complete library.

### Separate selection from transport controls

Drill-down row clicks will receive a track-selection callback, while the drill-down Now Playing bar retains a dedicated play/pause callback. `togglePlayPause()` remains unchanged and continues to serve explicit transport controls.

## Risks / Trade-offs

- **Risk: A restart during loading could seek the previously loaded engine item.** → Loading state only updates the pending autoplay request and published position; it does not seek the engine.
- **Risk: A stale UI click could select a missing ID and fall back to the first item.** → The shared helper validates membership before calling `setQueue`.
- **Risk: Search next/previous behavior changes from the complete library to the result subset.** → This is intentional and matches the visible-list queue contract.
- **Trade-off: Pure shared tests do not exercise physical audio output.** → Controller event-order tests and cross-platform compilation protect the contract; live playback remains a manual acceptance check.
