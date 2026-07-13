## ADDED Requirements

### Requirement: Additive library sources on supported platforms
The system SHALL let Android and desktop JVM/macOS users add multiple independently persisted music-folder sources through repeated folder selection, and SHALL keep iOS limited to its existing single app-local source.

#### Scenario: Add a second Android folder
- **WHEN** an Android user selects a SAF tree whose stable source identity differs from all configured sources
- **THEN** the system persists and scans the new source
- **AND** previously configured sources and their tracks remain available

#### Scenario: Add a second desktop folder
- **WHEN** a desktop user selects a canonical folder path whose stable source identity differs from all configured sources
- **THEN** the system persists and scans the new source
- **AND** previously configured sources and their tracks remain available

#### Scenario: Select an existing source again
- **WHEN** a user selects a folder whose stable source identity is already configured
- **THEN** the system rescans and refreshes that source instead of creating a duplicate source

#### Scenario: iOS source selection remains single-source
- **WHEN** the shared source-management UI runs on iOS
- **THEN** the system does not offer an action to add another folder
- **AND** the existing app-local source can continue to be scanned

### Requirement: Independent source identity and scan lifecycle
The system SHALL keep tracks and scan state isolated by source identity and SHALL run at most one source scan at a time.

#### Scenario: Equal relative paths in different folders
- **WHEN** two configured sources contain supported files with the same source-local path
- **THEN** the system persists the tracks independently under their respective source IDs

#### Scenario: Rescan one source
- **WHEN** a user requests a rescan for one configured source
- **THEN** the system scans only that source
- **AND** tracks belonging to other sources are not updated or removed

#### Scenario: Scan mutation controls
- **WHEN** any source scan is active
- **THEN** add, rescan, and source-removal controls are disabled until the scan reaches a terminal state

### Requirement: Configured source management
The system SHALL present configured sources in shared management UI with source status and source-scoped actions.

#### Scenario: View configured sources
- **WHEN** one or more sources have been persisted
- **THEN** Settings lists each source with its display name, access state, and last-scan state

#### Scenario: Lost source access
- **WHEN** the platform reports that a configured source is no longer accessible
- **THEN** the source remains visible with lost-access status
- **AND** the user can remove it without affecting other sources

### Requirement: Transactional source removal
The system SHALL remove a selected source and its dependent tracks, scan sessions, and scan errors atomically while preserving all records belonging to other sources.

#### Scenario: Remove one of multiple sources
- **WHEN** the user confirms removal of one configured source while no scan is active
- **THEN** the selected source and its tracks and scan history are removed in one repository operation
- **AND** every other source and its tracks and scan history remain unchanged

#### Scenario: Refresh after removal
- **WHEN** source removal succeeds
- **THEN** the shared source list and library track list refresh to exclude only the removed source data

### Requirement: Existing library behavior remains compatible
The system SHALL preserve existing playback, artwork loading, scan progress and cancellation, navigation, clear-library, and theme behavior while supporting multiple sources.

#### Scenario: Play a track from any configured source
- **WHEN** a user selects a track imported from any accessible configured source
- **THEN** playback uses the track's existing persisted `AudioSource` through the current playback engine seam
