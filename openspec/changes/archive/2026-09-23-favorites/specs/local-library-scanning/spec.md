## MODIFIED Requirements

### Requirement: Persistent local library database

The system SHALL persist library sources, tracks, scan sessions, scan errors, and track-favorite relationships in the shared local database.

#### Scenario: Scanned tracks survive restart

- **WHEN** tracks and their favorite relationships have been discovered and stored during a scan
- **THEN** reopening the app restores the tracks and favorite state from the database without requiring a new scan first

#### Scenario: Rescan updates existing tracks

- **WHEN** a source is scanned again
- **THEN** existing tracks are updated by source-local identity rather than duplicated
- **AND** a favorite relationship for an existing track remains associated with that track

#### Scenario: Missing files can be removed

- **WHEN** a rescan no longer sees previously stored tracks for that source
- **THEN** the user can remove or mark those missing tracks through the library manager
- **AND** removal also removes their favorite relationships

#### Scenario: Remove-missing requires the authoritative completed scan

- **WHEN** remove-missing is requested with a source and scan id
- **THEN** the system accepts it only when the id is the same source's latest valid completed scan
- **AND** missing-track deletion and favorite cleanup occur atomically
- **AND** the API returns an explicit accepted or rejected outcome

#### Scenario: Invalid remove-missing sessions are rejected

- **WHEN** the requested session is from another source, stale, active, cancelling, cancelled, failed, or absent
- **THEN** the system rejects the request without deleting tracks or favorite relationships

#### Scenario: Track deletion cascades favorite state

- **WHEN** source removal, clear-library, or an accepted remove-missing operation deletes a library track
- **THEN** the associated favorite relationship is deleted as part of the same database lifecycle
- **AND** the mutation cannot leave an orphaned favorite visible to the application
