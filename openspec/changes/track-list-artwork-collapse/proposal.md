## Why

Album and artist Track List pages currently collapse square artwork using Miuix's much shorter large-title scroll range while retaining a fixed expanded list inset. A small upward gesture can therefore remove the artwork immediately and leave a large gap between the collapsed TopBar and the tracks.

## What Changes

- Add progressive, pixel-following artwork collapse to album and artist drill-down pages with representative artwork.
- Represent the artwork collapse range directly in one `LazyColumn` item sequence, making that list the sole vertical input owner.
- Derive artwork progress and chrome treatment from list position so upward scrolling traverses the artwork range before normal row scrolling and reverse scrolling restores it naturally.
- Preserve the existing Miuix large-title nested-scroll path for drill-down pages without artwork.
- Add pure common regression coverage for collapse progress, expansion, clamping, resize, aligned artwork slices, and shared chrome/content geometry.

## Capabilities

### New Capabilities
- `track-list-artwork-collapse`: Defines coordinated single-list artwork collapse behavior for artwork-backed album and artist Track List pages.

### Modified Capabilities

None.

## Impact

- Shared Compose UI under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui`.
- Common Library UI tests under `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui`.
- The disposable desktop prototype and its launch/test dependencies are removed after supplying implementation evidence.
- No public API, persistence schema, playback, navigation-model, platform integration, or dependency changes.
