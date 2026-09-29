# Spec Delta

## ADDED Requirements

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
