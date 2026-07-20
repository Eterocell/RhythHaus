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
