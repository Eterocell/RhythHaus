# Spec Delta

## Purpose

Allow listeners to correct indexed music metadata for use inside RhythHaus while retaining the scanned source values and leaving audio files untouched.

## ADDED Requirements

### Requirement: Track metadata can be edited without modifying audio

The system SHALL allow a listener to assign app-local title, artist, album, track number, and disc number to an indexed track, independently for each field. A blank text entry SHALL mean no override for that field; positive integers SHALL be required for overridden numbers. Editing SHALL NOT write to the audio file or modify its source handle, duration, artwork, or embedded tags.

#### Scenario: Save a partial correction
- **WHEN** a listener saves a changed title and leaves other fields at their scanned values
- **THEN** the effective title changes in the library
- **AND** the scanned title remains available for later restoration
- **AND** the other fields keep their scanned values

#### Scenario: Reject an invalid edit
- **WHEN** a listener submits a non-positive track number or a track no longer exists
- **THEN** no partial override is saved
- **AND** the editor retains the draft and explains the failure

### Requirement: Overrides remain independent of scanning

The system SHALL associate corrections with a stable indexed track identity and SHALL retain them across rescans of that track. Scans SHALL continue recording the latest source tags independently; reverting an override SHALL expose the latest scanned value, not an obsolete value.

#### Scenario: Changed tags under an override
- **WHEN** a source is rescanned after a corrected title was saved and the source title changed
- **THEN** the corrected title remains visible
- **AND** removing that title correction reveals the newly scanned title

#### Scenario: Removed track
- **WHEN** a track is removed by remove-missing, source removal, or clear-library
- **THEN** its override record is deleted with that track
- **AND** a subsequently imported track cannot inherit the deleted correction by filename alone

### Requirement: Effective metadata is used consistently

The system SHALL use effective metadata for Library rows and search, album and artist groups, sorting, saved and smart playlist rows, and new playback queue selections. Editing an indexed track SHALL refresh the visible projection without preempting a scan, deleting a playlist, or restarting audio. Currently playing and queued occurrences SHALL retain identity, ordering, progress, and transport mode when their visible metadata changes.

#### Scenario: Change artist and album
- **WHEN** a listener changes a track's artist and album
- **THEN** the track appears in the corrected artist and album groups and matching search results
- **AND** dynamic playlist rules evaluate the corrected values

#### Scenario: Edit the currently playing track
- **WHEN** the current track receives a title override
- **THEN** Now Playing shows the corrected title without restarting playback or changing queue identity and progress
- **AND** system media metadata refreshes immediately where the platform supports a non-disruptive metadata update, otherwise on the next track load; the app SHALL NOT reload playing audio merely to update a system label

### Requirement: Overrides are reversible and accessible

The system SHALL offer per-field restoration and a way to restore all overridden fields, show original scanned values while editing, and label editor actions in English and Simplified Chinese for accessibility.

#### Scenario: Restore only album
- **WHEN** a listener restores the album field of a track whose title and album were corrected
- **THEN** the scanned album appears and the corrected title remains

#### Scenario: Cancel without saving
- **WHEN** a listener dismisses the editor after making unsaved changes
- **THEN** persisted overrides and active playback remain unchanged
