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
