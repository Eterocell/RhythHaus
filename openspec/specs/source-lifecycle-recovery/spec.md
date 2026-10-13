# source-lifecycle-recovery Specification

## Purpose
Provide safe, explicit recovery for local library sources and media that become unavailable while preserving stable source identity and all user-owned library state.

## Requirements

### Requirement: Inaccessible sources remain visible and recoverable
The system MUST retain a configured source when its platform access is lost or its root is unavailable. Settings MUST identify the source as inaccessible and offer only recovery actions valid for that platform.

#### Scenario: Android SAF permission is revoked
- **WHEN** a persisted SAF tree permission is revoked
- **THEN** the source remains stored and its tracks remain in the library
- **AND** Settings explains that access must be restored and offers the Android-appropriate picker or settings action
- **AND** no scan or deletion occurs until access is restored and the user explicitly requests scanning.

#### Scenario: Folder source is missing or unavailable
- **WHEN** a configured macOS folder or iOS app-local root is missing, moved, or unavailable
- **THEN** Settings shows its inaccessible state without deleting the source or tracks
- **AND** offers an explicit platform-appropriate recovery action or explains when the managed iOS root is not user-rebindable.

#### Scenario: Android MediaStore access is revoked
- **WHEN** required audio permission is absent for a registered MediaStore source
- **THEN** the source remains persisted and Settings exposes existing permission recovery
- **AND** restoring permission alone does not automatically scan or mutate the source.

### Requirement: Rebind a folder source without replacing its identity
The system MUST allow a recoverable folder source to adopt a newly selected folder handle without replacing its stable source ID, creation time, tracks, or user-owned per-track state. The operation MUST reject a conflicting handle already owned by another source and MUST be atomic from the library's published-state perspective.

#### Scenario: Successful source rebind
- **WHEN** the user selects a readable replacement folder for an inaccessible source
- **THEN** the same source ID is updated to the new platform handle and display name
- **AND** existing track IDs, favorites, play history, metadata overrides, and playlists remain unchanged
- **AND** scanning starts only through the existing App-owned operation coordinator after explicit user action.

#### Scenario: Rebind is cancelled or fails
- **WHEN** the user cancels selection or the selected handle cannot be persisted or accessed
- **THEN** the prior source record and all library state remain unchanged
- **AND** no scan or missing-track removal occurs.

#### Scenario: Replacement handle conflicts
- **WHEN** the selected handle belongs to a different configured source
- **THEN** rebind is rejected with a recoverable explanation
- **AND** neither source nor any track is modified.

### Requirement: Missing media reporting is non-destructive
The system MUST distinguish a source-access failure from individual missing media. A failed, cancelled, or inaccessible-source scan MUST NOT become authoritative missing-media evidence. Missing tracks MUST remain stored until the user explicitly confirms removal from the latest valid completed scan.

#### Scenario: Entire source unavailable during scan
- **WHEN** the source root cannot be enumerated or its grant is revoked during scanning
- **THEN** the scan terminates as failed or cancelled, not completed
- **AND** its summary cannot authorize remove-missing
- **AND** existing tracks and associated user state remain intact.

#### Scenario: Some files disappeared from an accessible source
- **WHEN** a completed scan can enumerate the source but individual previously known files are absent
- **THEN** those tracks are reported as missing and remain playable only if their stored source remains accessible
- **AND** favorites, history, metadata overrides, playlist entries, and records remain until explicit accepted removal.

#### Scenario: Rebound source is scanned
- **WHEN** a rebound source scan completes successfully
- **THEN** matching source-local identities update existing tracks rather than duplicating them
- **AND** unmatched old tracks remain visible as missing until explicit removal.
