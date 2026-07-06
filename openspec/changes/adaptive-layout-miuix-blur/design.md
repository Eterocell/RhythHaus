# Design: Adaptive Layout with Miuix Blur

## Dependency strategy

Add `miuix-blur:0.9.2` and try `miuix-navigation3-adaptive:0.8.5` through the version catalog. Keep `miuix-ui` and other existing Miuix modules on `0.9.2`.

The implementation must verify Gradle resolution before depending on adaptive APIs. `miuix-navigation3-adaptive:0.8.5` metadata requests older Miuix/Nav3 modules, so the first implementation task must prove that the project can compile while keeping current `0.9.2` modules. If it cannot, stop with exact output and ask before downgrading or replacing the adaptive implementation.

## Miuix blur replacement

Keep a RhythHaus-local wrapper around blur recording/drawing so call sites stay contained. Replace Kyant `LayerBackdrop`, `layerBackdrop`, `rememberLayerBackdrop`, `drawBackdrop`, `vibrancy`, `blur`, and `lens` imports with Miuix blur equivalents:

- `rememberLayerBackdrop`
- `layerBackdrop`
- `drawBackdrop`
- `BackdropEffectScope.blur`
- fallback/tint drawing through `onDrawSurface`

Miuix blur does not need to preserve the exact Kyant lens/refraction effect. It must preserve readable frosted chrome, shared app tint constants, single-layer bottom bar behavior, and the current no-self-recording backdrop structure.

## Adaptive layout

Add a pure `LibraryAdaptiveLayoutMode` helper so behavior is testable:

- Compact below wide thresholds.
- ListDetail at width >= 840dp, or width >= 600dp with height / width < 1.2.

Use compact mode to render the current one-pane root route animation unchanged.

Use list-detail mode to render:

- list pane: current Library/Home browse content;
- detail pane: album detail or artist detail when selected;
- placeholder pane: an empty detail prompt when no album/artist is selected.

If `ListDetailPaneScaffold` from `androidx.navigation3.adaptive` compiles with the dependency strategy, use it for the wide shell. Search, Settings, Clear Library dialog, and Now Playing remain overlay/single-surface behavior in this slice.

Wide album/artist selection should swap the detail pane with `replaceTop` when already showing a detail route. Compact selection keeps current push behavior.

## Verification

- Dependency resolution/compile gate before broad refactor.
- Common tests for adaptive layout thresholds.
- Existing navigation tests to prove stack semantics still hold.
- `openspec validate adaptive-layout-miuix-blur --strict`.
- Gradle verification for shared JVM, desktop, Android, and iOS simulator, or exact blockers.
