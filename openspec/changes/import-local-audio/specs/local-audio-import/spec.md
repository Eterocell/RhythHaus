## ADDED Requirements

### Requirement: Manual local audio import
The system SHALL provide a first manual local-audio import path that can add playable local audio handles to the shared library UI without requiring a full library scanner.

#### Scenario: Import local audio files
- **WHEN** a user chooses the import action on a platform with an available importer
- **THEN** the system allows selecting one or more supported audio files and adds them to the shared library as playable tracks

#### Scenario: Imported tracks are playable
- **WHEN** an imported track is selected and the user requests playback
- **THEN** the shared playback controller loads the imported track source instead of a demo metadata-only source

### Requirement: Platform import boundaries
The system SHALL keep platform-specific picker and file-handle behavior behind a shared import contract.

#### Scenario: Android document import
- **WHEN** the Android app imports audio
- **THEN** document picker results are represented as stable URI-based audio sources for shared playback

#### Scenario: macOS desktop file import
- **WHEN** the macOS desktop app imports audio
- **THEN** selected files are represented as file-path audio sources for shared playback

#### Scenario: Unsupported importer is visible
- **WHEN** a platform importer is not available in the first slice
- **THEN** the shared UI exposes a recoverable limitation message rather than failing silently

### Requirement: Metadata-lite imported tracks
The system SHALL create displayable track rows from imported file handles even when rich audio metadata is unavailable.

#### Scenario: Track display from file name
- **WHEN** a local file is imported without parsed metadata
- **THEN** the system derives a user-visible title from the file name or display name and uses placeholder artist/album values
