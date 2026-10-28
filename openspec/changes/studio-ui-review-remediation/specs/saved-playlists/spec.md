# Spec Delta

## MODIFIED Requirements

### Requirement: Save the visible playback queue as a playlist
In the player-accessible Queue surface, the system SHALL offer a localized, accessible Save queue as playlist action only while a current occurrence exists. Opening the workflow SHALL capture the displayed current-plus-upcoming occurrences once. Confirmation SHALL create exactly that ordered sequence using the existing nonblank-name validation. The save operation SHALL NOT mutate active playback state.

#### Scenario: Save an ordered queue with duplicate tracks
- **WHEN** the visible queue is current A, upcoming B, upcoming A, and the listener names and saves it
- **THEN** one saved playlist SHALL contain three independent entries A, B, A in that order, and the active playback queue SHALL remain unchanged

#### Scenario: Saving does not mutate playback state
- **WHEN** the listener confirms a saved queue
- **THEN** playback selection, queue contents, progress, repeat, shuffle, timer, files, tags, and source access SHALL remain unchanged

#### Scenario: Historical prefix excluded
- **WHEN** an earlier queue occurrence precedes the selected current occurrence
- **THEN** saving SHALL exclude that earlier occurrence and include current plus upcoming occurrences

#### Scenario: Queue changes during naming
- **WHEN** the listener opens the name dialog and the playback queue later changes before confirmation
- **THEN** the playlist SHALL contain the sequence captured on opening the dialog, not a mixture of old and new queue states

#### Scenario: Playback advances during naming
- **WHEN** playback changes the current occurrence while the listener is naming a queue snapshot
- **THEN** the Queue surface and name dialog SHALL remain available
- **AND** confirmation SHALL save the original captured current-plus-upcoming sequence, not a newly computed sequence

#### Scenario: Empty or unselected queue
- **WHEN** the queue has no current occurrence
- **THEN** the save action SHALL not be offered and no empty playlist SHALL be created by this workflow

#### Scenario: Cancel or failed save
- **WHEN** the listener cancels naming
- **THEN** no playlist SHALL be created
- **WHEN** persistence fails, including a track that disappeared after the snapshot
- **THEN** no partial playlist SHALL remain, and the dialog SHALL retain the draft and display an error for retry or cancellation

#### Scenario: Reach saving through the player
- **WHEN** the listener opens Queue from the player rather than a playlist-hub tab
- **THEN** the same queue-save snapshot and transaction SHALL be available without restoring the old Queue/Saved tab layout
