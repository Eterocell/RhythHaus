## Purpose

The Shared shell renders its authoritative Library route stack with Miuix transitions while retaining existing route, adaptive-layout, modal, and Back semantics on every supported platform.

## ADDED Requirements

### Requirement: Shared navigation rendering preserves authoritative route semantics

The application SHALL render compact Library destinations through a Shared-owned navigation renderer driven only by the authoritative Library route state. It SHALL preserve route push, pop, replacement, stale-detail recovery, exact displayed-playlist deletion invalidation, and the root route's non-poppable behavior. Equal route values with distinct appearances SHALL retain distinct entry state and correct predecessor restoration.

#### Scenario: A replacement has a fresh presented identity
- **WHEN** a displayed destination is replaced by an equal route value
- **THEN** the replacement receives a new presented identity
- **AND** a later pop restores the original predecessor rather than the replaced entry.

#### Scenario: A stale playlist deletion cannot affect a replacement
- **WHEN** a playlist deletion completes for a stale or no-longer-displayed appearance
- **THEN** the currently displayed route remains unchanged
- **AND** unrelated selection and Now Playing state remain unchanged.

### Requirement: Navigation rendering preserves responsive layout behavior

The application SHALL retain its compact and ListDetail layouts. In compact layout, route transitions SHALL be confined to the route container. In ListDetail layout, the master browser SHALL remain persistent and interactive while transitions are clipped to the detail pane; routes that are not active detail content SHALL retain their existing placeholder or overlay policy.

#### Scenario: A wide detail transition cannot cover the master browser
- **WHEN** a detail route changes in ListDetail layout
- **THEN** transition content is clipped to the detail pane
- **AND** the master browser remains visible and interactive.

### Requirement: Navigation rendering cannot take Back authority

The Shared Back arbiter SHALL remain the sole owner of Back precedence, exact-target predictive latching, cancellation, rejection, and authoritative in-flight settlement. Navigation rendering SHALL not consume, dismiss, or mutate a route before Shared has resolved a route target. Feature modals, edits, active-page selection, Now Playing expansion, and local dialogs SHALL retain their existing precedence before a route transition.

#### Scenario: A modal remains first Back target
- **WHEN** the active destination has a foremost modal, edit state, active-page selection, expanded Now Playing, and a poppable route
- **THEN** one Back intent targets only the modal
- **AND** no navigation transition begins until the modal is authoritatively inactive.

#### Scenario: A cancelled predictive Back changes nothing
- **WHEN** predictive Back begins for a route target and is cancelled
- **THEN** the active route, entry identity, and feature state remain unchanged
- **AND** a later Back intent is independently resolved by Shared.

#### Scenario: A route Back stays in flight until settlement
- **WHEN** Shared dispatches a route Back transition
- **THEN** repeated Back is suppressed until the exact latched target is authoritatively inactive or explicitly rejects completion
- **AND** callback return alone does not settle the intent.
