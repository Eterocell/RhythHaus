# Track List Artwork Collapse Design

## Summary

RhythHaus will make album and artist drill-down artwork collapse progressively and remain spatially coupled to the track list. On artwork-backed pages, upward drag first moves the square artwork and track content together, pixel for pixel, until the artwork reaches the collapsed top-bar height. Normal track-list scrolling begins after that range is exhausted. Pages without artwork retain their existing Miuix large-title behavior.

## Problem and Root Cause

`DrillDownMiuixScrollChrome` currently maps Miuix `ScrollBehavior.state.collapsedFraction` onto the entire distance between a square artwork header and the collapsed toolbar. `DrillDownView` independently reserves a fixed `maxWidth + 20.dp` top inset for the list.

Miuix 0.9.3 derives its collapse range from the measured large-title column, not from app-owned artwork. In artwork mode RhythHaus passes empty `title` and `largeTitle` values to the Miuix `TopAppBar`, so its measured collapse range is much shorter than the square artwork range. A small upward gesture can therefore drive the fraction to one and collapse the artwork completely, while the list retains its full static top reservation. The two geometries diverge and expose a large gap between the collapsed bar and the tracks.

The defect was introduced when artwork was added as an external overlay driven by the existing Miuix title-collapse fraction. Miuix exposes no supported multiplier for extending that fraction to an external header and does not measure or reserve layout space for arbitrary artwork.

## Interaction Design

For album and artist drill-down pages with representative artwork:

1. The expanded artwork height is the available drill-down width, preserving the existing square presentation.
2. The collapsed height is the system-bar top inset plus the existing 56 dp toolbar height.
3. The app-owned collapse range is `expandedArtworkHeight - collapsedChromeHeight`, clamped to a non-negative value.
4. Upward drag is consumed one for one by this range before the `LazyColumn` scrolls.
5. Downward drag restores the range symmetrically after the list reaches its start.
6. At every intermediate position, artwork height and track-content placement derive from the same remaining header height.

This produces a pinned, coordinated collapse rather than a timed animation or a proportional scroll-sharing effect. The content never reserves more vertical space than the visible header requires.

## Architecture and State Ownership

Introduce a small app-owned artwork-collapse policy and state for artwork-backed drill-down pages. It owns:

- the measured expanded and collapsed heights;
- the current consumed collapse offset in pixels;
- clamping when geometry changes;
- conversion from the current offset to remaining header height and normalized visual progress;
- nested-scroll consumption for upward collapse and downward expansion.

The policy arithmetic must be pure and independently testable. Compose owns the mutable offset and adapts it to a nested-scroll connection; it must not maintain a second independent content-padding or artwork-height state.

`DrillDownView` uses the resulting remaining header height to place the first list content. `DrillDownMiuixScrollChrome` uses the same result for artwork height, title-chip alpha, background fade, and back-button background. Miuix remains the renderer for the top bar, navigation icon, and existing title treatment, but its title-derived `collapsedFraction` no longer owns external artwork geometry.

For drill-down pages without artwork, retain the current `MiuixScrollBehavior`, `nestedScrollConnection`, large title, glass chrome, content padding, and divider behavior unchanged. The new state is not created or attached in that branch.

## Geometry and Resize Behavior

All collapse distances are derived from measured or declared layout geometry; no guessed pixel threshold, animation duration, or scroll multiplier is introduced.

When the window width, safe-area inset, density, or orientation changes, recompute the range and clamp the current offset into the new range. Preserve the nearest valid visual state rather than allowing an offset outside the new geometry. If the expanded height is less than or equal to the collapsed height, the range is zero and the page renders in the collapsed state without division by zero or nested-scroll consumption.

The list and chrome must consume one shared geometry value exactly once. System-bar and toolbar space must not be added again through a separate fixed top reservation.

## Preserved Behavior

The change preserves:

- album and artist routes, navigation transitions, predictive/back gestures, and safe-start navigation inset;
- square representative artwork, lazy Coil loading, crop behavior, scrim, title chips, and collapsed background fade;
- track ordering, row selection, visible-queue playback, scrollbar, spacing, and Now Playing bottom padding;
- no-artwork glass chrome and Miuix large-title behavior;
- existing accessibility labels and the 44 dp back-button target;
- Miuix 0.9.3 and all current dependencies.

## Error Handling and Accessibility

Artwork loading failure continues to use the existing no-placeholder rendering behavior; it does not change route state or playback. Geometry is valid independently of whether image decoding has completed because artwork-backed mode is determined by the existing representative artwork identity or eager bytes.

The collapse logic introduces no asynchronous failure path. Invalid or changing measurements are clamped safely. The top bar and track content remain reachable throughout the collapse, semantics are not removed, and the back button retains its safe-area inset and minimum target size.

## Testing

Use strict RED/GREEN TDD for the root-cause regression.

Pure common tests cover:

- fully expanded, partially collapsed, and fully collapsed geometry;
- one-to-one upward consumption before list scrolling;
- symmetric downward expansion at the list start;
- clamping beyond either boundary;
- zero or inverted collapse ranges;
- window-width or inset changes that shrink or enlarge the valid range;
- equality between visible chrome height and the list's reserved top position at every tested offset.

Focused integration or source-level policy tests cover:

- artwork-backed drill-down pages selecting the app-owned connection and shared geometry;
- no-artwork pages retaining the Miuix connection and existing top-padding policy;
- album and artist pages using the same behavior;
- title/background visual progress deriving from the app-owned artwork progress.

Verification commands:

- focused common tests for the new collapse policy and Library navigation/layout behavior;
- `openspec validate track-list-artwork-collapse --strict`;
- `./gradlew :shared:jvmTest --configuration-cache`;
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`;
- `/usr/bin/xcrun xcodebuild -version`;
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, recording any pre-existing common-test blocker exactly;
- `GIT_MASTER=1 git diff --check`.

Manual visual QA remains required on album and artist pages with and without artwork, at compact and wide widths. It must confirm pixel-following collapse, absence of a transient gap, smooth reverse expansion, title/background transitions, back-button interaction, and normal list scrolling after full collapse.

## Non-Goals

- No artwork, row, title-chip, scrollbar, or general top-bar redesign.
- No changes to playback, queue construction, navigation, scanning, persistence, or platform media integration.
- No Miuix upgrade, replacement, fork, or unsupported modification of its internal state.
- No spring, fling, snap, parallax, arbitrary duration, or scroll-distance multiplier for the artwork collapse.
- No Windows or Linux product or packaging support.
