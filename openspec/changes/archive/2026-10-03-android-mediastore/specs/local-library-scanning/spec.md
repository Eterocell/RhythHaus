# Spec Delta

## ADDED Requirements

### Requirement: Android MediaStore source is permission-aware

The system SHALL expose one persisted read-only Android MediaStore audio source only after the required Android audio permission is granted. It SHALL not request permission automatically at startup, and a denied or revoked permission SHALL preserve the source as inaccessible rather than deleting it.

#### Scenario: Permission is absent at startup
- **WHEN** RhythHaus starts without Android audio permission
- **THEN** it does not request permission or create the MediaStore source
- **AND** existing SAF sources remain available

#### Scenario: User grants permission from Settings
- **WHEN** the user grants the required permission through the Android host action
- **THEN** the permission state becomes available and the user can explicitly add the source or scan an existing source
- **AND** granting permission alone does not register or scan a source
- **AND** no media is copied or modified

#### Scenario: Explicit source registration remains idempotent
- **WHEN** the user adds MediaStore after granting permission
- **THEN** one stable source is registered through the existing library mutation coordinator
- **AND** repeated additions preserve the existing source and do not start a scan
- **AND** deleting the source prevents startup or permission refresh from automatically recreating it

### Requirement: MediaStore scanning is read-only and stable

The system SHALL query readable audio rows from Android MediaStore, emit stable source-local identities based on MediaStore row identity, preserve playable content URIs, and reuse the existing scan coordinator and terminal reporting.

#### Scenario: Rescan updates a MediaStore track
- **WHEN** the same MediaStore row is returned in a later scan
- **THEN** the existing track is updated rather than duplicated
- **AND** its source identity remains stable

#### Scenario: MediaStore row is removed
- **WHEN** a completed scan no longer returns a previously stored row
- **THEN** the existing remove-missing flow can remove that track
- **AND** no other source's track is removed

#### Scenario: Observed MediaStore row is temporarily unreadable
- **WHEN** a returned row encounters a recoverable read failure
- **THEN** the scan records the skipped row and continues processing readable siblings
- **AND** an existing track with that source-local identity is marked seen without replacing tags, artwork, favorites, history, or metadata overrides
- **AND** subsequent remove-missing does not delete that observed track

### Requirement: MediaStore source is isolated from SAF access

The system SHALL keep MediaStore and SAF source handles and access checks distinct. A MediaStore source SHALL fail closed when passed to SAF scanning, and SAF sources SHALL continue to use persisted tree permissions.

#### Scenario: Permission is revoked
- **WHEN** Android audio permission is revoked after the source was registered
- **THEN** the source reports lost access and scanning fails recoverably
- **AND** the source remains available for recovery after permission is restored

### Requirement: Settings exposes neutral recovery

The system SHALL expose permission state and a neutral recovery action in Settings without exposing Android permission names, intents, or package APIs to Shared or feature UI. The action SHALL not alter playback or unrelated library sources.

#### Scenario: Recovery is cancelled or denied
- **WHEN** the user cancels or denies the Android permission request
- **THEN** no MediaStore scan starts and existing library/playback state remains unchanged

#### Scenario: Permission requires system settings
- **WHEN** Android no longer allows the audio permission to be requested again
- **THEN** Settings offers application-settings recovery instead of an ineffective repeated prompt
- **AND** returning from system settings refreshes availability without automatically adding or scanning the source
