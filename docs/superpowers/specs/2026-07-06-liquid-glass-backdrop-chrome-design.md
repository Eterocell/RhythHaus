# Liquid Glass Backdrop Chrome Design

## Summary

Replace the current scrim-only nested-scroll top chrome and the existing solid `NowPlayingBar` panel surface with Kyant0 Backdrop liquid-glass surfaces. The change keeps the current RhythHaus layout and interactions intact: the top chrome still appears only after scroll progress, remains bounded to the status bar plus toolbar height, and morphs the page heading into a toolbar title; the bottom bar keeps its current rounded card layout, playback/search/settings controls, hide/show animation, navigation-bar padding, tap-to-expand, and drag-up gesture.

## Current context

- `NestedScrollBlurChrome` lives in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and currently renders a bounded translucent scrim plus title/divider.
- `NowPlayingBar` lives in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt` and currently uses a Miuix `Surface` with `HausColors.current.panel`.
- The requested library is Kyant0 AndroidLiquidGlass, published as `io.github.kyant0:backdrop:2.0.0`; the examples use `rememberLayerBackdrop`, `Modifier.layerBackdrop(backdrop)`, and `Modifier.drawBackdrop(backdrop, shape = { ... }, effects = { vibrancy(); blur(...); lens(...) })`.
- Backdrop also requires `io.github.kyant0:shapes:1.2.0` for rounded/capsule shape helpers used by the effect API.

## Design

Add a small RhythHaus-local wrapper around Backdrop instead of scattering raw Backdrop calls through app UI. The wrapper will expose a shared backdrop recorder and a bounded glass-surface modifier/composable for RhythHaus chrome. It will centralize the effect recipe, including vibrancy, blur radius, lens/refraction values, and fallback surface color.

Each route root that needs glass records its main content layer once with `Modifier.layerBackdrop(backdrop)`. The top chrome and bottom bar then draw against that recorded layer. This keeps backdrop capture scoped to visible app content, avoids recording overlays into themselves, and follows the upstream usage pattern.

The top chrome glass only covers the current `statusBarHeight + 56.dp` toolbar box. At scroll progress zero it remains visually quiet and returns without drawing, matching current behavior. As progress increases it uses the same title fade and divider behavior, but the background becomes a Backdrop glass surface rather than a plain scrim.

The bottom bar uses the same Backdrop glass recipe on the rounded card container while preserving current content, progress bar, artwork thumbnail, text, controls, click behavior, gestures, and hide/show animation. The bottom bar shape remains `RoundedCornerShape(20.dp)` so this task is an effect replacement, not a layout redesign.

## Non-goals

- Do not redesign top-bar or bottom-bar layout, controls, spacing, route transitions, scroll visibility rules, playback behavior, scanner behavior, or navigation semantics.
- Do not migrate to native platform UI or platform-specific glass implementations.
- Do not add Haze or reintroduce Haze APIs.
- Do not add Windows/Linux scope.

## Risks and mitigations

- Backdrop render/runtime shader support may vary across Android, iOS, and desktop. The wrapper will always provide a translucent fallback surface via `onDrawSurface`, so unsupported platforms remain readable.
- A dependency version mismatch could break compilation. The plan must add dependencies through the version catalog and verify with shared JVM compile, Android debug assemble, desktop compile, and iOS simulator tests.
- Backdrop capture can accidentally include overlays if applied too high in the tree. The plan must record only the route content/background layer and draw the chrome/bar above it.

## Acceptance criteria

- Library/Home and album/artist track-list top chrome use Backdrop glass instead of the current scrim-only background.
- The bottom `NowPlayingBar` rounded card uses Backdrop glass instead of the current solid panel surface.
- Top chrome remains bounded to status bar plus toolbar height and still avoids covering the full screen.
- Existing NowPlayingBar behavior remains intact: empty-library mode, playback controls, Search/Settings controls, tap-to-expand, drag-up expand, navigation-bar padding, and hide/show-on-scroll.
- Existing list content, scanner, browse mode, playback, route navigation, and nested-scroll state behavior are preserved.
- Automated verification passes or blockers are recorded with exact command output.
