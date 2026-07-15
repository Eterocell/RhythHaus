## Why

Album and artist Track List pages currently collapse square artwork using Miuix's much shorter large-title scroll range while retaining a fixed expanded list inset. A small upward gesture can therefore remove the artwork immediately and leave a large gap between the collapsed TopBar and the tracks.

## What Changes

- Add progressive, pixel-following artwork collapse to album and artist drill-down pages with representative artwork.
- Couple visible artwork height and track-content placement to one geometry-derived app-owned collapse state.
- Consume upward drag through the artwork range before normal track-list scrolling and restore it symmetrically after the list returns to its start.
- Preserve the existing Miuix large-title nested-scroll path for drill-down pages without artwork.
- Add pure common regression coverage for collapse, expansion, clamping, resize, and shared chrome/content geometry.

## Capabilities

### New Capabilities
- `track-list-artwork-collapse`: Defines coordinated nested-scroll behavior for artwork-backed album and artist Track List pages.

### Modified Capabilities

None.

## Impact

- Shared Compose UI under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui`.
- Common Library UI tests under `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui`.
- No public API, persistence schema, playback, navigation-model, platform integration, or dependency changes.
