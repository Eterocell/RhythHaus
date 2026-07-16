# Track List Artwork Collapse Design

## Summary

RhythHaus will make album and artist drill-down artwork collapse progressively through one native lazy-list scroll surface. On artwork-backed pages, the expanded square is represented by an upper scroll-away image slice plus a lower sticky collapsed-toolbar slice. Upward list travel removes exactly the square-to-toolbar range before rows scroll normally; reverse travel restores the same range naturally. Pages without artwork retain their existing Miuix large-title behavior.

## Problem and Root Cause

`DrillDownMiuixScrollChrome` currently maps Miuix `ScrollBehavior.state.collapsedFraction` onto the entire distance between a square artwork header and the collapsed toolbar. `DrillDownView` independently reserves a fixed `maxWidth + 20.dp` top inset for the list.

Miuix 0.9.3 derives its collapse range from the measured large-title column, not from app-owned artwork. In artwork mode RhythHaus passes empty `title` and `largeTitle` values to the Miuix `TopAppBar`, so its measured collapse range is much shorter than the square artwork range. A small upward gesture can therefore drive the fraction to one and collapse the artwork completely, while the list retains its full static top reservation. The two geometries diverge and expose a large gap between the collapsed bar and the tracks.

The defect was introduced when artwork was added as an external overlay driven by the existing Miuix title-collapse fraction. Miuix exposes no supported multiplier for extending that fraction to an external header and does not measure or reserve layout space for arbitrary artwork.

The first corrective design used app-owned nested scroll, but three live macOS fixes failed. Raw tracing showed that the z-indexed artwork chrome was a separate hit-test branch, while Compose Desktop could reject wheel deltas at `LazyListState` boundaries before nested-scroll callbacks ran. A disposable one-`LazyColumn` prototype then passed physical macOS checks for artwork-zone scrolling, deep reverse restoration, `scrollToItem(0, 0)`, and back interaction. The production design therefore removes nested-scroll ownership rather than patching it again. The prototype validates routing and restoration, not final visual fidelity.

## Interaction Design

For album and artist drill-down pages with representative artwork:

1. The expanded artwork height is the available drill-down width, preserving the existing square presentation.
2. The collapsed height is the system-bar top inset plus the existing 56 dp toolbar height.
3. The app-owned collapse range is `expandedArtworkHeight - collapsedChromeHeight`, clamped to a non-negative value.
4. One `LazyColumn` owns all vertical movement over artwork and rows.
5. Its first lazy range equals `expandedArtworkHeight - collapsedChromeHeight`, so upward travel advances the artwork/content boundary one for one before rows scroll normally.
6. Reverse list travel restores the same range, reaching full expansion at item zero offset zero.

This produces a pinned, coordinated collapse rather than a timed animation or a proportional scroll-sharing effect. The content never reserves more vertical space than the visible header requires.

## Architecture and State Ownership

Artwork-backed pages use one `LazyColumn` and `LazyListState` with this item order:

1. a non-sticky upper artwork slice with height `expandedHeight - collapsedHeight`;
2. a sticky lower artwork slice with height `collapsedHeight`;
3. the existing section label;
4. the existing keyed track rows;
5. the existing Now Playing bottom spacer.

The upper and lower slices clip one fixed square image placement. The lower slice translates that placement upward by the collapse range rather than independently cropping the image, keeping the seam continuous. It also owns the sticky title, scrim, and background fade. A button-sized back target remains at the safe inset without placing a full-size overlay over the artwork.

The sticky lower slice overlays a solid `HausColors.paper` background. Its opacity is the clamped collapse progress: zero at full expansion, progressive during collapse, and one when pinned. Artwork chrome does not use `LayerBackdrop`, `drawBackdrop`, or Miuix blur. Keeping the solid layer inside the sticky item avoids another input surface or measured sibling overlay. The existing bottom/Now Playing bar behavior remains unchanged.

Collapse progress is pure read-only state derived from `LazyListState.firstVisibleItemIndex` and `firstVisibleItemScrollOffset`. It never writes scroll state or changes item sizes. No mutable collapse offset, `NestedScrollConnection`, sibling `scrollable`, dynamic top padding, translated viewport, or `expandFully()` operation remains on the artwork path.

