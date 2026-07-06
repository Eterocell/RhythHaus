# Design: Adaptive Layout with Miuix Blur

## Dependency strategy

Use the current Miuix UI/blur line (`0.9.3`). The implementation initially tried `miuix-navigation3-adaptive:0.8.5`, but Android broad verification proved it transitively brings `miuix-android:0.8.5` beside `miuix-ui-android:0.9.x`, causing duplicate `top.yukonga.miuix.kmp.*` classes. Per user direction, remove `miuix-navigation3-adaptive` completely and keep the wide shell in project code.

`miuix-blur` declares Android minSdk 33 while RhythHaus still ships minSdk 29. The Android app manifest therefore overrides `top.yukonga.miuix.kmp.blur`, and blur usage must be runtime-gated: use `isRenderEffectSupported()` before creating/recording the backdrop scaffold, and use `isRuntimeShaderSupported()` before applying `blur(...)` or other RuntimeShader-backed effects. Unsupported paths must draw the fallback/tint surface only.

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

Use an in-project two-pane shell for the wide layout. Search, Settings, Clear Library dialog, and Now Playing remain overlay/single-surface behavior in this slice.

Wide album/artist selection should swap the detail pane with `replaceTop` when already showing a detail route. Compact selection keeps current push behavior.

## Verification

- Dependency resolution/compile gate before broad refactor.
- Common tests for adaptive layout thresholds.
- Existing navigation tests to prove stack semantics still hold.
- `openspec validate adaptive-layout-miuix-blur --strict`.
- Gradle verification for shared JVM, desktop, Android, and iOS simulator, or exact blockers.
