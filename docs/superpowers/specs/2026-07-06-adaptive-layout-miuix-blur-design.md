# Adaptive Layout and Miuix Blur Design

## Summary

RhythHaus will add an adaptive shared Compose layout for tablets, desktop JVM/macOS, and other wide screens while preserving the existing phone layout. The implementation initially tried `top.yukonga.miuix.kmp:miuix-navigation3-adaptive:0.8.5`, but final Android verification proved it incompatible with current Miuix artifacts. Per user direction, the final implementation removes `miuix-navigation3-adaptive` completely, uses an in-project two-pane shell, and updates Miuix UI/blur to `0.9.3`. The same change replaces the current Kyant Backdrop glass wrapper with Miuix blur so the glass/blur stack comes from Miuix rather than Kyant.

## Current context

- RhythHaus uses shared Compose UI under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.
- `LibraryHomeScreen` in `App.kt` owns `LibraryNavigationStack`, root `AnimatedContent` route rendering, predictive back wiring, the fixed root `NowPlayingBar`, and the Now Playing expand overlay.
- `DrillDownView` renders album/artist track lists as full-screen routes with its own nested-scroll chrome and bottom bar.
- `LiquidGlassChrome.kt` currently wraps Kyant Backdrop APIs: `rememberLayerBackdrop`, `layerBackdrop`, `drawBackdrop`, `vibrancy`, `blur`, and `lens`.
- `NowPlayingBar.kt` and `NestedScrollBlurChrome` consume that wrapper and currently type their backdrop as Kyant `LayerBackdrop`.
- Maven evidence from this design session: `miuix-navigation3-ui:0.9.2` and `0.9.3` do not contain `androidx.navigation3.adaptive` APIs. The adaptive APIs exist in separate `miuix-navigation3-adaptive:0.8.5`, whose metadata requests older Miuix/Nav3 dependencies. Final Android verification confirmed those older transitive Miuix dependencies are not shippable beside current Miuix UI.

## Goals

- Improve tablet/desktop UI by showing Library list content and album/artist detail content side by side on wide screens.
- Preserve the current compact phone UI and behavior.
- Remove `miuix-navigation3-adaptive:0.8.5` completely after its Android duplicate-class conflict is proven.
- Keep Miuix UI/blur on the current approved line (`0.9.3`).
- Replace Kyant Backdrop/Shapes usage with Miuix blur.
- Preserve existing playback, scanner, route stack, search, settings, clear-library dialog, Now Playing expand/collapse, bottom bar controls, gestures, status-bar coverage, and glass single-layer behavior.

## Non-goals

- No full migration from `LibraryNavigationStack` to Navigation3 route ownership.
- No native Android/iOS/macOS navigation migration.
- No Windows/Linux product or packaging scope.
- No redesign of Now Playing, Search, Settings, scanner, library persistence, playback, or media controls.
- No downgrade of existing Miuix UI modules to `0.8.5` without explicit user approval.
- No reintroduction of Haze.

## Design

### Dependency strategy

Add version-catalog aliases for:

- `miuix-blur = top.yukonga.miuix.kmp:miuix-blur:0.9.3`, using the existing `miuix` version reference.
- No `miuix-navigation3-adaptive` alias in the final dependency graph.

The plan verified dependency resolution before relying on adaptive APIs. Because the adaptive artifact metadata requests older modules and final Android verification produced duplicate classes, the implementation uses a local two-pane shell instead of the external adaptive artifact.

### Miuix blur wrapper

Replace `LiquidGlassChrome.kt` internals with Miuix blur APIs while keeping a RhythHaus-local wrapper surface. The wrapper should expose names close to the current call sites so changes stay contained:

- `rememberRhythHausBackdrop()` backed by `top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()`.
- `Modifier.recordRhythHausBackdrop(backdrop)` backed by `Modifier.layerBackdrop(backdrop)`.
- `Modifier.rhythHausLiquidGlass(...)` or a renamed local equivalent backed by Miuix `Modifier.drawBackdrop(...)` and `BackdropEffectScope.blur(...)`.

The wrapper must continue to draw a fallback/tint surface so unsupported runtime shader paths remain readable. It should drop Kyant-specific effects such as `lens`/refraction if Miuix blur does not provide an equivalent; preserving readable frosted blur and unified tint is more important than preserving the exact Kyant lens effect. Because Android minSdk remains 29 while `miuix-blur` declares minSdk 33, the app manifest uses `tools:overrideLibrary="top.yukonga.miuix.kmp.blur"`; the wrapper must gate backdrop creation/recording with `isRenderEffectSupported()` and blur application with `isRuntimeShaderSupported()`.

