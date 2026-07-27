## ADDED Requirements

### Requirement: Thin shared app entry point

The shared app entry point SHALL delegate library screen orchestration and presentation to focused common-code components instead of keeping most library internals in `App.kt`.

#### Scenario: App entry point wires dependencies only
- **WHEN** the shared `App()` composable is inspected after the refactor
- **THEN** it constructs or remembers app-level dependencies and theme state
- **AND** it hands those dependencies to a library app shell or coordinator
- **AND** it does not contain the main library route rendering, home/detail list rendering, chrome implementation, or dialog implementation.

### Requirement: Shared library coordinator boundary

Library navigation, selection, scan/import, bottom-bar, and Now Playing overlay orchestration SHALL be represented by a named shared state/coordinator boundary with explicit actions.

#### Scenario: Navigation actions are centralized
- **WHEN** a route is pushed, popped, replaced, or dismissed
- **THEN** the change flows through the coordinator/state boundary or a pure helper it owns
- **AND** the existing `LibraryNavigationStack` semantics are preserved.

#### Scenario: Playback actions preserve queue behavior
- **WHEN** a user selects or toggles playback from home, detail, or bottom bar UI
- **THEN** the same queue selection and play/pause behavior as before the refactor is used
- **AND** platform playback engine behavior is not redesigned.

#### Scenario: Scan/import actions preserve existing behavior
- **WHEN** a folder scan succeeds, fails, is unavailable, or is cancelled
- **THEN** the same user-visible import message, scan progress, library refresh, and cancellation behavior as before the refactor is preserved.

### Requirement: Focused common UI files

Library UI SHALL be split into focused common-source files by responsibility.

#### Scenario: Route shell is separated from leaf UI
- **WHEN** route/adaptive shell code changes in the future
- **THEN** it is located in a file responsible for the library shell or route rendering
- **AND** album cards, artist rows, track rows, chrome, and dialogs are not defined in the root app entry file.

#### Scenario: Presentational extraction preserves UI semantics
- **WHEN** presentational components are moved out of `App.kt`
- **THEN** their visual structure, strings, content descriptions, callbacks, and modifiers remain equivalent unless the implementation plan explicitly identifies a necessary behavior-preserving adjustment.

### Requirement: Behavior-preserving architecture refactor

The architecture refactor SHALL NOT change product behavior or broaden scope.

#### Scenario: Existing behaviors remain equivalent
- **WHEN** the refactored app is built and exercised through existing automated tests
- **THEN** existing route behavior, adaptive thresholds, route animations, predictive/system back handling, bottom-bar visibility, Now Playing overlay behavior, clear-library behavior, scan/import behavior, and playback controls remain equivalent.

#### Scenario: No dependency or platform scope expansion
- **WHEN** the refactor is complete
- **THEN** no new dependencies, toolchain changes, platform-native navigation migration, scanner rewrite, playback rewrite, database rewrite, or Windows/Linux support are introduced.

### Requirement: Extracted decisions are tested

Pure decisions introduced or moved as part of the refactor SHALL have common tests where practical.

#### Scenario: Decision helper coverage
- **WHEN** the refactor extracts route, selection, adaptive, scroll, or orchestration decisions into pure helpers
- **THEN** common tests cover representative cases before the helper is relied on by moved UI code.

## ADDED Requirements — Continuation: destination-scoped Library Back orchestration

### Requirement: LibraryAppState owns one destination-scoped Back module

`LibraryAppState` SHALL own one Back orchestration module that resolves Back only for the active Library destination. Feature destinations SHALL retain modal ordering and edit state, and SHALL publish only their foremost dismissal and edit-exit capability to that module.

#### Scenario: Active feature publishes the foremost dismissal
- **WHEN** an active feature has multiple modal experiences
- **THEN** the feature determines which one is foremost and publishes at most that dismissal to the Back module
- **AND** `LibraryAppState` does not maintain a global modal stack.

#### Scenario: Inactive destination cannot consume Back
- **WHEN** a retained, hidden, stale, outgoing, or otherwise inactive destination has modal, edit, or selection state
- **THEN** that state is not eligible to consume the current Back intent
- **AND** only the active destination's publication is considered.

