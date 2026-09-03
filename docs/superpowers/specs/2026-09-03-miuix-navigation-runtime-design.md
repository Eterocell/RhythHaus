# Miuix Navigation Runtime Adoption Design

Date: 2026-09-03
Status: no-go for `miuix-nav:0.9.4-rc01` — re-open only with upstream platform-Back opt-out/delegation

## Problem

Miuix `0.9.4-rc01` replaces its removed Navigation3 integration with `miuix-nav`. RhythHaus does not use the retired artifact; a new dependency would therefore be a full rendering-runtime migration, not a coordinate rename. The current Shared shell already owns destination identity, adaptive route policy, and the global Back protocol.

## Goals

- Evaluate and, only if the Back gate passes, use `miuix-nav` as the renderer for the existing Library route stack.
- Keep `LibraryAppState` as the single authority for route transition admission, replacement, exact playlist deletion invalidation, and Back settlement.
- Preserve compact and ListDetail behaviors, feature-local modals, active selection, Now Playing, dialogs, and the stable iOS facade.
- Keep feature and core module dependencies unchanged; Miuix is implementation-only in `:shared`.

## Non-Goals

- No route-state rewrite, new navigation module, serializable process-death route restoration, dialog-scene migration, or cross-feature navigation interface.
- No adoption of Miuix progressive blur in this change.
- No change to database, media playback, Koin, platform API, or iOS ABI.

## Design

`LibraryAppState` must retain the authoritative `LibraryNavigationStack`. An internal Shared `NavDisplay` renderer would need to project each `LibraryNavigationEntry` to an in-memory Miuix `NavKey` containing both the route value and immutable appearance token. The token would prevent Miuix content-state reuse when an equal route replaces an existing entry. The renderer would mirror state changes one way; it could never let Miuix directly mutate the app route stack.

The upstream source proves that renderer is not admissible in `0.9.4-rc01`. `NavDisplay` unconditionally registers `PredictiveBackHandlerWithSessions(enabled = backStack.size > 1)` and drives a route gesture during system predictive-Back progress; its public API exposes no platform-Back disablement. Its `onBack` callback runs only at commit, after route animation has started. That necessarily bypasses Shared's required first refusal for modal, edit, selection, and Now Playing targets. In-content Miuix swipe can be disabled, but that does not disable the system handler. The migration is stopped rather than introducing two Back authorities.

Compact layout hosts the display in the existing route region. Wide ListDetail mode keeps the master browser outside it and clips a detail-only display to its pane; Shared retains responsive eligibility/placeholder/overlay policy. Dialogs and feature modal/edit state remain outside the flat Miuix stack because v1 does not support a dialog/bottom-sheet scene strategy.

After adding the dependency, regenerate the existing Shared AboutLibraries export so the checked-in catalog describes the actual `0.9.4-rc01` artifacts.

## Acceptance Criteria

1. All production mutations still pass through `LibraryAppState`; direct Miuix push/pop/replace is impossible outside the internal renderer.
2. Equal route replacement receives a distinct presented key; pop restores the original predecessor.
3. Compact and wide detail transitions remain pane-bounded and master content stays interactive on wide layouts.
4. Modal, edit, selection, Now Playing, cancelled predictive Back, rejected Back, and route settlement retain the present Shared semantics.
5. The migration is blocked—not bypassed—because Miuix has a competing platform Back consumer.
6. No `miuix-nav` runtime dependency or AboutLibraries entry is added unless upstream first exposes platform-Back opt-out/delegation.

## Risks

`miuix-nav` is release-candidate software with a built-in Back stream. The dedicated feasibility gate protects established correctness; compilation or a visually plausible transition does not prove that global Back arbitration survived.

## OpenSpec

`openspec/changes/adopt-miuix-navigation-runtime/` contains the durable proposal, behavior contract, technical design, and task breakdown.
