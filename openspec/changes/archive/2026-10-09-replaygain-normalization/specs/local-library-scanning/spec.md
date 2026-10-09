# Spec Delta

## ADDED Requirements

### Requirement: Persist scanned ReplayGain metadata

The scanner SHALL persist nullable track/album ReplayGain gain and peak facts from Android descriptors/content URIs and iOS/macOS files without writing media. Migration SHALL initialize facts as absent and preserve identity and user state. Display overrides SHALL NOT replace audio facts.

#### Scenario: Existing database migrates
- **WHEN** a pre-ReplayGain library is opened
- **THEN** tracks, sources, favorites, history, metadata overrides and playlist relationships are preserved
- **AND** new ReplayGain facts are absent until a scan supplies them.

#### Scenario: Tagged media is scanned across platforms
- **WHEN** supported media with ReplayGain tags is scanned through Android SAF or MediaStore, iOS local files, or macOS references
- **THEN** valid gain and peak facts are stored for that stable track identity
- **AND** malformed optional tags do not discard otherwise readable metadata or fail playback.

#### Scenario: Rescan removes or changes tags
- **WHEN** a successful metadata read observes changed or removed ReplayGain tags
- **THEN** stored facts update or clear accordingly without duplicating tracks or altering user-owned state.

#### Scenario: Recoverably unreadable media
- **WHEN** a previously stored track cannot be read during a recoverable scan event
- **THEN** existing ReplayGain facts follow the existing metadata-preservation behavior and are not cleared merely because reading failed.

#### Scenario: Display corrections
- **WHEN** a listener edits or restores title, artist, album, track or disc display fields
- **THEN** the scanned ReplayGain facts remain unchanged.
