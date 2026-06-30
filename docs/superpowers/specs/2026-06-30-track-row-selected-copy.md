# Track Row Selected Copy Spec

## Decision

Replace the selected-row debug copy with `Now playing`.

## Requirements

- Selected `TrackRow` must show `Now playing`.
- The phrase `queued on shared UI` must not appear in user-visible shared UI.
- The animated selection percentage must not appear in user-visible shared UI.
- Existing row selection styling, click behavior, metadata display, and duration display must remain unchanged.

## Non-goals

- No equalizer animation.
- No playback-state model changes.
- No queue semantics changes.
