# Track Metadata Overrides

## Purpose

Keep listener-owned corrections separate from scanned media metadata so library presentation can be corrected without modifying audio files.

## Requirements

### Requirement: Track metadata can be edited without modifying audio

The system SHALL allow a listener to assign app-local title, artist, album, track number, and disc number to an indexed track independently. Blank text means no override; overridden numbers are positive integers. Editing SHALL NOT write to audio files or modify source handles, duration, artwork, or embedded tags.

#### Scenario: Save a partial correction
- **WHEN** a listener saves a changed title and leaves other fields unchanged
- **THEN** the effective title changes while the scanned title remains restorable
- **AND** other fields retain their scanned values

#### Scenario: Reject an invalid edit
- **WHEN** a listener submits an invalid number or a track no longer exists
- **THEN** no partial override is saved and the draft remains with an explanation

### Requirement: Overrides remain independent of scanning

The system SHALL associate corrections with stable indexed track identity and retain them across rescans. Reverting an override SHALL expose the latest scanned value. Removing a track SHALL remove its correction atomically.

#### Scenario: Changed tags under an override
- **WHEN** a source is rescanned after a corrected title was saved and its source title changed
- **THEN** the corrected title remains visible, and removing the correction reveals the newly scanned title

#### Scenario: Removed track
- **WHEN** remove-missing, source removal, or clear-library deletes a track
- **THEN** its correction is deleted and a later filename-only replacement does not inherit it

### Requirement: Effective metadata is used consistently

The system SHALL use effective metadata for Library rows, search, groups, sorting, saved and smart playlist rows, and new playback queues. Editing SHALL refresh visible projections without preempting scans, deleting playlists, or restarting audio. Current and queued occurrences retain identity, order, progress, and transport modes.

#### Scenario: Change artist and album
- **WHEN** a listener changes artist and album
- **THEN** corrected groups, search results, and dynamic rules use those values

#### Scenario: Edit the currently playing track
- **WHEN** the current track receives a title override
- **THEN** Now Playing and supported system metadata update without reloading audio, with unsupported system labels updated on the next load

### Requirement: Overrides are reversible and accessible

The system SHALL offer per-field and restore-all actions, show scanned values while editing, and label editor controls in English and Simplified Chinese.

#### Scenario: Restore only album
- **WHEN** a listener restores only album
- **THEN** scanned album appears while other corrections remain

#### Scenario: Cancel without saving
- **WHEN** a listener dismisses unsaved changes
- **THEN** persisted overrides and playback remain unchanged
