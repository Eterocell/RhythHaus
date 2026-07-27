# Proposal: Architecture Refactor

## Summary

Refactor the shared Compose app architecture so `App.kt` becomes a thin composition/dependency entry point, library orchestration moves behind a named state/coordinator boundary, and library UI is split into focused files. The change is behavior-preserving and incremental.

## Problem

`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` has grown into a large mixed-responsibility file. It currently owns dependency construction, navigation state, scanner callbacks, playback actions, adaptive layout routing, Now Playing overlay state, route rendering, top chrome, dialogs, and many presentational rows/cards.

This makes future work risky because small behavior changes require editing a file that also owns unrelated state and UI. The code already has good examples of pure tested helpers in `LibraryNavigation.kt`; the broader app architecture should follow that pattern.

## Goals

- Keep app behavior and visuals unchanged.
- Make `App.kt` a small entry point that wires dependencies and hands off to a library app shell/state boundary.
- Introduce a shared state/coordinator layer for route, selection, scan/import, bottom-bar, and Now Playing overlay orchestration.
- Split library UI into focused common-source files.
- Add or preserve tests around extracted pure architecture decisions.
- Keep the work shared-first and incremental.

## Non-goals

- No visual redesign.
- No scanner, repository, database, playback engine, TagLib, source-access, or artwork-cache rewrite.
- No platform-native navigation migration.
- No dependency/toolchain changes.
- No Windows/Linux scope.

## Continuation: Library Back orchestration

The completed architecture refactor is extended with a separate, minimal session-hybrid continuation for Library Back orchestration. `LibraryAppState` will own one destination-scoped Back module while feature destinations retain their own modal and edit state. An active feature publishes only its foremost dismissal or edit-exit capability; it does not contribute to a global modal stack.

The continuation establishes stable destination and target identities, an explicit begin/complete/cancel session lifecycle, and one authoritative precedence: modal, edit, active-page selection, Now Playing, then route. The module resolves only the active destination. Equivalent system, desktop, and predictive adapters must invoke the same resolution contract. Predictive gestures latch one target when they begin, never retarget or fall through during completion, and act only if that same target remains foremost and valid.

Repeated Back input remains suppressed after dispatch until authoritative state observes settlement or explicitly rejects the transition. A root-level request with no eligible transition is unhandled so its invoking adapter retains platform/default responsibility. Displayed-playlist deletion remains a separate exact active-destination invalidation, not a Back action: it leaves only the deleted displayed playlist destination and discards only state owned by that destination, preserving unrelated Library state.

### Continuation non-goals

- No global modal stack, app-wide selection ownership, platform-native navigation migration, or UI redesign.
- No change to feature ownership of modal ordering, edit state, playback, scanner, repository, database, or platform adapters beyond the Back contract.
- No treatment of deletion completion or route invalidation as a Back intent.
