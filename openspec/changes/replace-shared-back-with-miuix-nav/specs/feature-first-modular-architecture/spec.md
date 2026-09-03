## MODIFIED Requirements

### Requirement: Dependency direction and shared composition boundary

Applications SHALL depend on `:shared`; `:shared` SHALL own `App()`, root shell, the canonical app-wide navigation stack and its Miuix host, lifecycle, Koin assembly, and stable `MainViewController`. Shared SHALL own navigation composition and typed stack operations but SHALL NOT retain an independent Back-precedence/session arbiter. No core or feature module SHALL depend on `:shared` or an app module; no feature SHALL depend on another feature implementation; cross-feature access SHALL use feature APIs; and only `:shared` SHALL compose implementation modules.

#### Scenario: Canonical navigation host remains at the composition root
- **WHEN** a route or overlay is rendered
- **THEN** the Shared app shell composes the sole canonical navigation host
- **AND** feature implementations receive callback-first contracts rather than navigation-runtime types.

#### Scenario: Graph rejects a navigation bridge
- **WHEN** a feature implementation imports the navigation runtime or adds a feature-to-Shared implementation bridge
- **THEN** architecture verification fails with the forbidden dependency reported
- **AND** the normal graph remains acyclic.

#### Scenario: Graph rejects a forbidden bridge
- **WHEN** a fixture adds a core/feature-to-shared edge, an app edge, a feature-implementation edge, or a feature-to-shared-to-feature bridge
- **THEN** architecture verification fails with the forbidden dependency reported
- **AND** the normal graph remains acyclic.

### Requirement: Back publication and deletion remain authoritative

The previous Shared Back-publication, precedence, predictive-latching, and settlement protocol SHALL be removed as the application presentation authority. The canonical navigation host SHALL represent every dismissible surface as an ordered entry and SHALL own Back commit/cancellation. Shared and features SHALL retain only domain-state cleanup and explicit invalid-destination navigation commands; no feature dismissal port SHALL veto or dispatch a second Back action.

#### Scenario: Overlay order replaces Back publication
- **WHEN** modal, edit, selection, or Now Playing presentation is active
- **THEN** its canonical stack entry is above the active route
- **AND** a committed Back removes that entry without Shared target resolution, fallthrough, or duplicate dispatch.

#### Scenario: Predictive cancellation does not mutate feature state
- **WHEN** canonical predictive Back is cancelled
- **THEN** the stack and feature presentation state remain unchanged
- **AND** no legacy Shared Back session or feature dismissal registration is invoked.

#### Scenario: Predictive Back resolves only the latched active appearance
- **WHEN** modal and edit dismissal surfaces are published for an active playlist destination and predictive Back begins
- **THEN** the canonical host preserves the exact destination and appearance identity through cancellation, and a valid completion removes it at most once
- **AND** non-predictive dispatch cannot create a second intent while the first transition is in flight, rejection leaves the stack unchanged, and later Back starts a new intent.
