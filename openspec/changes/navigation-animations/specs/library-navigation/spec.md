## ADDED Requirements

### Requirement: Shared route transitions animate

RhythHaus shared Compose navigation SHALL animate transitions between Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog routes.

#### Scenario: Opening a nested route animates forward
- **WHEN** the current route is Home, Album Detail, or Artist Detail
- **AND** the user opens Album Detail, Artist Detail, Now Playing, Search, Settings, or Clear Library dialog through the shared route stack
- **THEN** the new route animates into view
- **AND** the transition direction communicates forward navigation

#### Scenario: Returning from a nested route animates backward
- **WHEN** the current route is Album Detail, Artist Detail, Now Playing, Search, Settings, or Clear Library dialog
- **AND** the user invokes a visible back control, shared left-edge swipe-back, or Android system/predictive back
- **THEN** the previous route animates into view
- **AND** the transition direction communicates backward navigation
- **AND** exactly one route is popped from the shared navigation stack

### Requirement: Navigation animation preserves behavior

Navigation animation SHALL NOT change shared route-stack semantics or unrelated app state.

#### Scenario: Existing route-stack behavior is preserved
- **WHEN** Search, Settings, or Now Playing is opened from Album Detail or Artist Detail
- **AND** the user returns with a back action
- **THEN** the app returns to the originating detail route
- **AND** playback state, selected track state, library contents, scanner state, and theme state are not reset by the animation layer

#### Scenario: Home remains the root
- **WHEN** the current route is Home
- **AND** the user invokes shared navigation back handling
- **THEN** the shared route stack does not consume the event
- **AND** no in-app route animation runs
