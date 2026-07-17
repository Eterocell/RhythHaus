## ADDED Requirements

### Requirement: Playlist hub and detail navigation
The system SHALL expose a Library playlist hub with `Saved` and `Queue` tabs, and SHALL navigate to saved-playlist detail using a route keyed by playlist ID. The Saved tab MUST list playlists in deterministic repository order with name and entry count, plus a create action and empty state. An unresolved detail route MUST return to the hub with a recoverable message.

#### Scenario: Open a saved playlist
- **WHEN** the user selects a playlist in the Saved tab
- **THEN** the system SHALL show that playlist's name and ordered entries on its keyed detail route

#### Scenario: Recover from a stale playlist route
- **WHEN** a playlist detail route no longer resolves to a saved playlist
- **THEN** the system MUST return to the playlist hub and present an actionable message

### Requirement: Durable playlist CRUD and error states
The system SHALL create, rename, and delete durable saved playlists through `PlaylistRepository` sharing the existing `RhythHausDatabase`. Names MUST be trimmed and non-empty, and names MUST NOT be required to be unique. Delete MUST require confirmation and return to the hub after success. Failed reads and mutations MUST retain the last confirmed state and expose retry or a recoverable message; failed create and rename MUST retain entered text.

#### Scenario: Create playlists with the same name
- **WHEN** the user confirms creation of two non-empty playlists with the same trimmed name
- **THEN** the system SHALL persist two distinct playlists and list both in deterministic order

#### Scenario: Reject a blank rename
- **WHEN** the user confirms a rename containing only whitespace
- **THEN** the system MUST reject it before repository mutation and retain the entered text

#### Scenario: Handle a failed playlist mutation
- **WHEN** a create, rename, delete, add, remove, or reorder repository mutation fails
- **THEN** the system MUST keep the last confirmed playlist state and expose a recoverable error

### Requirement: Ordered duplicate playlist entries
The system SHALL persist each saved-playlist entry as an independently identified occurrence with playlist ID, library-track ID, and zero-based position. The system MUST allow the same library track to occur multiple times in one playlist. Add MUST append after the final entry. Remove and reorder MUST rewrite a deterministic contiguous order in one transaction and MUST publish the resulting state only after that transaction succeeds.

#### Scenario: Add a duplicate track occurrence
- **WHEN** the user adds a track already present in a saved playlist
- **THEN** the system SHALL append a new entry with a distinct occurrence ID

#### Scenario: Reorder entries transactionally
- **WHEN** the user reorders an entry in a saved playlist
- **THEN** the system MUST persist one contiguous final position sequence without exposing partial ordering

### Requirement: Both saved-playlist add workflows
The system SHALL offer `Add to playlist` from Library home, Search, album detail, and artist detail track rows. That action MUST open an existing-playlist picker with inline playlist creation and append a new occurrence on confirmation. Playlist detail SHALL offer a searchable multi-select browser over the authoritative library; confirmation MUST append one occurrence for each selected track in the browser's visible order, with independent entry IDs.

#### Scenario: Add a row through the playlist picker
- **WHEN** the user confirms `Add to playlist` for a library row and a selected playlist
- **THEN** the system SHALL append a new occurrence to that playlist

#### Scenario: Add multiple rows from playlist detail
- **WHEN** the user confirms selected tracks in the searchable playlist-detail browser
- **THEN** the system MUST append one entry per selection in the browser's visible order

### Requirement: Cascade cleanup and empty-playlist retention
The system SHALL cascade delete entries when their playlist is deleted and MUST cascade delete entries that reference a deleted library track while retaining the containing playlist. Source removal, rescan cleanup, and clear-library operations MUST perform library-track deletion and entry cascades atomically. A playlist whose final entry is removed MUST remain as an empty playlist.

#### Scenario: Delete a referenced library track
- **WHEN** a library track referenced by one or more playlist entries is deleted
- **THEN** the system MUST remove each matching entry while retaining each affected playlist

#### Scenario: Delete the final playlist entry
- **WHEN** the user removes the only entry from a saved playlist
- **THEN** the system SHALL retain the saved playlist and show it as empty

### Requirement: Saved-playlist accessibility
The system SHALL use entry occurrence IDs as saved-row keys so duplicate tracks remain independently targetable. Interactive controls MUST have semantic labels that include the relevant playlist or track name. Reordering MUST provide accessible move-up and move-down controls in addition to drag interaction, and destructive actions MUST require confirmation without relying on color alone.

#### Scenario: Reorder without drag interaction
- **WHEN** an assistive-technology user invokes the labeled move-down control for a playlist entry
- **THEN** the system SHALL reorder that occurrence and announce a targetable updated row
