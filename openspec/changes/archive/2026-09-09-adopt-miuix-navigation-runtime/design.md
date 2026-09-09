## Context

See `proposal.md` for motivation. The current `LibraryAppState` owns `LibraryNavigationStack`, entry instance identity, route replacement/deletion invalidation, and an authoritative in-flight Back session. `LibraryAppShell` renders compact routes with `AnimatedContent` and wide routes in a persistent master/detail shell. `rememberLibraryNavigationEventBackHandler` already receives Android predictive Back through Navigation Event and feeds Shared's arbitration order.

`miuix-nav` is a flat navigation runtime. `NavDisplay` derives transition state from a mutable back stack and has its own platform/in-content Back driver. Its in-content swipe is opt-in, but its platform Back path and v1 omission of dialog/bottom-sheet scene strategies are material compatibility constraints.

## Goals / Non-Goals

**Goals:**

- Replace only the Shared compact and detail-pane route transition renderer with Miuix navigation transitions.
- Preserve one authoritative owner for route admission, destination identity, mutations, route Back, and final settlement.
- Preserve adaptive master/detail behavior and the existing modal/selection/Now Playing/local-dialog policies.
- Prove the upstream runtime cannot preempt Shared Back before changing production behavior.

**Non-Goals:**

- No `:core:navigation` module, feature API, route package rename, database/playback/DI/iOS-ABI change, or new navigation state framework.
- No process-death navigation restoration: the current app does not restore Library routes, so Miuix's saved stack is deliberately not introduced.
- No Miuix scene-based dialog or bottom-sheet migration; local dialogs and feature-owned modal/edit surfaces stay in their current owners.
- No progressive-blur visual change; that remains a separate, optional Miuix-blur adoption.

## Decisions

### Shared-only renderer seam

Create one internal Shared composable renderer that accepts the current immutable `LibraryNavigationStack`, the authoritative `LibraryAppState` route callbacks, and the existing entry-content callback. Its Miuix `NavBackStack` is a rendering mirror, created with `navBackStackOf` rather than `rememberNavBackStack`. `LibraryAppState` remains the only write authority; the renderer reconciles the mirror after a state mutation and never accepts direct push/pop/replacement from Miuix.

This is a deep module: callers retain the current small interface (state plus route callbacks) while the renderer contains key projection, Miuix-stack reconciliation, `NavDisplay` registration, clipping, and transition setup. A feature receives neither Miuix navigation types nor a Shared navigation interface.

### Entry key carries the existing appearance identity

Map every `LibraryNavigationEntry` to an internal `NavKey` containing both its `LibraryRoute` value and existing `instanceToken`. Never use route value alone as Miuix entry identity: replacement of an equal playlist/detail route must receive a fresh content state, and a later pop must return to the exact predecessor. Use a value-derived string key for each content slot.

The mirror is in-memory. This matches the current non-restored route state and avoids two independent persistence authorities. It also removes the need to annotate application routes with serialization solely for Miuix.

### One-directional synchronization

A Shared route transition changes `LibraryAppState` first. The renderer observes the resulting authoritative stack and applies the matching Miuix push, pop, replace, or full reconciliation. A NavDisplay content action calls the existing Shared callback; it cannot mutate the Miuix list directly. Entry composables retain the existing `LibraryNavigationEntry` so destination-scoped feature dismissal and exact deletion checks still use the same identity.

A route remains present in the visual mirror until Shared's existing Back session reports authoritative completion. A rejection/cancellation restores or retains the mirror without an unconfirmed pop.

### Back feasibility gate before cutover

Before replacing any production transition, add a production-host characterization test that mounts `NavDisplay` under the same Navigation Event dispatcher used by `LibraryAppShell`. It must show that the global arbiter receives first refusal for modal, edit, selection, and Now Playing targets, and that Miuix does not register a competing platform Back consumer or mutate its mirror before a route target is resolved.

In-content Miuix swipe is explicitly `NavSwipeDirection.None`. If Miuix's platform Back support cannot be disabled or delegated without competing with `rememberLibraryNavigationEventBackHandler`, stop this migration: rendering cannot safely coexist with a second predictive-Back authority. Do not replace the authoritative protocol with Miuix's built-in Back stream to make the test pass.

### Adaptive layout wraps, not redefines, the display

Compact mode hosts one renderer in the existing route region. ListDetail mode keeps the master browser outside the renderer and wraps the detail renderer in a bounds-clipped pane with pane-appropriate zero screen-corner clipping and no duplicate insets. Existing route policy decides which key is eligible for the detail pane and which content remains an overlay or placeholder. The renderer does not infer responsiveness or move state between stacks.

### Explicit transition and visual policy

Use the Miuix default slide/parallax transition only after the Back gate passes. Configure a theme-consistent backdrop and block pointer input during a transition so a partially animated entry is not interactive. Preserve existing test tags and semantics on rendered route content. Dialogs, bottom sheets, and the floating Now Playing surface remain outside the display.

### AboutLibraries consistency

Use the existing `:shared` AboutLibraries configuration to regenerate the checked-in JSON after adding `miuix-nav`, so the catalog reports all runtime artifacts at `0.9.4-rc01`. Do not hand-edit generated dependency data.

## Risks / Trade-offs

- **Resolved primary risk — dual Back consumers:** `miuix-nav:0.9.4-rc01` is incompatible. `NavDisplay` always registers `PredictiveBackHandlerWithSessions` whenever its stack has a destination to pop; it starts the route animation during progress and calls its `onBack` callback only on commit. There is no public parameter to disable that platform handler. The Shared arbiter could therefore not retain first refusal for modal, edit, selection, or Now Playing targets. Do not add this runtime or replace `AnimatedContent` on this release. Re-open only if upstream exposes platform-Back opt-out/delegation while retaining `NavDisplay` rendering.
- **Mirror lag:** state-first synchronization defers visual updates until Shared accepts a mutation. This is the required cost of retaining one source of truth; direct Miuix mutation would make stale deletion and in-flight rejection unsafe.
- **Wide layouts:** Miuix documents pane hosting, but it cannot model the app's master/detail policy. Shared retains that policy and clips the display strictly to the detail pane.
- **Pre-release upstream API:** `0.9.4-rc01` is already resolved in the repository but remains release-candidate software. Compilation, targeted transition behavior, and all supported platform checks remain required before acceptance.
