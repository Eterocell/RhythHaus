## MODIFIED Requirements

### Requirement: Dependency direction and shared composition boundary

Applications SHALL depend on `:shared`; `:shared` SHALL own `App()`, the root shell, the complete Library route state, cross-feature Back arbitration, lifecycle, Koin assembly, and stable `MainViewController`. Miuix navigation MAY be an implementation-only renderer inside `:shared`, but SHALL NOT move routing, Back, lifecycle, composition, or iOS-facade authority to a core, feature, or app module. No core or feature module SHALL depend on `:shared` or an app module; no feature SHALL depend on another feature implementation; cross-feature access SHALL use feature APIs; and only `:shared` SHALL compose implementation modules.

#### Scenario: Navigation rendering does not create a forbidden ownership edge
- **WHEN** navigation rendering is added
- **THEN** `:shared` remains the sole owner of the route and Back interface
- **AND** the normal project graph remains acyclic with no feature-to-Shared bridge or new navigation module.

#### Scenario: Graph rejects a forbidden bridge
- **WHEN** a fixture adds a core/feature-to-shared edge, an app edge, a feature-implementation edge, or a feature-to-shared-to-feature bridge
- **THEN** architecture verification fails with the forbidden dependency reported
- **AND** the normal graph remains acyclic.
