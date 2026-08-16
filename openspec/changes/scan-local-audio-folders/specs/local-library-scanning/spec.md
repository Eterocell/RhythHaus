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

#### Scenario: Remove-missing requires the authoritative completed scan
- **WHEN** remove-missing is requested with a source and scan id
- **THEN** the repository accepts it only when the id is the same source's latest valid `Completed` session
- **AND** latest is ordered deterministically by `completedAtEpochMillis DESC`, `startedAtEpochMillis DESC`, then `id DESC`
- **AND** the missing-track deletion and validation occur atomically
- **AND** the API returns `RemoveMissingTracksResult.Removed(count)` on success or `RemoveMissingTracksResult.Rejected(reason)` with `RemoveMissingTracksRejectionReason` on rejection
- **AND** a rejected request leaves tracks unchanged
- **AND** this query/transaction change does not require a schema migration

#### Scenario: Invalid remove-missing sessions are rejected
- **WHEN** the requested session is from another source, stale, active, cancelling, cancelled, failed, or absent
- **THEN** the repository rejects the request without deleting tracks

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

#### Scenario: One coordinator owns competing operations
- **WHEN** the App receives scan, remove-missing, remove-source, or clear requests
- **THEN** one App-owned coordinator admits and serializes those operations
- **AND** repeated clicks are explicitly rejected while an operation is admitted
- **AND** scan cancellation is awaited through terminal session persistence before a mutation repository write
- **AND** each operation has a token that guards scan/progress and combined library-plus-playlist publications
- **AND** stale tokens cannot publish state or overwrite a newer operation's result

#### Scenario: Library manager preserves production states
- **WHEN** the library manager is rendered in compact or wide production layouts
- **THEN** it preserves the intended empty/add-source, scanning/cancel, completed/rescan/add-source/remove-missing/report, cancelled/retry, failed/retry/report, and lost-access/recovery states and actions

### Requirement: Recoverable scan errors

The system SHALL record recoverable scan errors without failing the entire scan.

#### Scenario: Unreadable files are skipped
- **WHEN** the scanner encounters an unreadable or unsupported file
- **THEN** the file is skipped and recorded in the scan report
- **AND** scanning continues for other files

### Requirement: Terminal scan restoration

The system SHALL expose a repository global `latestTerminalScanSession(): ScanSession?` query and restore terminal scan state on App startup.

#### Scenario: Startup restores the latest terminal session
- **WHEN** App startup queries the repository's global `latestTerminalScanSession(): ScanSession?` and its source still exists
- **THEN** it restores that single latest `Completed`, `Cancelled`, or `Failed` session and its persisted errors
- **AND** the query orders by `COALESCE(completedAtEpochMillis, startedAtEpochMillis) DESC`, then `startedAtEpochMillis DESC`, then `id DESC`
- **AND** the restored result drives the single scan outcome panel

#### Scenario: Startup ignores active or missing-source sessions
- **WHEN** the global latest session is active or `Cancelling`, or its source no longer exists
- **THEN** startup ignores that session for restoration
- **AND** it does not restore errors from an ignored session

#### Scenario: Metadata failure falls back to filename
- **WHEN** metadata extraction fails or is unavailable for a discovered audio file
- **THEN** the track is stored with filename-derived display metadata

### Requirement: Existing playback integration

The system SHALL keep playback routed through existing shared playback and platform engine seams.

#### Scenario: Scanned track is playable
- **WHEN** a user selects and plays a scanned track
- **THEN** the shared playback controller receives the track's persisted `AudioSource`
- **AND** playback starts through the current platform engine when the source remains accessible

#### Scenario: Library and playlist publications reconcile after mutation
- **WHEN** a source or track mutation completes
- **THEN** the coordinator publishes reconciled library and playlist state together under the current operation token
- **AND** playback reconciles inaccessible or removed tracks through the existing playback policy
