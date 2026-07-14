## ADDED Requirements

### Requirement: Settings exposes application information
The Settings screen SHALL provide an accessible About action after its existing management content without changing its safe-area handling, compact spacing policy, source-management controls, or clear-library behavior.

#### Scenario: Opening About from Settings
- **WHEN** a user activates the Settings About action
- **THEN** the application SHALL push the shared About route above Settings.

### Requirement: About page presents common product identity
The shared About page SHALL display the common RhythHaus logo, the application name `RhythHaus`, the generated value of `rhythhaus.versionName`, and a source-code action for `https://github.com/Eterocell/RhythHaus`.

#### Scenario: Rendering product metadata on a supported target
- **WHEN** the About route is current on Android, desktop JVM/macOS, or iOS
- **THEN** the page SHALL render the same logo, name, and generated version without reading Android-only resources or `BuildConfig`.

#### Scenario: Opening the source repository
- **WHEN** a user activates the source-code action
- **THEN** the application SHALL hand `https://github.com/Eterocell/RhythHaus` to the platform URI handler.

### Requirement: About navigation preserves stack behavior
The About page SHALL provide an Open Source Libraries action and SHALL use the existing route stack for all return actions. Both About routes SHALL render as overlays and non-detail routes in compact and wide layouts.

#### Scenario: Returning from the libraries page
- **WHEN** a user returns from Open Source Libraries
- **THEN** the application SHALL return to About, then Settings, then the preceding route through successive back actions.

#### Scenario: Rendering nested About routes in adaptive layouts
- **WHEN** Settings, About, or Open Source Libraries is current in compact or wide layout
- **THEN** the corresponding route SHALL remain visible as the active non-detail overlay with correct stack back behavior.
