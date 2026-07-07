## ADDED Requirements

### Requirement: Root app entry point remains stable

The shared app entry point SHALL remain available as `com.eterocell.rhythhaus.App` while package organization is refactored.

#### Scenario: Platform app entry points keep using App
- **WHEN** Android, iOS, and desktop entry points compile after the refactor
- **THEN** they can still invoke the shared `App()` composable
- **AND** no platform entry point requires a behavioral rewrite.

### Requirement: Library UI has a dedicated package

Library feature UI and orchestration SHALL live under a dedicated library UI package separate from local-library infrastructure.

#### Scenario: Library UI files move out of the root package
- **WHEN** the package refactor is complete
- **THEN** library shell, coordinator, route, navigation, home/detail content, chrome, dialog, and row/card UI files are declared under `com.eterocell.rhythhaus.library.ui`
- **AND** scanner, repository, database, source-access, and path-resolution code remains under `com.eterocell.rhythhaus.library` or platform-specific source sets.

### Requirement: Feature screens have feature packages

Now Playing, Search, and Settings shared UI SHALL be placed in feature packages.

#### Scenario: Feature UI package ownership is clear
- **WHEN** route overlay/screen implementation files are inspected
- **THEN** Now Playing UI is under `com.eterocell.rhythhaus.nowplaying`
- **AND** Search UI is under `com.eterocell.rhythhaus.search`
- **AND** Settings UI is under `com.eterocell.rhythhaus.settings`.

### Requirement: Shared helpers move only with safe package alignment

Reusable UI/theme/playback/model helpers SHALL move only when package declarations and expect/actual declarations remain consistent across source sets.

#### Scenario: Expect/actual package declarations remain aligned
- **WHEN** a file with expect/actual declarations is moved
- **THEN** every corresponding actual declaration is moved to the same package
- **AND** common, Android, JVM, and iOS compilations succeed.

#### Scenario: High-risk moves may be deferred
- **WHEN** moving playback or model files would cause disproportionate platform or Swift-facing churn
- **THEN** the implementation may leave those files in their current package
- **AND** the deferral is recorded in the task report and final evidence.

### Requirement: Package refactor preserves behavior

The package organization refactor SHALL NOT change product behavior or broaden scope.

#### Scenario: Existing behavior remains equivalent
- **WHEN** the refactor is complete and verification runs
- **THEN** existing route behavior, adaptive behavior, route animations, predictive/system back handling, bottom-bar visibility, Now Playing overlay behavior, clear-library behavior, scan/import behavior, playback behavior, theme behavior, strings, content descriptions, Miuix/blur behavior, and platform seams remain equivalent.

#### Scenario: No dependency or product scope expansion
- **WHEN** the refactor is complete
- **THEN** no new dependencies, toolchain changes, resource changes, native navigation migration, scanner rewrite, playback rewrite, database rewrite, TagLib rewrite, or Windows/Linux support are introduced.