For drill-down pages without artwork, retain the current `MiuixScrollBehavior`, `nestedScrollConnection`, large title, glass chrome, content padding, and divider behavior unchanged. The new state is not created or attached in that branch.

## Geometry and Resize Behavior

All collapse distances are derived from measured or declared layout geometry; no guessed pixel threshold, animation duration, or scroll multiplier is introduced.

When the window width, safe-area inset, density, or orientation changes, recompute both slice heights and derive progress from the current lazy-list position under the new geometry. If the expanded height is less than or equal to the collapsed height, omit the upper slice and render the sticky lower slice in the collapsed state without division by zero.

The upper and lower artwork slices must total one expanded square exactly once. System-bar and toolbar space must not be added again through separate padding or an external reservation.

## Preserved Behavior

The change preserves:

- album and artist routes, navigation transitions, predictive/back gestures, and safe-start navigation inset;
- square representative artwork, lazy Coil loading, crop behavior, scrim, title chips, and collapsed background fade;
- progressive sticky-toolbar solid paper background, transparent when expanded and opaque when pinned;
- track ordering, row selection, visible-queue playback, scrollbar, spacing, and Now Playing bottom padding;
- no-artwork glass chrome and Miuix large-title behavior;
- existing accessibility labels and the 44 dp back-button target;
- Miuix 0.9.3 and all current dependencies.

## Error Handling and Accessibility

Artwork loading failure continues to use the existing no-placeholder rendering behavior; it does not change route state or playback. Only resolved available artwork bytes select the artwork-backed lazy sequence; loading, unavailable, failed, and absent artwork retain the Miuix path.

The collapse logic introduces no asynchronous failure path. Invalid or changing measurements are clamped safely. The top bar and track content remain reachable throughout the collapse, semantics are not removed, and the back button retains its safe-area inset and minimum target size.

## Testing

Use strict RED/GREEN TDD for the root-cause regression.

Pure common tests cover:

- fully expanded, partially collapsed, and fully collapsed geometry;
- one-to-one progress from item-zero list offset before normal row scrolling;
- symmetric restoration as reverse list movement returns to item zero offset zero;
- clamping beyond either boundary;
- zero or inverted collapse ranges;
- window-width or inset changes that shrink or enlarge the valid range;
- aligned upper/lower slice geometry at expanded, partial, and collapsed positions;
- exact restoration through `scrollToItem(0, 0)` without separate collapse state.

Focused integration or source-level policy tests cover:

- artwork-backed drill-down pages selecting the single-list sequence and shared geometry;
- no-artwork pages retaining the Miuix connection and existing top-padding policy;
- album and artist pages using the same behavior;
- title/background visual progress deriving from the list-position artwork progress.
- solid paper opacity clamping at expanded, partial, and pinned progress.

Verification commands:

- focused common tests for the new collapse policy and Library navigation/layout behavior;
- `openspec validate track-list-artwork-collapse --strict`;
- `./gradlew :shared:jvmTest --configuration-cache`;
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`;
- `/usr/bin/xcrun xcodebuild -version`;
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, recording any pre-existing common-test blocker exactly;
- `GIT_MASTER=1 git diff --check`.

Manual visual QA remains required on album and artist pages with and without artwork, at compact and wide widths. It must confirm artwork-zone trackpad input, pixel-following collapse, a seamless image-slice boundary, absence of a transient gap, smooth deep reverse expansion, progressive solid-background opacity and contrast, title/background transitions, back-button interaction, exact scrollbar top restoration, unchanged bottom/Now Playing bar rendering, and normal row scrolling after full collapse.

## Non-Goals

- No artwork, row, title-chip, scrollbar, or general top-bar redesign.
- No changes to playback, queue construction, navigation, scanning, persistence, or platform media integration.
- No Miuix upgrade, replacement, fork, or unsupported modification of its internal state.
- No artwork nested-scroll adapter, sibling vertical scrollable, dynamic content padding, translated viewport compensation, full-size input-blocking overlay, or platform pointer controller.
- No spring, fling, snap, parallax, arbitrary duration, or scroll-distance multiplier for the artwork collapse.
- No Windows or Linux product or packaging support.
