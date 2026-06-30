## Context

RhythHaus shared Compose UI currently has strong visual identity but several source-backed UX flaws. This change targets fixes that are small, shared-first, and compatible with the existing route stack and Miuix/Compose setup.

## Decisions

### Decision: Empty Home import CTA

Reuse the existing `ImportAudioCard` and show it on Home when `snapshot.tracks.isEmpty()`. Settings remains a valid place to manage music, but Home should provide the first-run Add music folder path directly.

### Decision: Small pure layout helper for album columns

Add a pure helper for album-grid column count and test it in common tests. The UI should use `BoxWithConstraints` to choose the column count and chunk rows at render time.

Breakpoints:

- width under 560dp: 2 columns
- 560dp to under 900dp: 3 columns
- 900dp and wider: 4 columns

### Decision: Add Songs browse mode only

Add `Songs` to `BrowseMode` and render the existing `TrackRow` for `snapshot.tracks`. A click should select that track and use the current full-library playable queue to start/toggle playback. This intentionally does not add folders, genres, or playlists yet.

### Decision: Search result selection returns to origin

Keep Search as a route overlay. When a user taps a result, Search sets the queue, starts playback, and calls `onDismiss()`. Back therefore returns to the route that opened Search with the bottom bar updated.

### Decision: Keep clear action simple

Add a trailing clear action to the existing BasicTextField container. It may use text/icon-like copy already available in Compose/Miuix; no dependency changes.

### Decision: Remove normal developer panels

Remove user-facing TagLib developer metadata panels from normal app surfaces and clean up dead developer-only code/imports if unused. This does not touch scanner metadata extraction, TagLib wrappers, or tests for metadata.

### Decision: Hit target adjustments are local

Increase `BackChip` height and bottom-bar Search/Settings boxes to at least 44dp. Keep icon glyph sizes, colors, and visual style close to current design.

## Risks / Trade-offs

- `App.kt` remains large. This batch avoids broad refactoring so review can focus on UX behavior.
- UI screenshot/device validation remains manual. Automated tests cover pure helpers and build/compile behavior.
- Songs mode uses current library order and full-library queue semantics to avoid queue architecture changes.
