## Context

Album and artist drill-down pages share `DrillDownView`. The original change replaced Miuix-owned artwork geometry with an app-owned nested-scroll state, but three live macOS attempts failed. Raw tracing showed that the z-indexed artwork chrome formed a separate hit-test branch, and Compose Desktop can reject wheel deltas at a `LazyListState` boundary before nested-scroll dispatch. Therefore `onPreScroll` and `onPostScroll` cannot guarantee artwork-zone input or reverse restoration.

A disposable desktop prototype removed that boundary: one `LazyColumn` owned all vertical input, artwork was lazy content, and restoration was `scrollToItem(0, 0)`. The user physically verified artwork-zone trackpad scrolling, deep reverse restoration, programmatic top restoration, and back interaction. This evidence validates input topology, not final product visual fidelity.

## Goals / Non-Goals

**Goals:**

- Make one `LazyColumn` and its `LazyListState` the sole vertical input owner on artwork-backed drill-down pages.
- Represent exactly `expandedArtworkHeight - collapsedChromeHeight` pixels of collapse before track rows scroll normally.
- Derive artwork progress and visual treatment from list position without mutable collapse state or layout feedback.
- Preserve a seamless square artwork image at rest and a pinned collapsed artwork toolbar after the collapse range is exhausted.
- Preserve the current Miuix behavior for no-artwork pages.

**Non-Goals:**

- Redesign artwork, title chips, rows, scrollbar, navigation, or playback.
- Add nested-scroll interception, sibling vertical scrollables, dynamic content padding, platform pointer controllers, parallax, snap, spring, or timing-based collapse.
- Upgrade, fork, or modify Miuix or Compose.
- Retain the disposable desktop prototype as product functionality.

## Decisions

### Use one lazy item sequence as the scroll model

For artwork-backed pages, the lazy sequence is:

1. a non-sticky upper artwork slice with height `collapseRange = expandedHeight - collapsedHeight`;
2. a sticky lower artwork slice with height `collapsedHeight` that becomes the collapsed toolbar;
3. the existing section label;
4. the existing keyed track rows;
5. the existing Now Playing bottom spacer.

At rest, the two artwork slices total the available width and form one square. The first `collapseRange` pixels of list travel remove only the upper slice. The lower slice then pins at collapsed height while rows scroll normally. Reverse list movement restores the same sequence naturally.

When `expandedHeight <= collapsedHeight`, omit the upper slice and render one collapsed sticky artwork/chrome item. The normalized progress is one and no invalid range or division occurs.

### Render one image through two aligned clips

Both slices render aligned portions of the same fixed-size square image. The upper slice displays source y-offset zero. The lower slice clips the same square after translating it by `-collapseRange`, so the seam is continuous at rest and during movement. Each slice uses the same crop geometry and existing scrim treatment; neither independently crops the bytes.

The large title treatment belongs to the lower artwork slice near its expanded-state bottom edge. The collapsed title/background treatment also belongs to that sticky slice and derives alpha from progress. A continuously available back button may remain as a button-sized safe-inset overlay above the list, but no full-size chrome overlay may cover the artwork input region or own vertical scrolling.

The sticky lower slice renders a solid `HausColors.paper` background above its aligned artwork. Its opacity equals clamped collapse progress: transparent at full expansion, progressive during collapse, and fully opaque when pinned. The artwork toolbar does not use `LayerBackdrop`, `drawBackdrop`, or Miuix blur. This keeps the chrome inside the sticky lazy item, avoids backdrop capture recursion and measured sibling-overlay state, and adds no scroll or hit-test owner. The existing bottom/Now Playing bar behavior is outside this artwork-chrome change and remains unchanged.

### Derive progress without controlling scroll

Collapse progress is read-only derived state:

- zero/inverted range: `1f`;
- first visible item is the upper slice: clamp its scroll offset to the collapse range and divide by that range;
- any later first visible item: `1f`.

The derivation never writes state, changes item sizes, or invokes scrolling. `scrollToItem(0, 0)` fully restores valid artwork ranges because item zero is the upper slice. No separate collapse offset, `expandFully()`, nested-scroll adapter, translated viewport, or dynamic top padding remains.

### Preserve branch and direct-navigation behavior

No-artwork pages retain the existing Miuix connection, glass chrome, large title, divider, and padding behavior. The custom scrollbar remains a direct-navigation control rather than a vertical scroll owner. Its top target calls `scrollToItem(0, 0)` in artwork mode; other index targeting remains unchanged unless separately specified.

## Rejected Alternatives

- **Shared `LazyListState` across sibling scrollables:** synchronizes position but creates independent input owners and fails macOS wheel-boundary delivery.
- **Parent nested-scroll interception:** cannot receive Desktop wheel deltas rejected before nested-scroll dispatch.
- **Full-size non-scrollable artwork overlay:** still wins hit testing and recreates the dead artwork zone.
- **Platform pointer controller:** would reimplement wheel, trackpad, velocity, focus, and accessibility behavior without need after the one-owner prototype passed.

## Risks / Trade-offs

- **Artwork seam mismatch:** lock the shared image placement and manually inspect the seam at compact and wide widths.
- **Sticky-header visual differences:** verify title chips, scrim, background fade, safe insets, and 44 dp back target in partial and fully collapsed states.
- **Solid transition contrast:** verify progressive paper opacity and title/back contrast at expanded, partial, and pinned states.
- **Scrollbar range approximation:** preserve existing index targeting and require exact top restoration; do not claim pixel-perfect total-content mapping.
- **No-artwork regression:** retain explicit branch tests and unchanged Miuix wiring.
- **Cross-platform input differences:** run common/JVM/Android/iOS compilation tests and require physical macOS trackpad acceptance on the production screen.
