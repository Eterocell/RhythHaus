## Purpose

Provide one app-wide presentation stack whose ordering determines Back behavior across routes, overlays, selection, editors, dialogs, and expanded Now Playing, with continuous predictive transitions and restoration.

## ADDED Requirements

### Requirement: Presentation is represented by one canonical stack

The application SHALL maintain one canonical ordered presentation stack. Every visible dismissible surface SHALL correspond to exactly one stack entry, and the visible surface SHALL be the top entry. Domain data MAY remain owned by its feature, but feature-local visibility state SHALL NOT create a second presentation authority.

#### Scenario: Base route is presented
- **WHEN** the application opens a base destination
- **THEN** the destination is the top entry of the canonical stack
- **AND** its route payload and presentation identity are stable while it remains active.

#### Scenario: Overlay is opened over a route
- **WHEN** a modal, editor, dialog, selection mode, or expanded Now Playing surface is opened
- **THEN** exactly one corresponding entry is placed above the underlying destination
- **AND** Back targets that entry before the underlying destination.

### Requirement: Back removes only the foremost presentation

The application SHALL remove exactly the canonical stack’s top entry for a committed Back action. Back SHALL NOT inspect or fall through to independent feature-local precedence rules.

#### Scenario: Overlay Back precedence
- **WHEN** a route has an active overlay entry
- **THEN** a committed platform Back removes the overlay entry
- **AND** the underlying route remains the active destination.

#### Scenario: Route Back precedence
- **WHEN** no overlay entry is active and the stack has a predecessor
- **THEN** a committed Back removes exactly the current route entry
- **AND** the predecessor is restored.

#### Scenario: Root Back
- **WHEN** the stack contains only its root entry
- **THEN** the application does not underflow the stack
- **AND** platform root behavior remains available.

### Requirement: Predictive Back and swipe dismissal settle from stack transitions

The application SHALL support predictive Back cancellation and commit without mutating the canonical stack on cancellation. Approved swipe-to-dismiss gestures SHALL remove one top entry on commit and SHALL not steal interactions from protected or disabled entries.

#### Scenario: Predictive Back cancellation
- **WHEN** a predictive Back gesture is cancelled
- **THEN** the canonical stack remains unchanged
- **AND** the presentation returns to its pre-gesture depth.

#### Scenario: Predictive Back commit
- **WHEN** a predictive Back gesture commits
- **THEN** exactly one top entry is removed
- **AND** the resulting predecessor presentation settles continuously.

#### Scenario: Disabled overlay swipe
- **WHEN** a selection, editor, dialog, or protected overlay has swipe dismissal disabled
- **THEN** an edge swipe does not remove that entry
- **AND** its in-entry controls retain pointer ownership.

### Requirement: Presentation identity and restoration remain stable

Re-presenting an equal route SHALL create a distinct presentation identity, while ordinary pop SHALL restore the exact predecessor entry. Canonical stack entries SHALL be serializable and restorable without requiring the previous Shared route stack.

#### Scenario: Equal route replacement
- **WHEN** an equal-valued route replaces an outgoing route
- **THEN** the incoming route receives a fresh presentation identity
- **AND** the outgoing entry’s saved state is not reused for the incoming entry.

#### Scenario: Process restoration
- **WHEN** the application restores a saved navigation stack
- **THEN** every restorable route and overlay entry is reconstructed from its serialized key
- **AND** an invalid key/provider configuration fails explicitly rather than silently falling back to a second stack.

### Requirement: Feature modules remain navigation-library independent

Feature implementations SHALL request presentation changes through callback-first contracts and SHALL NOT depend on the navigation runtime or receive the canonical stack object. Shared SHALL adapt feature requests into stack operations at the app-shell composition root.

#### Scenario: Feature requests an overlay
- **WHEN** a feature requests a modal, editor, selection, or Now Playing presentation
- **THEN** Shared creates the typed corresponding stack entry
- **AND** the feature remains unaware of the navigation runtime implementation.

### Requirement: Platform Back has one owner

The application SHALL install no competing Shared platform Back arbiter alongside the canonical navigation runtime. Platform Back, predictive progress, cancellation, commit classification, and approved swipe transitions SHALL be handled by the canonical navigation host.

#### Scenario: Platform Back is committed during entry animation
- **WHEN** platform Back commits while a navigation entry is entering
- **THEN** the canonical navigation host resolves the interruption as one stack transition
- **AND** no independent Shared session dispatches a second Back action.
