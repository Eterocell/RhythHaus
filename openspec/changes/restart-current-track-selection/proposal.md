## Why

Clicking the currently playing song in a Library track list currently toggles play/pause, so an ordinary row selection can unexpectedly pause playback. Track rows should behave as selection controls: selecting the active song restarts it from the beginning and keeps playback active.

## What Changes

- Make Library track-row selection restart the current track at position zero and play it.
- Keep selection of a different track loading that track from the relevant visible track list and playing it.
- Apply the same behavior to Library home song rows and album/artist drill-down rows.
- Preserve dedicated play/pause controls, queue order, shuffle/repeat settings, Now Playing behavior, and platform playback-engine contracts.

## Capabilities

### New Capabilities
- `library-track-selection-playback`: Defines playback behavior when users select current and non-current tracks from Library lists.

### Modified Capabilities

None.

## Impact

- Shared playback-controller selection behavior and shared Compose Library click wiring.
- Shared common tests for controller and Library selection policy behavior.
- No dependency, persistence, database, platform engine, media-session, or navigation changes.
