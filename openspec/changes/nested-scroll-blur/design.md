# Design: Nested Scroll Blur

## Summary

The change adds a common Compose scroll chrome layer to pages with `LazyColumn` content. The layer is rendered above the list and becomes stronger as content scrolls underneath it.

## Dependency Decision

The roadmap mentions Backdrop or Haze. This change uses Haze via the common multiplatform artifact `dev.chrisbanes.haze:haze` so the shared Compose UI can use real `hazeSource`/`hazeEffect` APIs across Android, iOS, and desktop.

## Behavior

- `LibraryScrollPosition` drives a pure `NestedScrollChromeState`.
- At top of list: transparent chrome, no header offset.
- During early scroll: chrome opacity/blur/divider increase progressively.
- After enough scroll: chrome is fully active and stable.

## Risks

- Visual blur support may vary by platform. If a target renders blur differently, the overlay still degrades to a translucent scrim and divider.
- This does not implement a full Material3 TopAppBar scroll behavior; it preserves RhythHaus custom layout and only adds the nested-scroll visual treatment.
