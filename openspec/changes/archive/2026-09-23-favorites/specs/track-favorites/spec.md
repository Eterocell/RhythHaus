## Purpose

Provide a durable, low-friction way to mark local tracks for repeated listening across the existing library and Now Playing surfaces without changing their files, metadata, source access, or playback queue.

## ADDED Requirements

### Requirement: Favorites persist independently of scanned metadata

The system SHALL persist whether a library track is a favorite by stable library-track identity, independently of scanned audio metadata and source location details.

#### Scenario: Favorite survives application restart

- **WHEN** a user marks an existing library track as a favorite and restarts the app
- **THEN** the same persisted track is shown as a favorite after library restoration

#### Scenario: Rescan preserves a favorite on an existing track

- **WHEN** a source rescan updates a discovered track by its existing source-local identity
- **THEN** its favorite state remains unchanged

#### Scenario: Removed track no longer has a favorite relationship

- **WHEN** a track is removed because its source is removed, the library is cleared, or remove-missing deletes it
- **THEN** its favorite relationship is removed with that track
- **AND** no stale favorite is presented if a later scan discovers a new track

### Requirement: Favorite state is immediately coherent across presentation surfaces

The system SHALL render the latest persisted favorite state consistently wherever the same available track is presented.

#### Scenario: Toggling from a library track row updates current views

- **WHEN** a user toggles a track's favorite state from a song, album, or artist presentation
- **THEN** the control communicates the new checked state accessibly
- **AND** all visible presentations of that same track reflect the persisted state without an application restart

#### Scenario: Toggling from Now Playing updates the library

- **WHEN** a user toggles the current available track's favorite state from Now Playing
- **THEN** the Now Playing control communicates the new checked state accessibly
- **AND** the next Library presentation reflects the persisted state

#### Scenario: Unknown or removed track cannot create a favorite

- **WHEN** a favorite toggle is requested for a track absent from the authoritative library
- **THEN** the request has no observable mutation
- **AND** the UI does not present a persisted favorite for that absent track

### Requirement: Favorites are browsable without changing playback selection semantics

The system SHALL provide a Favorites library presentation containing every currently available favorite track in the standard library track ordering.

#### Scenario: Favorites contains only favorite tracks

- **WHEN** a user selects the Favorites browse presentation
- **THEN** it contains every and only currently available favorite track
- **AND** an empty state communicates that no tracks are favorited

#### Scenario: Favoriting and unfavoriting changes Favorites membership

- **WHEN** a user favorites a currently available track
- **THEN** it becomes available in Favorites
- **WHEN** the user unfavorites it
- **THEN** it is no longer available in Favorites

#### Scenario: Playing from Favorites uses the visible Favorites queue

- **WHEN** a user plays a track from Favorites
- **THEN** playback uses the existing library selection behavior with the Favorites presentation's visible tracks as its queue
- **AND** toggling favorite state does not otherwise alter current playback, queue order, repeat, shuffle, or progress

### Requirement: Favorite actions do not mutate media or source access

The system SHALL treat favorite actions as user-library state only.

#### Scenario: Favorite action has no file or source side effect

- **WHEN** a user favorites or unfavorites a track
- **THEN** the system does not modify the original media file, embedded metadata, source records, source permissions, scan sessions, or playback session
