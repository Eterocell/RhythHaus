# Miuix v0.9.4-rc01 research

Research date: 2026-09-03.

## Repository status

- `gradle/libs.versions.toml` already pins Miuix to `0.9.4-rc01`.
- `:core:ui:jvmRuntimeClasspath` resolves exactly `top.yukonga.miuix.kmp:miuix-ui:0.9.4-rc01` and `top.yukonga.miuix.kmp:miuix-blur:0.9.4-rc01` alongside Compose Multiplatform `1.12.0`.
- Production Gradle files declare `miuix-ui`, `miuix-blur`, and `miuix-preference`; no dependency or import refers to either `miuix0-navigation3-ui` or `miuix-navigation3-ui`. The app uses its own `LibraryNavigationStack` / `LibraryAppState` route and Back arbitration, plus AndroidX Navigation Event for platform Back events. There is therefore no legacy navigation artifact to replace.
- `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt` is the single Miuix backdrop boundary. It currently calls `drawBackdrop { blur(blurRadius.toPx()) }` with a uniform blur. Its three callers are the drill-down chrome and Now Playing bar. No production code uses `progressiveBlur` or `ProgressiveBlur`.
- `shared/src/commonMain/composeResources/files/aboutlibraries.json` still declares Miuix artifacts at `0.9.3`; this is stale user-facing metadata despite the resolved runtime upgrade.

## Upstream changes

The signed `v0.9.4-rc01` release adds `top.yukonga.miuix.kmp:miuix-nav`, replacing the removed `miuix-navigation3-ui`. This is a new navigation runtime, not a rename: it uses `NavKey`, `rememberNavBackStack`, `NavDisplay`, and optionally `NavController`, and has no AndroidX Navigation3 dependency. RhythHaus does not consume the removed artifact, so adding `miuix-nav` would be an unrelated replacement of the app's authoritative routing/Back model.

`miuix-blur` adds a true progressive blur API:

```kotlin
fun BackdropEffectScope.progressiveBlur(
    radiusX: Float,
    radiusY: Float = radiusX,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
)
```

The radii are pixels. `ProgressiveBlur` describes a full-strength-to-clear ramp and supplies `Top`, `Bottom`, `Left`, and `Right` presets. To retain a pixel-sharp clear endpoint, `drawBackdrop` must receive the same `progressiveGradient` used by `progressiveBlur`; `progressiveTextureBlurEffect` is the upstream helper for that pairing.

The release also raises Miuix's Android minimum from 23 to 24. RhythHaus's Android minimum is already 29, so that change is compatible. The existing Android runtime shader/render-effect gating remains necessary for unsupported rendering paths.

## Recommended adoption

Adopt progressive blur only for the scroll-adjacent top drill-down chrome. Its top edge benefits from a `ProgressiveBlur.Top` falloff: retained blur at the system-bar edge, continuous fade into sharp scrolling content below. Keep uniform blur for the floating Now Playing bar and dialogs; a one-edge top gradient would be visually incorrect for those bounded, floating surfaces.

Expose the effect as an explicit optional core-ui wrapper parameter rather than changing the global default. That preserves all existing callers and avoids accidentally applying a top-edge gradient to the Now Playing bar. Add a focused production-composable regression around the chosen gradient and fallback behavior, update the checked-in AboutLibraries Miuix versions through the repository's existing generator path, and leave the routing stack unchanged.

## Navigation feasibility outcome

Full `miuix-nav` adoption is not safe in `0.9.4-rc01`. Its public `NavDisplay` unconditionally installs `PredictiveBackHandlerWithSessions` when the stack has more than one entry. It starts a route transition from system predictive-Back progress and invokes `onBack` only on commit; its public overload has no platform-Back opt-out. RhythHaus must instead let Shared choose modal, edit, active-page selection, Now Playing, or route before any route preview starts. Do not add `miuix-nav` until upstream offers an explicit platform-Back disable/delegation API.

## Primary sources

- [Miuix v0.9.4-rc01 release notes](https://github.com/compose-miuix-ui/miuix/releases/tag/v0.9.4-rc01)
- [`ProgressiveBlur` source at the release tag](https://raw.githubusercontent.com/compose-miuix-ui/miuix/v0.9.4-rc01/miuix-blur/src/commonMain/kotlin/top/yukonga/miuix/kmp/blur/ProgressiveBlur.kt)
- [`progressiveBlur` source at the release tag](https://raw.githubusercontent.com/compose-miuix-ui/miuix/v0.9.4-rc01/miuix-blur/src/commonMain/kotlin/top/yukonga/miuix/kmp/blur/BackdropEffects.kt)
- [`drawBackdrop` progressive-composite contract](https://raw.githubusercontent.com/compose-miuix-ui/miuix/v0.9.4-rc01/miuix-blur/src/commonMain/kotlin/top/yukonga/miuix/kmp/blur/DrawBackdropModifier.kt)
- [Miuix-nav guide](https://compose-miuix-ui.github.io/miuix/guide/miuix-nav.html)
