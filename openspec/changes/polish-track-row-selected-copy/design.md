# Design: Track Row Selected Copy Polish

## Decision

When a `TrackRow` is selected, show a short user-facing status label: `Now playing`.

## Rationale

The existing label includes implementation language (`shared UI`) and an animated alpha percentage. Users need to understand why the row is highlighted, not see internal animation state. `Now playing` matches the selected-row role in the current UI: the selected row is the active track candidate/current track in the list and is paired with the now-playing bar.

## UI behavior

- Non-selected rows keep their current title, artist/album metadata, artwork/mark, duration, colors, border, and click behavior.
- Selected rows keep their existing visual highlight and additionally render `Now playing` in the same location as the old debug copy.
- The old animation percentage must not be visible.
- `selectionAlpha` should be removed if it is no longer needed.

## Verification

- Source search must find no remaining `queued on shared UI` text.
- Shared JVM compilation must pass.
- Broad JVM/desktop/Android verification should pass or record existing flakes precisely.
