## ADDED Requirements

### Requirement: Explicit shared navigation stack

The shared RhythHaus UI SHALL model library navigation as an explicit stack of routes with Home as the root route. Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog SHALL be represented as stack routes instead of independent local visibility booleans/nullables.

#### Scenario: Home is the immutable root
- **WHEN** the navigation stack is newly created
- **THEN** the current route is Home
- **AND** popping the stack leaves Home current

#### Scenario: Opening and backing out of album detail
- **WHEN** the user opens an album from Home
- **THEN** the current route is Album Detail for that album
- **WHEN** the user invokes back
- **THEN** the current route returns to Home

### Requirement: Origin-preserving overlays

The shared UI SHALL push Search, Settings, and Now Playing on top of the current route so back returns to the route that opened them.

#### Scenario: Search opened from album returns to album
- **WHEN** the current route is Album Detail
- **AND** the user opens Search
- **THEN** the current route is Search
- **WHEN** the user invokes back
- **THEN** the current route returns to the same Album Detail route

#### Scenario: Now Playing opened from artist returns to artist
- **WHEN** the current route is Artist Detail
- **AND** the user expands Now Playing
- **THEN** the current route is Now Playing
- **WHEN** the user invokes back or left-edge swipe-back
- **THEN** the current route returns to the same Artist Detail route

### Requirement: Dialog participates in navigation

The Clear Library confirmation dialog SHALL be represented as a route on top of the current route. Dismiss, cancel, and Android back SHALL remove only the dialog route. Confirming clear SHALL clear the library and remove the dialog route.

#### Scenario: Clear dialog preserves settings origin
- **WHEN** the current route is Settings
- **AND** the user opens Clear Library confirmation
- **THEN** the current route is Clear Library dialog
- **WHEN** the user cancels or invokes back
- **THEN** the current route returns to Settings

### Requirement: Back is consumed only when an in-app route can pop

Android back handling SHALL consume back only when the navigation stack can pop an in-app route. If the current route is Home, Android back SHALL not be consumed by the shared navigation stack.

#### Scenario: Back from Home may close the app
- **WHEN** the current route is Home
- **AND** the user invokes Android back
- **THEN** the shared navigation stack does not consume the event

#### Scenario: Back from a nested route does not close the app
- **WHEN** the current route is Search, Settings, Now Playing, Album Detail, Artist Detail, or Clear Library dialog
- **AND** the user invokes Android back
- **THEN** the shared navigation stack pops one route
- **AND** the app remains in the previous in-app screen
