## Context

See `proposal.md` for motivation. The existing Shared-owned `:core:ui` glass wrapper is an implementation detail used by feature-owned chrome. It must preserve fallback behavior and avoid leaking Miuix dependency types across module boundaries.

## Goals / Non-Goals

**Goals:**

- Make only the scrolling detail top chrome use a Miuix top-edge progressive blur with a sharp clear endpoint.
- Retain uniform floating-surface glass and all existing capability gates.
- Keep the feature-to-core UI interaction as a small, typed local policy.

**Non-Goals:**

- No navigation, Back, layout, platform ABI, dependency-coordinate, or color/shape changes.
- No Miuix type in the public feature API or cross-module contract.
- No hand-edited generated library catalog.

## Decisions

### Local style abstraction

The core-ui wrapper accepts a local `RhythHausGlassBlurStyle` whose default is `Uniform`. It selects either the current uniform effect or Miuix `progressiveBlur` with `ProgressiveBlur.Top`; the same gradient is supplied to `drawBackdrop` for Miuix’s sharp-end composite. The library feature explicitly opts the top chrome into the progressive style. The Now Playing caller makes no style change.

### Existing fallback owns unsupported behavior

No capability gate is broadened. Without a supported backdrop/runtime shader, the wrapper follows its current clip-and-background fallback. This prevents incompatible native effects and preserves existing unavailable-renderer visuals.

### Verification boundary

Core UI tests verify mapping of the local style to Miuix’s gradient policy. Library JVM UI tests verify the drill-down production composable requests progressive style; the existing Now Playing public call site retains the default. Desktop visual QA confirms the actual supported rendering path; JVM tests do not assert pixels from GPU/shader output.

### Generated catalog only

The existing AboutLibraries Gradle export regenerates the JSON after the focused behavior tests pass. No dependency coordinate changes are needed because `0.9.4-rc01` is already resolved.

## Risks / Trade-offs

- The supported-renderer effect intentionally differs at one edge, while fallbacks remain unchanged. This matches the request and preserves cross-platform safety.
- Miuix’s release-candidate shader path must compile against the catalog-resolved artifact. A failure is a blocker; do not simulate progressive blur with a tint gradient.
