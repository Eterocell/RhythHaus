## ADDED Requirements

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
- **WHEN** an iOS user sets up or rescans the RhythHaus app-local music folder
- **THEN** the system recursively scans supported audio files in app storage
- **AND** stores discovered tracks with playable local file audio sources

### Requirement: Persistent local library database

The system SHALL persist library sources, tracks, scan sessions, and scan errors in a shared KMP database.

#### Scenario: Scanned tracks survive restart
- **WHEN** tracks have been discovered and stored during a scan
- **THEN** reopening the app restores the library from the database without requiring a new scan first

#### Scenario: Rescan updates existing tracks
- **WHEN** a source is scanned again
- **THEN** existing tracks are updated by source-local identity rather than duplicated

#### Scenario: Missing files can be removed
- **WHEN** a rescan no longer sees previously stored tracks for that source
- **THEN** the user can remove or mark those missing tracks through the library manager

### Requirement: Scan progress and management

The system SHALL expose scan progress and management actions through shared UI state.

#### Scenario: Active scan is visible and cancellable
- **WHEN** a scan is running
- **THEN** the UI shows scan status, visited counts, imported/updated counts, skipped count, and latest scanned item when available
- **AND** the user can cancel the scan

#### Scenario: Cancel preserves imported tracks
- **WHEN** the user cancels an active scan
- **THEN** tracks already imported before cancellation remain in the library
- **AND** the scan session is marked cancelled

#### Scenario: Completed scan shows management actions
- **WHEN** a scan completes
- **THEN** the UI offers rescan, add source, remove missing files, and view scan report actions

### Requirement: Recoverable scan errors

The system SHALL record recoverable scan errors without failing the entire scan.

#### Scenario: Unreadable files are skipped
- **WHEN** the scanner encounters an unreadable or unsupported file
- **THEN** the file is skipped and recorded in the scan report
- **AND** scanning continues for other files

#### Scenario: Metadata failure falls back to filename
- **WHEN** metadata extraction fails or is unavailable for a discovered audio file
- **THEN** the track is stored with filename-derived display metadata

### Requirement: Existing playback integration

The system SHALL keep playback routed through existing shared playback and platform engine seams.

#### Scenario: Scanned track is playable
- **WHEN** a user selects and plays a scanned track
- **THEN** the shared playback controller receives the track's persisted `AudioSource`
- **AND** playback starts through the current platform engine when the source remains accessible
