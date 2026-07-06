# Design: Playback Repeat and Shuffle Modes

## Overview

Add shared playback ordering controls for repeat and shuffle, then expose them as buttons on `NowPlayingScreen`. Playback mode state and queue-order decisions live in shared common code (`PlaybackState` and `PlaybackController`). Platform playback engines continue to load/play/pause/seek/stop one selected media item at a time.

## State model

Add shared enums:

- `RepeatMode.RepeatOne`
- `RepeatMode.RepeatPlaylist`
- `RepeatMode.StopAfterCurrent`
- `RepeatMode.StopAfterQueue`
- `ShuffleMode.Off`
- `ShuffleMode.On`

Expose `repeatMode` and `shuffleMode` on `PlaybackState`. Default to `RepeatMode.StopAfterQueue` and `ShuffleMode.Off` to keep shuffle disabled and preserve current automatic list-stop behavior as the default.

## Effective order

`PlaybackController` remains the single source of truth for queue navigation. It should maintain an effective order of track ids:

- Shuffle off: effective order equals the current queue order.
- Shuffle on: enabling shuffle immediately generates a shuffled order containing every current queue track exactly once. The current track remains the current effective position, so toggling shuffle does not switch tracks.

Disabling shuffle discards the shuffled order and resumes navigation from the current track's position in the original queue. Updating the queue while shuffle is on regenerates the shuffled order for the new queue and preserves the selected/current track when it still exists.

The visible library and browse lists are not reordered by shuffle.

## Completion and transport semantics

Automatic completion:

- `RepeatOne`: seek to `0` and replay the current track.
- `RepeatPlaylist`: advance to the next effective track and wrap from the final effective track to the first.
- `StopAfterCurrent`: keep the current track, publish a non-playing state at the end, and do not advance.
- `StopAfterQueue`: advance through the effective order until the final track, then publish a non-playing state at the end and do not wrap.

Manual previous/next:

- Follows the same effective order as completion, including shuffled order when shuffle is on.
- Wraps only in `RepeatPlaylist`.
- Does not wrap in `RepeatOne`, `StopAfterCurrent`, or `StopAfterQueue`; missing boundary directions are no-ops.
- Remains available for adjacent tracks in `RepeatOne` and `StopAfterCurrent`; those modes do not lock manual navigation to the current track.

When stopping at the end for `StopAfterCurrent` or terminal `StopAfterQueue`, keep `positionMillis` at the known duration when available. If duration is unknown, preserve the latest non-negative position.

## NowPlayingScreen UI

Add two mode controls near the existing transport controls:

- Shuffle toggle: switches `ShuffleMode.Off` / `ShuffleMode.On` and highlights when on.
- Repeat cycle: cycles `StopAfterQueue -> RepeatPlaylist -> RepeatOne -> StopAfterCurrent -> StopAfterQueue`.

Use existing shared Compose styling, `HausColors`, `hausClickable`, and Material vector icons. Do not use emoji/text glyph icons. Each control must have a useful `contentDescription` naming the current mode or next action.

The UI should call controller methods (for example `toggleShuffleMode()` and `cycleRepeatMode()`) instead of mutating state directly.

## Test strategy

Add common tests for controller/order behavior:

- default mode state;
- each repeat mode's completion behavior;
- manual next/previous boundary behavior;
- shuffled order generation contains every track exactly once and keeps current track active;
- previous/next/completion follow shuffled order;
- disabling shuffle returns to original queue order;
- queue replacement while shuffle is on regenerates order and preserves selected/current track when possible.

## Risks

Shuffle introduces randomness into tests. Keep tests deterministic by injecting or isolating the shuffle function, or by exposing a pure order-building helper that accepts a deterministic shuffle strategy in tests.

Changing previous/next from current UI-local wraparound to shared mode-aware behavior can affect platform media controls too. This is intended: all transport paths should use the same shared repeat/shuffle semantics.
