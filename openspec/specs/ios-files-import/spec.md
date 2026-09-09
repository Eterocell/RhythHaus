# iOS Files Import Specification

## Purpose

Provide a safe, local-first iOS import workflow that lets users select audio content from Files.app and makes a durable, playable copy in RhythHaus app storage.

## Requirements

### Requirement: Files.app audio import

The system SHALL let an iOS user select one or more supported audio files or directories through the system file picker and import them into RhythHaus-managed app storage.

#### Scenario: Import selected audio files
- **WHEN** the user selects supported audio files and confirms the system picker
- **THEN** the system copies each selected supported file into the managed RhythHaus music directory
- **AND** the system returns the existing iOS app-local `LibrarySource` for scanning
- **AND** copied files remain playable after the picker closes and after app restart

#### Scenario: Import a selected directory recursively
- **WHEN** the user selects a directory containing supported audio files and nested directories
- **THEN** the system recursively discovers supported audio files within that directory
- **AND** copies them into the managed RhythHaus music directory without copying unsupported files
- **AND** reports imported, skipped, duplicate, and failed counts

### Requirement: Safe import lifecycle

The system SHALL scope external document access to the import operation and SHALL not persist external security-scoped URLs as playable library handles.

#### Scenario: Cancel the picker
- **WHEN** the user cancels the system file picker
- **THEN** the system reports a cancellation distinct from failure
- **AND** it leaves the managed music directory and library database unchanged

#### Scenario: Picker or copy failure
- **WHEN** the picker fails or a selected item cannot be copied
- **THEN** the system reports a recoverable failure with a user-facing message
- **AND** successfully copied files remain valid for a later scan
- **AND** the system does not publish a partially constructed external source

### Requirement: Duplicate-safe managed storage

The system SHALL avoid creating duplicate managed files when the same source content is imported repeatedly.

#### Scenario: Re-import an unchanged file
- **WHEN** an imported file has the same managed destination identity as an existing managed file
- **THEN** the system keeps one managed file
- **AND** reports the item as duplicate or unchanged
- **AND** does not create a duplicate library track during the subsequent scan

### Requirement: Default managed iOS source

The system SHALL register RhythHaus's Files-visible `Documents/RhythHaus` app-local directory as the one `IosAppLocal` source on first iOS launch and scan it through the existing source lifecycle.

#### Scenario: Start with music already in the RhythHaus directory
- **WHEN** RhythHaus starts on iOS without an existing `ios-app-local` source
- **THEN** the system registers the managed `Documents/RhythHaus` directory with the stable `ios-app-local` identity
- **AND** the system scans that source once through the existing App-owned coordinator
- **AND** supported audio already present in the directory is available without Files.app reimport

#### Scenario: Preserve an existing managed source
- **WHEN** RhythHaus starts on iOS with an existing `ios-app-local` source
- **THEN** the system preserves its persisted source record and does not create a second managed source

### Requirement: Managed-directory selection is scan-only

The system SHALL not copy files that are already inside RhythHaus's managed iOS directory when that directory, one of its descendants, or an audio file inside it is selected in Files.app.

#### Scenario: Select the managed directory
- **WHEN** the user selects `Documents/RhythHaus` in Files.app
- **THEN** the system returns the existing `ios-app-local` source for scanning
- **AND** it does not recursively copy the directory into itself
- **AND** it does not create renamed copies or report those files as newly imported

#### Scenario: Select a managed audio file
- **WHEN** the user selects an audio file already inside `Documents/RhythHaus`
- **THEN** the system returns the existing `ios-app-local` source for scanning
- **AND** leaves that file in place without creating a duplicate copy

### Requirement: Import progress and result

The system SHALL expose import state while copying and a terminal result after completion, cancellation, or failure.

#### Scenario: Observe an active import
- **WHEN** multiple files are being copied
- **THEN** the UI exposes active progress and prevents competing source mutations until the import settles

#### Scenario: Complete an import
- **WHEN** copying finishes with at least one imported, duplicate, or already-managed supported file
- **THEN** the system reports counts for imported, duplicate, unsupported, and failed items
- **AND** offers the existing scan flow for the managed iOS source

### Requirement: Import privacy boundary

The system SHALL copy only user-selected local documents into private app storage and SHALL not upload, index, or export their audio contents as part of import.

#### Scenario: Import remains local
- **WHEN** an import completes
- **THEN** all imported audio bytes remain on the device in RhythHaus-managed storage
- **AND** the import result contains no external URL that is required for future playback
