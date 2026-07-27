# Design: Architecture Refactor

## Overview

The refactor will split the current shared app monolith into a small root entry point, a named common state/coordinator boundary, and focused UI files. It preserves current behavior while making future changes easier to isolate and test.

## State/coordinator boundary

Introduce a shared coordinator/state holder for library app orchestration. It should centralize responsibilities currently scattered through `LibraryHomeScreen(...)`:

- current `LibraryNavigationStack` and last `LibraryNavigationTransition`;
- selected track id and selected-track fallback behavior;
- browse mode;
- expanded Now Playing visibility;
- bottom bar visibility and scroll-derived updates;
- import/scan state where still owned by common UI;
- actions for push/pop/replace route, opening overlays, dismissing overlays, playing selected tracks, clearing library, and updating scroll state.

The state holder may be a Compose-remembered common state object or a common ViewModel-style object. The implementation should choose the lowest-risk option that preserves behavior and avoids new dependencies. Platform dependencies stay injected; the state holder must not construct platform playback engines, database drivers, or platform source access directly.

## Pure decisions

Move decisions out of composable bodies where practical and test them in common tests. Existing `LibraryNavigation.kt` helpers are the model. Candidate decisions include:

- whether a wide detail route should replace the current detail route or push;
- selected-track fallback when playback state or library content changes;
- bottom-bar visibility state update from previous/current scroll position;
- route transition update for push/pop/replace/root;
- back handling decisions where they can be represented without Compose APIs.

## File organization

Target file responsibilities:

- `App.kt`: `App()` entry point, dependency construction, theme preference collection, handoff to the library shell.
- `RhythHausTheme.kt` or existing theme file: shared Haus/Miuix theme wrapper if moved.
- `LibraryAppState.kt`: coordinator/state holder and pure orchestration helpers.
- `LibraryHomeScreen.kt` or `LibraryAppShell.kt`: root library screen shell, adaptive list/detail container, transition host, fixed bottom bar, Now Playing overlay.
- `LibraryRoutes.kt`: route-content selection for home/detail/settings/search/dialog routes.
- `LibraryHomeContent.kt`: home list content and browse sections.
- `LibraryDetailContent.kt`: drill-down album/artist detail screens.
- `LibraryChrome.kt`: nested-scroll chrome, scrollbar, system-bar padding helper.
- `LibraryDialogs.kt`: clear-library dialog route.
- `LibraryCards.kt` / `LibraryRows.kt`: album, artist, track, import, and section presentational components if needed to keep files focused.

The exact file names may be adjusted in the implementation plan to minimize churn, but the responsibility split must remain clear.

## Incremental plan shape

Each implementation task should keep the project compiling and preserve behavior:

1. Add tests for pure extracted decisions.
2. Introduce the state/coordinator boundary while leaving UI layout mostly in place.
3. Extract the route/adaptive shell.
4. Extract home/detail content.
5. Extract chrome/dialog/leaf UI components.
6. Run final verification and record evidence.

## Verification

Focused verification should include common tests covering extracted decisions after each slice. Final verification should include:

- `openspec validate architecture-refactor --strict`;
- `./gradlew :shared:jvmTest --configuration-cache` or a justified focused equivalent during intermediate tasks;
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`;
- `/usr/bin/xcrun xcodebuild -version`;
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`;
- `git diff --check`.

## Continuation design: destination-scoped Library Back sessions

This section adds to, rather than replaces, the completed coordinator extraction. `LibraryAppState` owns exactly one Back orchestration module for the active Library destination. It is responsible for choosing, latching, dispatching, and settling a Back transition; feature destinations retain modal ordering and edit state. An active feature supplies a snapshot/capability for its foremost modal dismissal and, where applicable, edit exit. Hidden, retained, stale, outgoing, or merely composed destinations cannot register a consumable Back action.

### Stable identities and resolution

The module uses stable `DestinationId` and `BackTargetId` values. A target identity includes the active destination identity and the concrete target instance (for example, the modal, edit session, selection owner, Now Playing instance, or route transition) so a later similarly shaped UI state cannot satisfy an earlier session. Every resolution is against the current active destination and yields exactly one of:

1. foremost feature-provided modal dismissal;
2. feature-provided edit exit;
3. active-page-owned selection clear;
4. Now Playing dismissal;
5. route pop/replacement transition; or
6. unhandled at root when none applies.

Selection is eligible only when its owner page is the active destination. Retained selection owned by another page is stale state for normal reconciliation and is never a Back target. The module does not own modal ordering; it consumes the single foremost dismissal that the active feature publishes.

### Session lifecycle and suppression

`beginBackSession` resolves once and either returns `Unhandled` without consuming or creates an in-flight session with its stable target. The caller dispatches the target at most once. `completeBackSession` performs the latched transition only if that exact target is still foremost and valid; it must not re-resolve, retarget, or fall through to another precedence level. `cancelBackSession` performs no transition and releases the session.

For immediate/non-predictive adapters, dispatch creates the same in-flight session model. While a transition is in flight, further input is suppressed. Suppression ends only when authoritative state observes that the target is no longer active, or when the target explicitly reports rejection/failure; callback return alone is not settlement. If state changes make a latched target invalid before predictive completion, completion is a no-op and must not fall through.

### Adapter equivalence

System Back, desktop keyboard/interaction Back, and predictive Back adapters are thin callers of the same module. Predictive progress may drive presentation feedback, but target selection occurs only at gesture begin. Cancellation does not dispatch; completion cannot choose a new target. Non-predictive adapters use begin and dispatch once, then await authoritative settlement or rejection before accepting another request. Root `Unhandled` is returned to the adapter rather than converted into a consumed no-op.

### Displayed-playlist invalidation

Playlist deletion is modelled separately from Back resolution. When the deleted playlist identity exactly matches the active playlist-detail destination identity, `LibraryAppState` atomically leaves that destination and clears only state scoped to it, including its Back publication/session if applicable. It preserves unrelated route, playback, Now Playing, selection, and feature state. Deleting another playlist, or deleting the same identity while its detail is not active, must not invalidate the active destination. This invalidation neither begins nor completes a Back session.

### Continuation verification

Implement the module through common pure state-machine tests before adapter wiring. Incremental tests must cover precedence, active-destination filtering, stable identities, session settlement/rejection suppression, adapter equivalence, predictive latching/cancellation/invalid completion, root unhandled behavior, and exact displayed-playlist invalidation. Follow with focused common tests and the existing supported-platform verification matrix.
