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

The Library drill-down track list uses Miuix `MiuixScrollBehavior().nestedScrollConnection` so the large title and collapsed title are owned by Miuix `TopAppBar` physics instead of RhythHaus' former `LazyListState`-derived overlay threshold.

The Library home screen can continue to use `NestedScrollBlurChrome` until its home-specific large header/import-card layout is explicitly migrated. The correction scope is the drill-down track-list behavior requested by the user.

The drill-down migration keeps RhythHaus' glass/backdrop shell custom by placing Miuix `TopAppBar(scrollBehavior = scrollBehavior, largeTitle = title, title = title)` inside the glass overlay, while the `LazyColumn` attaches `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`.

## Inset and Glass Rules

- Keep caller-owned status bar height via `rememberSystemBarTopPadding()`.
- Keep `chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight`.
- Do not let Miuix default system bar insets add extra top padding inside the overlay.
- Keep the glass overlay as a sibling outside the recorded content subtree, as currently implemented by the call sites.
- Use a transparent or glass-compatible app bar color so `rhythHausLiquidGlass` remains visible.

## Compatibility

Existing `RhythHausTopAppBar` usages in Search and Settings must keep the same default visual behavior after any wrapper extension. Drill-down track-list chrome now uses a direct Miuix `TopAppBar` with `MiuixScrollBehavior` instead of `DrillDownHeader`.

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

- Miuix `TopAppBar` internally applies top system bar padding in Miuix 0.9.3 source. The drill-down list must reserve enough top content padding for the expanded app bar and status area so content does not render beneath the large title.
- Removing the pulse dot changes visual personality. The user asked to migrate nested scroll to Miuix TopAppBar, so the standard Miuix title should be preferred unless manual QA later requests restoring the dot as an action/bottom content.
- Transparent app bar color must not create unreadable text over glass; use existing Haus ink/muted colors and current glass fallback.
