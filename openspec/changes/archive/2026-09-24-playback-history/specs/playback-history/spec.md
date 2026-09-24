# Playback History

## Purpose

Persist listener-owned play history for local tracks and present recent listening and recently added music without changing source access, playback controls, or recommendation scope.

## ADDED Requirements

### Requirement: Durable per-track play history

The system SHALL persist each existing library track's non-negative play count and most recent successful-play epoch timestamp independently of scanned metadata.

#### Scenario: First successful play creates history
- **WHEN** an indexed library track starts actual playback for the first time
- **THEN** the system persists play count `1` and that playback's epoch timestamp
- **AND** reopening the application retains both values

#### Scenario: Later successful play updates history
- **WHEN** a previously recorded library track starts actual playback again
- **THEN** the system increments its play count exactly once
- **AND** replaces its last-played timestamp with the newer successful-play timestamp

#### Scenario: Non-library playback is ignored
- **WHEN** playback starts for a queue track that is no longer an indexed library track
- **THEN** the system does not create or update a history record
- **AND** it does not report a false successful history update

### Requirement: Count actual playing generations once

The system SHALL record history only after a playback generation enters actual playing state, at most once for that generation.

#### Scenario: Loading and failure do not count
- **WHEN** a track is selected or loaded but never enters actual playing state because it is paused, replaced, stopped, or fails
- **THEN** its play count and last-played timestamp remain unchanged

#### Scenario: Repeated playing status is deduplicated
- **WHEN** the active playback engine reports `Playing` more than once for the same playback generation and occurrence
- **THEN** the system records one history event only

#### Scenario: Replay after a new generation counts again
- **WHEN** the listener restarts, retries, reselects, or advances to a track and the controller admits a distinct playback generation that enters actual playing state
- **THEN** the system records one additional history event for that newly admitted generation

### Requirement: Recent library presentations

The Library SHALL provide Recently Played and Recently Added browse modes in addition to existing Albums, Artists, Songs, and Favorites modes.

#### Scenario: Recently Played ordering
- **WHEN** the listener opens Recently Played
- **THEN** it shows only indexed tracks with history, ordered by last played descending
- **AND** ties use stable title and artist ordering

#### Scenario: Recently Added ordering
- **WHEN** the listener opens Recently Added
- **THEN** it shows indexed tracks ordered by library-added timestamp descending
- **AND** ties use stable title and artist ordering

#### Scenario: Recent mode queue behavior
- **WHEN** the listener starts a track from either recent browse mode
- **THEN** playback uses exactly the currently visible ordered recent list as its queue
- **AND** existing selection-mode and Back behavior remains consistent with Songs and Favorites

#### Scenario: Empty recent history
- **WHEN** no indexed track has history
- **THEN** Recently Played presents a normal localized empty state
- **AND** it does not present source-import or scan-management actions

### Requirement: Authoritative cross-surface history state

The system SHALL publish immutable history state together with the current library track projection so Library surfaces do not rely on stale local history mutations.

#### Scenario: Successful playback updates the library
- **WHEN** an admitted actual-playing generation records a history event
- **THEN** the next authoritative library publication reflects its updated count and last-played value
- **AND** Recently Played reorders from that authoritative publication

#### Scenario: Concurrent library lifecycle update
- **WHEN** a scan, remove-missing, source removal, or clear-library operation overlaps a history write
- **THEN** a stale publication cannot overwrite newer authoritative history or library content
- **AND** the final projection contains only history for surviving tracks

### Requirement: Accessible localized recent controls

The system SHALL localize Recently Played, Recently Added, and empty-history presentation in English and Simplified Chinese and expose browse controls with selected-state semantics.

#### Scenario: Browse mode accessibility
- **WHEN** a screen reader encounters the recent browse-mode controls
- **THEN** it announces each localized mode name and whether that mode is selected

#### Scenario: Empty-history accessibility
- **WHEN** Recently Played has no entries
- **THEN** the localized empty-state message is exposed as readable content
- **AND** no inactive import or scan action is exposed.