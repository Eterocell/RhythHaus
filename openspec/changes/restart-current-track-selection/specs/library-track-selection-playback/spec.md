## ADDED Requirements

### Requirement: Selecting the current track restarts playback
The system SHALL restart the active track from position zero and ensure it is playing when the user selects that track from a Library or Search result row.

#### Scenario: Current playing track is selected
- **WHEN** the user selects the currently playing track row
- **THEN** playback seeks to position zero before continuing in the playing state
- **AND** the existing queue membership and order remain unchanged

#### Scenario: Current paused or stopped track is selected
- **WHEN** the user selects the current track while it is paused or stopped
- **THEN** playback restarts from position zero and enters the playing state

#### Scenario: Current track is selected while loading
- **WHEN** the user selects the current track while its media is still loading
- **THEN** the pending load remains selected at position zero and auto-plays when loading completes
- **AND** the system does not seek a previously loaded engine item

#### Scenario: Current track is selected from another list
- **WHEN** the active track appears in a different visible list and the user selects it there
- **THEN** playback restarts without replacing the existing queue with that visible list

### Requirement: Selecting another track uses the visible list as the queue
The system SHALL replace the playback queue with the exact membership and ordering of the visible track list when the user selects a non-current track, then SHALL start the selected track from position zero.

#### Scenario: Track is selected from Library home
- **WHEN** the user selects a non-current track from the Library home Songs list
- **THEN** the queue matches the rendered Songs list and the selected track starts playing

#### Scenario: Track is selected from album or artist detail
- **WHEN** the user selects a non-current track from an album or artist detail list
- **THEN** the queue matches that rendered detail list and the selected track starts playing

#### Scenario: Track is selected from Search
- **WHEN** the user selects a non-current filtered Search result
- **THEN** the queue matches the current filtered results in rendered order and the selected track starts playing

#### Scenario: Selected ID is not in the visible list
- **WHEN** a stale selection references a track ID absent from the supplied visible list
- **THEN** playback and queue state remain unchanged

### Requirement: Track selection preserves playback modes and transport controls
The system SHALL preserve repeat and shuffle mode values during track selection and SHALL keep dedicated play/pause controls independent from track-row restart behavior.

#### Scenario: Selection occurs with repeat or shuffle enabled
- **WHEN** the user selects the current or another track while repeat or shuffle is enabled
- **THEN** the configured repeat and shuffle mode values remain unchanged

#### Scenario: Dedicated play pause control is activated
- **WHEN** the user activates a Now Playing play/pause control
- **THEN** the control toggles playback without restarting the track or replacing the queue
