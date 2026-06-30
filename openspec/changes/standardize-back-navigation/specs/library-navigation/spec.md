## ADDED Requirements

### Requirement: Standard visible back affordance

The shared RhythHaus UI SHALL use one consistent visible back affordance for drill-down, now playing, search, and settings screens.

#### Scenario: Back labels are visually consistent
- **WHEN** drill-down, now playing, search, or settings renders a visible back control
- **THEN** the visible label is `‹ Back`
- **AND** the control uses the shared back-chip styling
- **AND** the action invokes the same route-pop behavior as other in-app back mechanisms

### Requirement: Android predictive back pops in-app routes

Android system back SHALL use the predictive-back gesture pipeline for nested RhythHaus routes. When the shared navigation stack can pop, completing a predictive/system back gesture SHALL pop exactly one in-app route. When the stack is at Home, the shared navigation stack SHALL not consume back.

#### Scenario: Predictive back from a nested route
- **WHEN** the current route is Search, Settings, Now Playing, Album Detail, Artist Detail, or Clear Library dialog
- **AND** the user completes Android predictive/system back
- **THEN** the current route returns to the previous route
- **AND** the app does not close

#### Scenario: Predictive back from Home
- **WHEN** the current route is Home
- **AND** the user invokes Android predictive/system back
- **THEN** the shared navigation stack does not consume the event
