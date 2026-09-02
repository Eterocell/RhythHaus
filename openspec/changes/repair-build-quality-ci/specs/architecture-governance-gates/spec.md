## ADDED Requirements

### Requirement: Current and legacy dependency-update aggregate edges are not architecture dependencies

`architectureCheck` SHALL observe both `io.github.ben-manes.versions` and `com.github.ben-manes.versions`. When either ID is applied, it SHALL exclude a project-dependency edge only when the exact dependency instance belongs to that plugin's `dependencyUpdatesAggregation` configuration. The exclusion SHALL not depend on configuration names alone and SHALL not suppress any ordinary authored project dependency.

#### Scenario: Current Versions plugin ID owns the aggregate

- **WHEN** the root build applies `io.github.ben-manes.versions`
- **THEN** `architectureCheck` excludes those aggregate edges
- **AND** the root quality gate completes without an `ARCH-CYCLE` or `ARCH-EDGE` diagnostic from the aggregate configuration

#### Scenario: Legacy Versions plugin ID remains supported

- **WHEN** a build applies `com.github.ben-manes.versions`
- **THEN** `architectureCheck` excludes that plugin's aggregate edges using the same identity-based rule

#### Scenario: Authored dependency remains governed

- **WHEN** a project declares a dependency that is not one of the captured aggregate dependency instances
- **THEN** `architectureCheck` evaluates the dependency using the normal architecture policy
- **AND** a forbidden dependency continues to emit its existing `ARCH-EDGE` diagnostic

#### Scenario: Existing narrow suppressions remain unchanged

- **WHEN** the graph includes KSP processor registrations or qualifying Android synthetic self-dependencies
- **THEN** their existing identity- and registry-based suppression behavior remains unchanged
- **AND** no other project-dependency edge is suppressed because of this requirement