After successful replacement, remove Kyant Backdrop/Shapes dependencies from `shared/build.gradle.kts` and the version catalog.

### Adaptive layout model

Add a small pure layout helper near `LibraryNavigation.kt`, for example:

- `LibraryAdaptiveLayoutMode.Compact`
- `LibraryAdaptiveLayoutMode.ListDetail`
- `libraryAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float): LibraryAdaptiveLayoutMode`

The thresholds should match the Miuix adaptive helper semantics:

- List-detail when width is at least `840.dp`, or when width is at least `600.dp` and the window is landscape enough (`height / width < 1.2`).
- Compact otherwise.

This pure helper gives tests stable behavior independent of Compose `LocalWindowInfo`.

### Wide list-detail rendering

On compact screens, keep the existing one-pane `AnimatedContent` route rendering and bottom bar arrangement.

On wide screens:

- Use an in-project two-pane shell, because `miuix-navigation3-adaptive:0.8.5` is not Android-shippable beside current Miuix artifacts.
- List pane: render the Library/Home content surface.
- Detail pane: render album detail or artist detail when the selected/current route is `AlbumDetail` or `ArtistDetail`.
- Placeholder: render a simple RhythHaus-branded empty detail state when no album/artist detail is selected.
- Search, Settings, Clear Library dialog, and Now Playing remain overlays or single-surface UI on top of the adaptive shell for this slice.

To avoid duplicating all of `App.kt`, the implementation should extract focused composables only where necessary:

- A home/list pane composable that contains the current Library header, import/scanning cards, browse mode picker, album/artist/song list, scroll backdrop recording, and nested-scroll chrome.
- A detail-pane composable that reuses the existing `DrillDownView` behavior where possible.

Wide-screen album/artist selection should prefer `replaceTop` between detail routes so choosing another album/artist swaps the detail pane instead of creating an awkward stack of wide-screen details. Compact selection keeps current push behavior.

### Back and route behavior

- Compact behavior remains unchanged.
- Wide list-detail behavior keeps `LibraryNavigationStack` as source of truth.
- Back from Search/Settings/Clear Library still returns to the origin route.
- Back from a visible detail route in wide mode returns the detail pane to placeholder/Home state.
- Now Playing expand/collapse continues to be controlled by existing overlay state, not by the adaptive scaffold.

## Risks and mitigations

- `miuix-navigation3-adaptive:0.8.5` may not compile cleanly with `miuix-ui:0.9.2` and current Navigation3/runtime dependencies. Mitigation: add dependency resolution as the first implementation task and stop on incompatible output rather than silently downgrading.
- `App.kt` is already large. Mitigation: extract only adaptive/home/detail shell pieces needed for this change; avoid broad unrelated refactors.
- Blur effect details may differ from Kyant Backdrop. Mitigation: preserve RhythHaus fallback color/tint constants and verify compile/runtime-safe API use; manual visual tuning can follow if needed.
- Backdrop/self-recording crashes can reappear if blur surfaces are recorded into their own backdrop. Mitigation: keep the current pattern: record non-glass content layers and render blur chrome/bars as overlay siblings.

## Acceptance criteria

- Compact phone width renders the current one-pane UI and preserves current route/back/playback/search/settings behavior.
- Wide tablet/desktop width renders Library list and album/artist detail side by side.
- `miuix-navigation3-adaptive:0.8.5` blocker is recorded, the dependency is removed, and the local two-pane shell preserves adaptive behavior.
- Current Kyant Backdrop/Shapes dependencies and imports are removed after Miuix blur replacement succeeds.
- Top nested-scroll chrome and bottom `NowPlayingBar` use Miuix blur while retaining unified tint, bounded status-bar coverage, single bottom-bar layer, controls, gestures, and readable fallback surface.
- Focused common tests cover adaptive layout-mode thresholds and unchanged navigation behavior.
- `openspec validate adaptive-layout-miuix-blur --strict` passes.
- Relevant Gradle verification passes, including shared JVM tests, desktop compile, Android debug assemble, and iOS simulator tests, or exact blockers are recorded.
