# Local Library Scanning

## Purpose

Define the supported cross-platform workflows for discovering, persisting, managing, restoring, and playing local audio sources.

## Requirements

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

### Requirement: Persistent local library database

The system SHALL persist library sources, tracks, scan sessions, scan errors, track-favorite relationships, listener-owned per-track play history, and app-local per-track metadata corrections in a shared KMP database.

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

#### Scenario: Track deletion cascades metadata corrections
- **WHEN** source removal, clear-library, or an accepted remove-missing operation deletes a library track
- **THEN** its associated metadata correction is deleted in the same lifecycle
- **AND** a later track with the same filename cannot inherit that correction

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

### Requirement: Persisted scan change summary
A completed explicit scan MUST persist counts for added, modified, unchanged, and missing tracks and MUST retain at most 100 affected-row details per category. A cancelled or failed scan MUST NOT replace the latest authoritative summary.

#### Scenario: Unchanged rescan
- **WHEN** a completed scan observes the same source-local identities with unchanged known size and modification metadata
- **THEN** the summary reports those tracks as unchanged and preserves their track IDs.

#### Scenario: Added and modified media
- **WHEN** a completed scan observes a new source-local identity or an existing identity with changed known size or modification time
- **THEN** it reports added or modified respectively and keeps the existing ID for modified media.

#### Scenario: Missing media is non-destructive
- **WHEN** a completed scan does not observe a previously persisted source-local identity
- **THEN** it reports missing without deleting the track, favorite, history, metadata override, or source.

#### Scenario: Unknown file facts
- **WHEN** size or modification time is unknown in either observation
- **THEN** that field alone does not classify the track as modified.

#### Scenario: Temporarily unreadable observed media
- **WHEN** an existing MediaStore identity is observed but its read probe fails recoverably
- **THEN** it is not reported missing and its existing record remains protected from remove-missing.

#### Scenario: Persisted arbitrary paths
- **WHEN** affected paths contain separator characters or Unicode, or missing files share a basename in different folders
- **THEN** restarting preserves the full counts and distinct path details.

### Requirement: Localized scan summary presentation
Settings MUST show the latest completed summary with localized labels, bounded affected paths, and an explicit distinction between missing-file reporting and the existing Remove missing action. Summary controls MUST expose readable semantics and MUST remain reachable in compact layouts.

#### Scenario: Failed or cancelled scan
- **WHEN** a scan fails or is cancelled
- **THEN** the prior completed summary remains visible and the failed/cancelled result does not claim authoritative change counts.

#### Scenario: Restoring reports after restart
- **WHEN** the latest terminal report is failed or cancelled and an earlier completed summary exists
- **THEN** Settings restores both the terminal report and the separately identified completed summary.

### Requirement: Desktop dropped sources preserve scan identity and deletion authority
The local-library scanner MUST preserve source-local identity, source-scoped deduplication, explicit scan admission, and existing missing-file confirmation for desktop dropped sources.

#### Scenario: Repeated drop and rescan
- **WHEN** the same folder or file is dropped again and then rescanned
- **THEN** existing track identities and user state remain stable and no media is copied.

#### Scenario: Existing unreadable path
- **WHEN** a persisted dropped path exists but is unreadable, non-regular, or a dangling symbolic link
- **THEN** its existing track is marked observed without replacing metadata or user state and cannot be removed as missing.

#### Scenario: Confirmed absence
- **WHEN** the persisted path itself no longer exists and a scan completes
- **THEN** the track is reported missing but retained until explicit missing-track removal.

#### Scenario: Unavailable fixed-file source recovery
- **WHEN** no member of a registered dropped-file set is currently readable
- **THEN** its Settings recovery action rescans that original set without requiring folder selection.