### Requirement: Back resolution has stable target identities and precedence

Each Back target SHALL have a stable identity composed of its active destination identity and concrete target identity. The module SHALL resolve at most one target in this order: foremost modal, edit exit, active-page selection clear, Now Playing dismissal, then route transition.

#### Scenario: One Back intent chooses the foremost eligible action
- **WHEN** the active destination publishes a modal dismissal and also has edit, selection, Now Playing, or route transitions available
- **THEN** the module chooses only the modal dismissal
- **AND** one completed Back intent performs exactly one transition.

#### Scenario: Selection is eligible only on its owning active page
- **WHEN** selection exists for a page other than the active destination
- **THEN** it is not selected as a Back target
- **AND** stale selection remains subject to ordinary state reconciliation rather than app-wide Back ownership.

#### Scenario: A replacement target does not satisfy an existing session
- **WHEN** a Back session latched a target and a new target with similar type or presentation replaces it
- **THEN** the replacement has a different target identity
- **AND** it cannot be completed or settled as the latched target.

### Requirement: Back transitions use explicit authoritative sessions

The Back module SHALL support begin, complete, and cancel operations. A dispatched transition SHALL remain in flight until authoritative state observes that its target is no longer active or the target explicitly rejects completion; repeated Back input SHALL be suppressed while it remains in flight.

#### Scenario: Authoritative settlement releases suppression
- **WHEN** a non-predictive Back adapter dispatches a valid target
- **THEN** subsequent Back input is suppressed until authoritative state no longer reports that exact target as active
- **AND** callback return alone does not release suppression.

#### Scenario: Explicit rejection releases suppression without a transition
- **WHEN** the active target explicitly reports that it cannot complete
- **THEN** the session is released without treating the target as settled
- **AND** a later Back intent may resolve current authoritative state.

### Requirement: Adapters preserve one Back contract

System, desktop, and predictive Back adapters SHALL use equivalent resolution and session semantics. Predictive Back SHALL latch its target at begin, SHALL not retarget during progress or completion, and SHALL not fall through to a lower-precedence target.

#### Scenario: Predictive cancellation is transition-free
- **WHEN** a predictive Back gesture is cancelled after its target is latched
- **THEN** the module cancels the session
- **AND** no dismissal or navigation transition is performed.

#### Scenario: Invalid predictive completion does not fall through
- **WHEN** a predictive Back target is no longer foremost or valid at completion
- **THEN** completion performs no transition
- **AND** it does not resolve a new target or fall through to another precedence level.

#### Scenario: Root Back remains unhandled
- **WHEN** the active destination has no modal, edit mode, eligible selection, Now Playing overlay, or route transition
- **THEN** the module returns an unhandled Back intent
- **AND** the invoking adapter retains responsibility for its platform or interaction default.

### Requirement: Displayed-playlist deletion is exact destination invalidation

Displayed-playlist deletion SHALL be handled separately from Back resolution. Only deletion whose playlist identity exactly matches the active playlist-detail destination SHALL atomically leave that destination and discard state owned by it; unrelated Library state SHALL be preserved.

#### Scenario: Deleting the displayed active playlist invalidates only its destination
- **WHEN** the playlist currently displayed by the active playlist-detail destination is deleted
- **THEN** the app leaves that destination and clears only its destination-scoped state and Back publication/session
- **AND** unrelated routes, playback, Now Playing, selection, and feature state remain intact.

#### Scenario: Deleting a different or inactive playlist preserves the active destination
- **WHEN** a different playlist is deleted, or the matching playlist detail is not the active destination
- **THEN** the active destination is not invalidated
- **AND** deletion neither begins nor completes a Back session nor acts as a Back intent.

### Requirement: Continuation decisions are test-first and incremental

The continuation SHALL add pure common tests for resolution, identities, sessions, adapter equivalence, predictive behavior, and displayed-playlist invalidation before each corresponding production wiring slice relies on those decisions.

#### Scenario: Test-first contract coverage
- **WHEN** a continuation behavior is introduced or changed
- **THEN** its focused test first demonstrates the missing or incorrect contract
- **AND** the implementation makes that test pass before the next adapter or invalidation slice is started.
