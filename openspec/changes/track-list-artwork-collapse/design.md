## Context

Album and artist drill-down pages share `DrillDownView`. Artwork-backed pages render a square app-owned overlay in `DrillDownMiuixScrollChrome`, drive its height from Miuix `collapsedFraction`, and independently reserve `maxWidth + 20.dp` above the track list. Miuix 0.9.3 measures collapse from its large-title content and does not include external artwork, so these two geometries can diverge.

The approved interaction is a pinned coordinated collapse: upward movement removes the artwork range one pixel per pixel before the list scrolls, and downward movement restores the range after the list reaches its start.

## Goals / Non-Goals

**Goals:**

- Give artwork-backed drill-down pages one app-owned, geometry-derived collapse range.
- Drive artwork height, list placement, and visual progress from the same clamped offset.
- Preserve deterministic one-to-one nested-scroll consumption and symmetric expansion.
- Preserve the current Miuix behavior for no-artwork pages.
- Make arithmetic and branch selection testable in common tests.

**Non-Goals:**

- Redesign artwork, title chips, rows, scrollbar, navigation, or playback.
- Add parallax, snap, spring, fling, arbitrary multipliers, or timing-based collapse.
- Upgrade, fork, or modify Miuix.
- Change dependencies, persistence, platform integrations, or packaging.

## Decisions

### Use a pure geometry value plus a Compose-owned offset

`ArtworkCollapseGeometry` will hold expanded and collapsed pixel heights and expose a non-negative range, clamped offset, remaining header height, and normalized progress. Pure functions make boundary, resize, and coupling behavior testable without Compose instrumentation.

Compose will retain the mutable consumed offset. Each rendered snapshot is derived immediately from the latest geometry and a clamped current offset; a side effect persists any clamp after resize. This avoids a frame where chrome and content use stale geometry.

Alternative rejected: reuse Miuix `collapsedFraction`. Its range is measured from large-title content and cannot represent the external square artwork.

### Attach exactly one nested-scroll owner per branch

Artwork-backed pages attach an app-owned `NestedScrollConnection`. Negative `onPreScroll` consumes the remaining upward collapse range before the child. Positive `onPostScroll.available.y` expands the header only after the child cannot consume more movement toward the list start. Returned consumption preserves the input sign and equals the actual clamped offset change.

No-artwork pages attach only the existing Miuix connection. They keep the current large title, glass chrome, divider, and top-padding behavior.

Alternative rejected: attach both connections. Two owners would compete for the same deltas and make ordering dependent on modifier composition.

### Couple list placement and chrome through one snapshot

The artwork snapshot provides remaining header height and progress. `DrillDownView` uses the height for the list's current top placement, while `DrillDownMiuixScrollChrome` uses the same height and progress for artwork size, title chips, solid background, and back-button fill. There is no separate fixed expanded artwork reservation.

Alternative rejected: animate only artwork height while keeping static content padding. That is the current root cause and necessarily permits a gap.

### Derive all distances from current geometry

Expanded height remains the available width. Collapsed height remains the system-bar top inset plus 56 dp. A zero or inverted range renders at the collapsed height, consumes no nested scroll, and never divides by zero. Width, density, inset, or orientation changes recompute the range and clamp the current offset.

## Risks / Trade-offs

- **Dynamic `LazyColumn` top placement can cause layout churn during drag** → Keep the derived state minimal, use a single geometry snapshot, and verify smoothness through runtime visual QA on compact and wide widths.
- **Incorrect nested-scroll sign handling can reverse or over-consume movement** → Lock negative pre-scroll and positive post-scroll behavior with pure and focused adapter tests before UI wiring.
- **Resize can briefly expose stale geometry** → Derive the rendered snapshot from the latest geometry immediately and persist its clamp after composition.
- **No-artwork behavior could regress through shared wiring** → Keep explicit artwork/no-artwork branch selection and cover both policies in common tests.
- **Automated tests cannot prove perceived smoothness** → Require manual visual QA for album and artist routes, forward and reverse movement, compact and wide windows, and post-collapse list scrolling.
