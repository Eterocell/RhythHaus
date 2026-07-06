# Design: Playback Repeat and Shuffle Modes

## Overview

Add shared playback ordering controls for repeat and shuffle, then expose them as buttons on the shared Compose `NowPlayingScreen`. The feature stays shared-first: playback mode state and queue-order decisions live in `PlaybackController` / `PlaybackState`, while platform playback engines keep handling only the currently loaded media item.

The user-facing modes are:

- Repeat: single-track repeat, playlist repeat, play current song then stop, and play current list then stop.
- Shuffle: off, or shuffle songs inside the current playback list.

## Goals

- Add repeat modes to shared playback state and controller behavior.
- Add shuffle mode to shared playback state and controller behavior.
- Make automatic completion, previous, and next use one shared queue-order decision path.
- Add `NowPlayingScreen` controls for repeat and shuffle using existing shared Compose / Material icon style.
- Preserve existing queue ownership in the shared controller; platform engines remain single-current-item playback engines.
- Preserve library browsing order visually; shuffle affects playback order, not visible library list ordering.
- Avoid new dependencies.

## Non-goals

- No persistent storage for repeat/shuffle preferences in this change.
- No system media notification / lock-screen UI additions for repeat or shuffle controls.
- No mini-player `NowPlayingBar` controls for repeat or shuffle.
- No playlist management UI or saved playlists.
- No platform-specific shuffle implementation in Android/iOS/macOS engines.
- No change to scanner, library persistence, metadata extraction, artwork, theme, or navigation architecture.

## Playback modes

Add shared enums near the playback model:

- `RepeatMode.RepeatOne`: when the current track completes, seek to the start and continue playing the same track.
- `RepeatMode.RepeatPlaylist`: playback advances through the effective order and wraps from the last track to the first.
- `RepeatMode.StopAfterCurrent`: when the current track completes, remain on the current track at its end and publish a non-playing state. It does not reset position to 0 and does not advance to another track.
- `RepeatMode.StopAfterQueue`: playback advances through the effective order until the last track, then remains on that last track at its end and publishes a non-playing state.

Add shared shuffle enum:

- `ShuffleMode.Off`: the effective order is the current queue order.
- `ShuffleMode.On`: the controller generates a shuffled effective order for the current queue.

Default state should match current closest product behavior for automatic list playback without unexpectedly enabling shuffle. Use `RepeatMode.StopAfterQueue` and `ShuffleMode.Off` as defaults.

## Effective playback order

`PlaybackController` remains the single source of truth for queue navigation. Add internal order state that can answer:

- current effective track id;
- next effective track id, if any;
- previous effective track id, if any;
- whether a boundary should wrap for the current repeat mode.

When shuffle is off, the effective order is `state.queue.map { it.id }`.

When shuffle is turned on, immediately generate a shuffled order containing every current queue track exactly once. Preserve the currently playing track as the current effective position, so enabling shuffle does not suddenly switch tracks. The visible library / browse lists keep their original order.

When shuffle is turned off, discard the shuffled order and continue from the current track's index in the original queue.

When the queue changes while shuffle is on, regenerate the shuffled order for the new queue and keep the selected/current track as the current effective position when it exists in the queue.

## Boundary behavior

Automatic completion:

- `RepeatOne`: replay current track from the start.
- `RepeatPlaylist`: advance to next effective track, wrapping to the first track at the end.
- `StopAfterCurrent`: stop at the end of the current track and do not advance.
- `StopAfterQueue`: advance to next effective track until the last effective track; at the end, stop at the end of the last track and do not wrap.

Manual previous/next:

- Uses the same effective order as automatic completion, including shuffled order when shuffle is on.
- `RepeatPlaylist` wraps at boundaries.
- `RepeatOne`, `StopAfterCurrent`, and `StopAfterQueue` do not wrap at boundaries. At the first/last track, previous/next is a no-op for the missing direction.
- Manual previous/next is not disabled just because the repeat mode is `RepeatOne` or `StopAfterCurrent`; it can still move to an adjacent effective track when one exists.

Stopping at the end:

- For `StopAfterCurrent` and terminal `StopAfterQueue`, publish a non-playing status and keep `positionMillis` at the known duration when available. If duration is unknown, keep the latest non-negative position.

## NowPlayingScreen controls

Add two mode controls near the existing transport controls:

- Shuffle toggle: switches between `ShuffleMode.Off` and `ShuffleMode.On`; highlighted when on.
- Repeat mode cycle: cycles through `StopAfterQueue -> RepeatPlaylist -> RepeatOne -> StopAfterCurrent -> StopAfterQueue`.

Use existing Material vector icons and the app's `HausColors` styling. Do not use emoji/text glyphs as icons. Controls must have clear `contentDescription` values that include the current mode or next action.

The UI should call controller methods such as `toggleShuffleMode()` and `cycleRepeatMode()` rather than mutating state directly.

## Test strategy

Add common JVM tests around `PlaybackController` / pure helper behavior:

- Default repeat/shuffle state is `StopAfterQueue` / `Off`.
- `RepeatOne` completion replays the same track from the beginning and remains playing.
- `RepeatPlaylist` completion wraps from last effective track to first.
- `StopAfterCurrent` completion stays on current track at end and does not advance.
- `StopAfterQueue` completion advances through middle tracks but stops on the final track at end.
- Manual next/previous follows original order when shuffle is off and does not wrap except in `RepeatPlaylist`.
- Enabling shuffle creates an effective order containing all tracks exactly once while preserving the current track as the active position.
- Manual next/previous and automatic completion follow shuffled order when shuffle is on.
- Disabling shuffle returns navigation to original queue order from the current track.
- Updating the queue while shuffle is on regenerates order and preserves the selected/current track when present.

## Verification

- Validate the OpenSpec change with `openspec validate playback-repeat-shuffle --strict`.
- Run focused common playback tests.
- Run relevant shared JVM compile/test checks.
- Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, or record exact blockers.
