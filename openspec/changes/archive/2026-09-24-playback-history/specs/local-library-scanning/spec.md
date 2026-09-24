# Spec Delta

## MODIFIED Requirements

### Requirement: Persistent local library database

The system SHALL persist library sources, tracks, scan sessions, scan errors, track-favorite relationships, and listener-owned per-track play history in a shared KMP database.

#### Scenario: Scanned tracks survive restart
- **WHEN** tracks, their favorite relationships, and their play histories have been discovered and stored
- **THEN** reopening the app restores tracks, favorite state, and play history without requiring a new scan first

#### Scenario: Rescan updates existing tracks
- **WHEN** a source is scanned again
- **THEN** existing tracks are updated by source-local identity rather than duplicated
- **AND** favorite relationships and play history for an existing track remain associated with that track

#### Scenario: Missing files can be removed
- **WHEN** a rescan no longer sees previously stored tracks for that source
- **THEN** the user can remove or mark those missing tracks through the library manager
- **AND** removal also removes their favorite relationships and play history

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

#### Scenario: Track deletion cascades favorite state
- **WHEN** source removal, clear-library, or an accepted remove-missing operation deletes a library track
- **THEN** the associated favorite relationship is deleted as part of the same database lifecycle
- **AND** the mutation cannot leave an orphaned favorite visible to the application

#### Scenario: Track deletion cascades play history state
- **WHEN** source removal, clear-library, or an accepted remove-missing operation deletes a library track
- **THEN** the associated play-history record is deleted as part of the same database lifecycle
- **AND** the mutation cannot leave an orphaned history record visible to the application