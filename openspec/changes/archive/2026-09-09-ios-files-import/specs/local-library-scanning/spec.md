## MODIFIED Requirements

### Requirement: Recursive local source scanning

The system SHALL let users add a local music source and recursively scan supported audio files from that source.

#### Scenario: Android folder source is scanned
- **WHEN** an Android user adds a music folder through the system tree picker
- **THEN** the system persists access to the selected tree URI
- **AND** recursively scans supported audio documents under that tree
- **AND** stores discovered tracks with playable URI audio sources

#### Scenario: macOS folder source is scanned
- **WHEN** a macOS desktop user adds a music folder through the native folder picker
- **THEN** the system recursively scans supported audio files under that folder
- **AND** stores discovered tracks with playable file-path audio sources

#### Scenario: iOS app-local source is scanned

The iOS local-library workflow SHALL register and scan the managed `Documents/RhythHaus` app-local source when it is first absent, and SHALL allow the user to populate it through Files.app import while retaining the existing source identity and playable local-file handles.

#### Scenario: Default iOS source is created and scanned
- **WHEN** RhythHaus starts on iOS and no `ios-app-local` source is persisted
- **THEN** the system adds the existing `Documents/RhythHaus` directory as the single `IosAppLocal` source
- **AND** scans it through the App-owned scan coordinator
- **AND** preserves an already-persisted `ios-app-local` source unchanged

#### Scenario: Import into the managed iOS source
- **WHEN** an iOS user chooses audio files or a directory from Files.app
- **THEN** the system copies supported audio into the RhythHaus-managed app-local music directory
- **AND** the subsequent scan stores playable local file sources for the copied files
- **AND** reopening the app does not require access to the original external picker URLs

#### Scenario: Existing app-local files remain supported
- **WHEN** an iOS user rescans the managed source or has placed supported audio in the visible app Documents container
- **THEN** the scanner continues to discover and play those files under the existing `IosAppLocal` source
- **AND** the import workflow does not delete or replace existing managed files

#### Scenario: Managed app-local selection does not copy files
- **WHEN** an iOS user selects the managed `Documents/RhythHaus` directory, one of its descendants, or an audio file inside it from Files.app
- **THEN** the import workflow returns the existing `IosAppLocal` source for scanning
- **AND** does not copy, suffix, or replace files that are already under that managed directory
