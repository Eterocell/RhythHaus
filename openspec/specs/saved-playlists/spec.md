# Saved Playlists

## Purpose

Define the supported workflows for adding ordered track occurrences to saved playlists.

## Requirements

### Requirement: Both saved-playlist add workflows
The system SHALL enter page-scoped selection from Library home Songs, Search, album detail, and artist detail track rows instead of exposing per-row Add to Playlist buttons. The contextual Add to Playlist action MUST open the existing-playlist picker with inline playlist creation and append one new occurrence for every selected track on confirmation in the page's current visible order. Playlist detail SHALL continue to offer its searchable multi-select browser over the authoritative library; confirmation MUST append one occurrence for each selected track in the browser's visible order, with independent entry IDs.

#### Scenario: Add selected library rows through the playlist picker
- **WHEN** the user confirms Add to Playlist for an ordered page-scoped selection and a selected playlist
- **THEN** the system SHALL append one distinct occurrence per selected track in visible order

#### Scenario: Create a playlist from selected library rows
- **WHEN** the user confirms inline creation for an ordered page-scoped selection
- **THEN** the system SHALL create one playlist containing one distinct occurrence per selected track in visible order

#### Scenario: Add multiple rows from playlist detail
- **WHEN** the user confirms selected tracks in the searchable playlist-detail browser
- **THEN** the system MUST append one entry per selection in the browser's visible order

### Requirement: Save the visible playback queue as a playlist
On the Queue tab the system SHALL offer a localized, accessible Save queue as playlist action only while a current queue occurrence exists. Activating it SHALL freeze a snapshot of the current occurrence and every upcoming occurrence in displayed order, preserving repeated track entries while excluding occurrences before the current one. The listener SHALL provide a nonblank playlist name using the existing naming rules; a successful confirmation SHALL create one durable saved playlist with exactly those ordered entries. Cancelling SHALL create nothing. The action SHALL NOT change playback selection, queue contents, progress, repeat, shuffle, timer, files, tags, or source access.

#### Scenario: Save an ordered queue with duplicate tracks
- **WHEN** the visible queue is current A, upcoming B, upcoming A, and the listener names and saves it
- **THEN** one saved playlist SHALL contain three independent entries A, B, A in that order, and the active playback queue SHALL remain unchanged

#### Scenario: Historical prefix excluded
- **WHEN** an earlier queue occurrence precedes the selected current occurrence
- **THEN** saving SHALL exclude that earlier occurrence and include current plus upcoming occurrences

#### Scenario: Queue changes during naming
- **WHEN** the listener opens the name dialog and the playback queue later changes before confirmation
- **THEN** the playlist SHALL contain the sequence captured on opening the dialog, not a mixture of old and new queue states

#### Scenario: Empty or unselected queue
- **WHEN** the queue has no current occurrence
- **THEN** the save action SHALL not be offered and no empty playlist SHALL be created by this workflow

#### Scenario: Cancel or failed save
- **WHEN** the listener cancels naming
- **THEN** no playlist SHALL be created
- **WHEN** persistence fails, including a track that disappeared after the snapshot
- **THEN** no partial playlist SHALL remain, and the dialog SHALL retain the draft and display an error for retry or cancellation
