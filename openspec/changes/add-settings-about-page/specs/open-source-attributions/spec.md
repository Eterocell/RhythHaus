## ADDED Requirements

### Requirement: Cross-platform attribution catalog
The build SHALL generate and version-control an AboutLibraries JSON catalog under common Compose resources using AboutLibraries `15.0.3` and the non-Android-specific plugin configuration.

#### Scenario: Loading the catalog from shared UI
- **WHEN** the Open Source Libraries route is current on Android, desktop JVM/macOS, or iOS
- **THEN** the shared UI SHALL load the catalog from `files/aboutlibraries.json` through Compose Resources.

### Requirement: Open source libraries screen
The application SHALL render the parsed AboutLibraries catalog in a Settings-derived Open Source Libraries screen with the existing shared top-bar back affordance.

#### Scenario: Catalog has not finished parsing
- **WHEN** the screen is waiting for the shared catalog to parse
- **THEN** the screen SHALL present a localized non-crashing loading state.

#### Scenario: Catalog is available
- **WHEN** the shared catalog has parsed successfully
- **THEN** the screen SHALL present the AboutLibraries library and license information using its Compose Material 3 UI.

### Requirement: Catalog validation
The JVM test suite SHALL verify that the checked-in shared catalog is readable and contains at least one entry parsed by the pinned AboutLibraries `15.0.3` API. Catalog export SHALL be explicit maintenance/CI work, not an input to normal compilation.

#### Scenario: Dependency metadata is regenerated
- **WHEN** dependencies change and the AboutLibraries export task runs
- **THEN** the generated catalog diff SHALL be available for review and the catalog regression test SHALL pass.
