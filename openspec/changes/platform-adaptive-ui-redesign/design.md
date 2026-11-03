# Design

## Context

See `proposal.md` for motivation and `specs/` for observable contracts. The approved design baseline is [`2026-10-29-platform-adaptive-ui-redesign-design.md`](../../../docs/superpowers/specs/2026-10-29-platform-adaptive-ui-redesign-design.md). The durable interactive prototype and token evidence are [`2026-10-29-platform-adaptive-ui-redesign.html`](../../../docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.html) and [`2026-10-29-platform-adaptive-ui-redesign.tokens.json`](../../../docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.tokens.json); they are presentation references, not product behavior evidence.

The app is a Kotlin Multiplatform Compose application. `:shared` owns the Home-rooted route stack, app-wide Back arbitration, composition, window-dependent shell, foreground Now Playing surface, and platform facade. `AppShellLayout` already classifies compact/rail/sidebar widths at 720dp and 1000dp, measures bottom chrome, and applies safe-drawing/navigation/IME dimensions. `LibraryAppShell` owns route transitions and reduced-motion state. Feature modules own their screens and local UI state and are composed with explicit parameters. The approved visual direction changes presentation, not playback, scanning, repository, document, queue, or storage behavior.

Appearance is separately specified by [`theme-selection`](../theme-selection/). Its palette extension and plan define four complete palettes, mode/palette/Monet persistence, Android API 31+ dynamic resolution, and curated fallback. This change consumes that resolved semantic theme; it does not create another theme owner.

## Goals / Non-Goals

**Goals:**

- Make one Shared-owned safe-area geometry and foreground-input policy govern route and overlay presentation, including transition and cancellation frames.
- Apply the existing compact/rail/sidebar model across the approved supported widths and screens, using two-pane content only when the approved minimum content widths fit.
- Restore downward dismissal only for expanded Now Playing Controls while preserving the existing selection, route, playback, queue, and Back owners.
- Deliver the approved complete screen/state presentation with semantic tokens and existing EN/ZH feature ownership.

**Non-Goals:**

- No new route stack, presentation framework, service locator, playback/scanning/storage authority, business workflow, platform Back handler, or iOS interactive-back gesture.
- No SQLDelight schema or migration, media-file mutation, product-facing Windows/Linux support, new dependency, or app-level reduced-motion preference.
- No replacement of the behavior contracts in the existing Library, playlists, playback, onboarding, import, selection, dialog, and scroll-chrome capabilities.
- No claim that browser prototype rendering proves Compose layout, real OS Insets, Monet, native picker, window chrome, audio, or physical-device acceptance.

## Decisions

### 1. Shared owns the current window geometry and foreground boundary

Use one current safe-area-adjusted window geometry at the Shared composition root. Resolve system-bar, cutout, and IME dimensions there, then give the active route/overlay the explicit inset responsibility for its presentation. Do not let an obscured route and its foreground overlay independently consume the same keyboard or safe-area edge. A real window/inset change updates the shared geometry; an ordinary route transition reuses it for both source and target until the physical geometry changes.

Preserve existing explicit feature-entry parameters and layer-specific content behavior where required. Replace ad hoc route-owned or duplicated inset math only after the Compose failure baseline identifies it; do not change the existing full-screen player or onboarding safe-area treatment by assumption. Keep bottom-bar and navigation clearance based on active measured dimensions, and invalidate stale size measurements on content/layout changes before exposing input.

For Android system bars, the Shared host is the only writer. Determine icon appearance from the final surface under each bar, including a foreground surface that only partially covers the system region. Do not infer it from global light/dark mode alone or add window mutations to features.

### 2. Keep one responsive shell and its existing breakpoints

Retain the existing width classes: compact below 720dp, rail from 720dp through 999dp, sidebar at 1000dp and above. Classify using the safe-area-adjusted window width before subtracting app navigation. Re-evaluate the remaining content width before adding a second pane: keep the browse column at least 320dp, detail at least 400dp, and their gap 24dp; otherwise use a single content pane. Keep the 1440dp maximum content width and the approved class-specific margins from the design baseline. Height constrains vertical split/player presentation but does not create new navigation breakpoints.

Use one Shared-owned route header with an explicit title/return/action projection from the active route. Keep its height fixed for the active layout class and text scale: a long title ellipsizes but keeps its accessible full name, excess actions move to a named overflow menu, and feature content does not add a second system-inset app header. Distinct content headings remain in the body only when they add information beyond that route title. A text-scale or width-class change may recompute the common header height; a route/title/action change inside the same class may not.

Maintain the one Home-rooted stack. Resize, theme change, or appearance change is not navigation; route-owned state remains attached to its current entry. Settings and About remain global/tool routes rather than extra primary destinations. Use existing Back arbitration and preserve current explicit iOS return gestures without adding platform-native interactive Back.

### 3. Sequence appearance separately, then migrate presentation by owner

The implementation dependency is the existing `theme-selection` change. Finish its approved palette/persistence work first and expose one immutable resolved Haus scheme. This change consumes `:core:ui` semantic roles and makes the presentation cutover in Shared and each owning feature module; it does not duplicate theme resolution or persist a redesign-specific preference.

