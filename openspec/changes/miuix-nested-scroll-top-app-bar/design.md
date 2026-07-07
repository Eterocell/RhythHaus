# Design: Miuix Nested-Scroll TopAppBar

## Context

`NestedScrollBlurChrome` is the shared Library top overlay used by `LibraryHomeContent` and `DrillDownView`. It currently owns two separate concerns:

1. RhythHaus-specific glass overlay mechanics: status-bar-covering height, `rhythHausLiquidGlass`, recorded backdrop, and bottom divider.
2. Standard toolbar/title layout: a custom Row, pulse dot, and Text that fades in as scroll progress approaches the collapsed state.

This change migrates concern 2 to Miuix top app bar components while keeping concern 1 custom.

## Architecture

The implementation should keep `NestedScrollBlurChrome` as the overlay shell and place a Miuix compact top app bar inside its bottom toolbar area.

Preferred structure:

```text
NestedScrollBlurChrome
└── outer Box: height = statusBarHeight + 56.dp, glass background
    └── toolbar Box: height = 56.dp, align BottomCenter
        ├── RhythHausTopAppBar / SmallTopAppBar title, alpha = titleProgress
        └── 1.dp divider, alpha = progress
```

`RhythHausTopAppBar` may be extended with optional customization parameters so this nested-scroll use can pass a transparent/glass-compatible background and keep default insets disabled.

## Scroll Behavior Decision

Do not adopt Miuix `MiuixScrollBehavior.nestedScrollConnection` for Library lists in this change.

Reasons:

- Library already derives `NestedScrollChromeState` from `LazyListState`.
- That same list state also feeds scroll reporting and bottom-bar visibility logic.
- The current chrome is an overlay, not a layout-reserving app bar. Switching to Miuix nested scroll behavior directly would change list consumption/collapse physics and requires broader visual/runtime design.

The migration should therefore use Miuix `SmallTopAppBar` as a rendering component, driven by the existing RhythHaus scroll progress.

## Inset and Glass Rules

- Keep caller-owned status bar height via `rememberSystemBarTopPadding()`.
- Keep `chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight`.
- Do not let Miuix default system bar insets add extra top padding inside the overlay.
- Keep the glass overlay as a sibling outside the recorded content subtree, as currently implemented by the call sites.
- Use a transparent or glass-compatible app bar color so `rhythHausLiquidGlass` remains visible.

## Compatibility

Existing `RhythHausTopAppBar` usages in Search, Settings, and `DrillDownHeader` must keep the same default visual behavior after any wrapper extension.

Required existing API compatibility:

```kotlin
RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
)
```

Adding optional parameters after `subtitle` is acceptable. Reordering or removing existing parameters is not.

## Risks

- Miuix `SmallTopAppBar` internally applies top system bar padding even when `defaultWindowInsetsPadding = false` in Miuix 0.9.3 source. The implementation must verify the overlay does not double-cover the status bar or change total chrome height. If this behavior causes unavoidable double inset, use direct `SmallTopAppBar` in a bounded toolbar area with measurements adjusted, and record the reason.
- Removing the pulse dot changes visual personality. The user asked to migrate nested scroll to Miuix TopAppBar, so the standard Miuix title should be preferred unless manual QA later requests restoring the dot as an action/bottom content.
- Transparent app bar color must not create unreadable text over glass; use existing Haus ink/muted colors and current glass fallback.
