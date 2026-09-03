# Progressive Drill-Down Blur Design

Date: 2026-09-03
Status: proposed

## Context

`RhythHausBackdrop` and `Modifier.rhythHausLiquidGlass()` form the sole Miuix blur boundary in `:core:ui`. The modifier currently applies uniform `blur(radiusPx)` whenever a backdrop and runtime shaders are supported. `DrillDownMiuixScrollChrome` uses this modifier over scroll-adjacent, full-width top chrome; the floating `NowPlayingBar` uses the same modifier within a rounded rectangular bar.

Miuix `0.9.4-rc01` supplies `BackdropEffectScope.progressiveBlur(...)` and `ProgressiveBlur.Top`. To achieve the documented pixel-sharp clear endpoint, the identical gradient must be passed to `drawBackdrop(progressiveGradient = ...)`; using only the effect produces a downscaled soft clear end.

## Goals

- Make the detail top chrome blur strongest at its top edge and progressively clear toward its content edge.
- Preserve the exact uniform blur behavior of the floating Now Playing bar and every unspecified caller.
- Retain fallback output and existing Android/non-shader capability gates.
- Keep Miuix API types within `:core:ui` implementation details.

## Non-Goals

- No Miuix navigation adoption, route transition, Back-handling, dialog, bottom-sheet, color-token, opacity, shape, or spacing change.
- No new public dependency type, process-death state, platform-specific source set, or generated code.
- No visual change to Now Playing, dialogs, or unsupported renderers.

## Decisions

### Explicit local blur-style policy

Add a `RhythHausGlassBlurStyle` enum in `:core:ui` with `Uniform` and `TopEdgeProgressive`. Add an optional `blurStyle` argument to `rhythHausLiquidGlass`, defaulting to `Uniform`. The wrapper converts the local policy internally: `Uniform` keeps the existing `blur(radius.toPx())`; `TopEdgeProgressive` calls `progressiveBlur(radius.toPx(), gradient = ProgressiveBlur.Top)` and passes the same `ProgressiveBlur.Top` value to `drawBackdrop(progressiveGradient = ...)`.

A local enum keeps feature modules independent of Miuix blur implementation types. A default preserves source and visual compatibility for the Now Playing bar and any future caller that does not opt in.

### One intentional caller opt-in

Only `DrillDownMiuixScrollChrome` passes `TopEdgeProgressive`. Its rectangular top chrome ends directly at scrollable content, matching the top-strong/bottom-clear geometry. `NowPlayingBar` stays on the default uniform style because its rounded floating surface needs blur across the full bounded region; one-directional edge blur would make it visually uneven.

### Preserve capability and fallback ownership

The existing `backdrop != null && isRuntimeShaderSupported()` branch remains the only effect path. The new style does not call progressive APIs when no supported backdrop/shader exists. The fallback continues to clip and paint exactly the supplied fallback color, retaining unsupported Android/JVM/iOS behavior and avoiding new allocation/work on the fallback path.

### Test at policy and production-composable boundaries

Create a core-ui policy test covering the uniform-to-null-gradient and top-edge-to-`ProgressiveBlur.Top` mapping. It must fail if either renderer selection regresses. Add/extend a JVM production-composable regression at the drill-down chrome boundary that asserts the explicit top-edge style is selected while the Now Playing caller remains default-uniform. The tests verify the public shared wrapper’s observable policy rather than GPU pixels, which cannot be deterministic on the JVM renderer.

### Generated dependency disclosure

Run the existing `:shared:exportLibraryDefinitions` task after source tests pass. It is the sole author of `aboutlibraries.json`; do not hand-edit its Miuix versions.

## Risks / Trade-offs

- **Visual behavior changes only on supported effect paths:** the clear endpoint is intentionally sharper at the drill-down content edge. Unsupported renderer behavior remains unchanged by design.
- **Pre-release Miuix implementation:** use precisely the released `0.9.4-rc01` API and preserve capability gates. Focused compile/tests plus actual desktop visual QA are required before acceptance.
- **No pixel-exact JVM assertion:** native shader output is not deterministic in JVM unit tests. Policy and production composition tests prove selection; desktop interactive visual QA proves the rendered effect.