Migrate the shell, Library, Search, Playlists, Now Playing, Settings, and Onboarding in dependency order. Preserve explicit feature parameters and local state ownership. Reuse existing feature actions, resources, and repositories. Do not copy business state into Shared solely to animate a redesigned surface. Remove replaced presentation at each complete cutover rather than leaving A/B implementations or compatibility aliases.

### 4. Scope player drag arbitration to the active player surface

Keep `LibraryAppShell` as the sole authority for expanded-player visibility and call its existing close path once after a successful Controls dismissal. The Controls surface alone registers the full-player downward gesture; accept starts on non-interactive content such as artwork/unused surface, decide vertical intent before interception, and let buttons, seek controls, nested scroll, and edge Back keep their existing input. Use a distance/velocity threshold and settle incomplete gestures back to Controls; exact tuning is an implementation measurement, not a new product preference.

Queue remains a nested panel: its handle/header can close Queue to Controls, while its body owns scroll/reorder. Lyrics and Timer own their content scrolling and return to Controls using their existing close/Back action. A higher-priority dialog/editor disables underlying player input. Cancel, pointer loss, window resize, or foreground-layer changes settle to a valid stable state, with no route pop, queue mutation, playback stop, stale scrim, or orphaned pointer/accessibility target. Keep explicit close and platform Back as non-gesture alternatives.

### 5. Use existing feature owners for the full route/state matrix

Treat the 51 named prototype states and the page/state requirements in the approved design as the coverage inventory, not as a mandate to duplicate all combinations. Preserve the existing data projection and interaction owner for empty/currently-empty, scanning, error/cancel, missing artwork/metadata, selection/editing, playback loading/ended/error, playlist rules/documents, maintenance, onboarding, About, and drop feedback. Use real state labels/actions already available; do not add fake data or service behavior to make a screen look complete.

For each shared screen family, validate one compact and one wide representative. Add named platform representatives for Android/iOS safe area and appearance, and macOS window/sidebar/hover/context/drag surfaces. Use the design's size and locale matrix, 200% text and reduced-motion edge samples. Do not attempt a visual Cartesian product; native acceptance uses the explicit matrix in the approved design.

### 6. Keep verification at the consumer-visible boundary

Before changing shell Insets or player gestures, reproduce and record both failures on the actual Compose surface with window geometry and interaction-state evidence. Add deterministic tests for user-visible insets/clearance, transition settlement/cancellation, active-layer semantics, route-state retention, and the player interaction competition. Keep tests in Shared/feature production UI boundaries, not source-string checks or copies of internal math. Run a real desktop surface smoke after every nontrivial slice; run device/simulator checks where available and record unavailable environments exactly. Browser prototype smoke is only design-navigation coverage.

## Risks / Trade-offs

- [Gesture arbitration competes with Compose controls, nested scrolling, and queue reorder] → Scope the recognizer to Controls non-interactive surfaces, prove short/tap/horizontal/reorder/scroll/cancel cases in production UI tests, and retain visible close/Back actions.
- [Insets differ across Android gesture/three-button modes, iOS safe areas, and desktop resizing] → Capture before-state and same-window transition geometry on each available platform; recompute only on actual window/inset changes; keep user device acceptance open until performed.
- [Large route inventory can produce inconsistent feature migrations] → Use the approved route/state list as a completion checklist, migrate complete owning-feature slices in order, and run contract tests for the preserved behavior at each slice.
- [Dynamic colors can make preview rendering look like a curated palette or vice versa] → Keep the appearance contract separate, expose effective source/fallback through its semantic state, and report browser preview and device Monet evidence separately.
- [The browser screenshot helper timed out during this planning pass] → A managed-Chromium recording fallback captured the adaptive Library and Player/Queue prototype surfaces after the 51-route content and interaction smoke. This verifies only the browser design prototype; it is not native Compose, Android Monet, accessibility, or device acceptance.

## Migration Plan

1. Review and approve the adaptive [implementation plan](../../../docs/superpowers/plans/2026-10-30-platform-adaptive-ui-redesign.md) together with the separate [theme-selection plan](../../../docs/superpowers/plans/2026-10-30-theme-selection-palettes.md). Keep all production work blocked until this single gate is satisfied.
2. Implement the appearance change first in `.worktrees/theme-selection-palettes/`, with its own failing-before tests, strict OpenSpec completion, independent review, verification, and handoff. Integrate the completed dependency before starting adaptive shell code.
3. Create `.worktrees/platform-adaptive-ui-redesign/` from the resulting baseline. Start with real Compose failure evidence for Insets and player dismissal; execute the plan's shared-shell checkpoint before feature presentation checkpoints.
4. Complete each feature-owner slice in order, migrating all callers and removing replaced presentation in the same slice. Run the scoped verification and actual-surface smoke before advancing.
5. Run final project verification (`./init.sh`), strict OpenSpec checks, architecture/static checks, and independent review. Record device-only acceptance separately and leave unavailable/unperformed matrix items unverified.
6. Rollback uses a revert of the affected completed change/commit. No database rollback or compatibility shim is needed because this work changes presentation and existing preference keys only; a screen slice is not shipped partially as an alternate mode.

## Open Questions

None that change requirements, architecture, or task order. Gesture thresholds and platform-specific inset APIs are implementation measurements within the stated behavior and verification contract.
