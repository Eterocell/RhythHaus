## MODIFIED Requirements

### Requirement: Shared playback control contract
The system SHALL expose a shared playback control contract that supports loading a local track, play, pause, stop, seek, selecting a track from a queue, repeat mode selection, and shuffle mode selection without duplicating business logic per platform.

#### Scenario: Play a selected local track
- **WHEN** a user selects a playable local track and requests playback
- **THEN** the system starts playback through the current platform engine and publishes a playing state for the selected track

#### Scenario: Pause current playback
- **WHEN** audio is playing and the user requests pause
- **THEN** the system pauses playback and publishes a paused state without losing the current track or position

#### Scenario: Seek within current track
- **WHEN** a user seeks to a valid position in the current track
- **THEN** the system moves playback to that position and publishes the updated position

#### Scenario: Select repeat mode
- **WHEN** a user changes repeat mode
- **THEN** the shared playback state exposes the selected repeat mode
- **AND** automatic completion and manual previous/next navigation use that repeat mode consistently

#### Scenario: Select shuffle mode
- **WHEN** a user enables shuffle for the current queue
- **THEN** the shared playback state exposes shuffle as enabled
- **AND** the controller generates a shuffled playback order containing every queued track exactly once while keeping the current track active

### Requirement: Observable playback state
The system SHALL publish playback state from a shared model that includes current track identity, queue identity when available, playback status, current position, duration when known, buffering state when known, selected repeat mode, selected shuffle mode, and user-visible errors.

#### Scenario: Playback progress updates
- **WHEN** a track is playing
- **THEN** the shared playback state updates position over time so the shared UI can render progress

#### Scenario: Playback error is visible
- **WHEN** a platform engine cannot load or play the selected local track
- **THEN** the shared playback state exposes a user-visible error and returns to a recoverable non-playing state

#### Scenario: Repeat and shuffle state updates
- **WHEN** repeat mode or shuffle mode changes
- **THEN** the shared playback state updates so the Now Playing UI can render the active modes

### Requirement: Shared now-playing UI integration
The system SHALL allow the shared Compose UI to render now-playing controls from the shared playback state and dispatch user playback intents to the shared controller.

#### Scenario: Controls reflect playback status
- **WHEN** playback status changes between playing, paused, buffering, stopped, or error
- **THEN** the shared now-playing UI reflects the current status and available actions

#### Scenario: UI remains shared across platforms
- **WHEN** playback controls are displayed on Android, iOS, and macOS
- **THEN** the product UI is implemented primarily in shared Compose code rather than separately in each platform app module

#### Scenario: Change repeat mode from Now Playing
- **WHEN** the user taps the repeat mode control on Now Playing
- **THEN** the shared controller cycles to the next repeat mode
- **AND** the Now Playing UI reflects the new repeat mode

#### Scenario: Change shuffle mode from Now Playing
- **WHEN** the user taps the shuffle control on Now Playing
- **THEN** the shared controller toggles shuffle on or off
- **AND** the Now Playing UI reflects the new shuffle mode

## ADDED Requirements

### Requirement: Repeat playback modes
The system SHALL support shared repeat behavior for single-track repeat, playlist repeat, play-current-song-then-stop, and play-current-list-then-stop.

#### Scenario: Single-track repeat completion
- **WHEN** repeat mode is single-track repeat and the current track completes
- **THEN** playback restarts the same track from the beginning and continues playing

#### Scenario: Playlist repeat completion
- **WHEN** repeat mode is playlist repeat and the final effective track completes
- **THEN** playback wraps to the first effective track and continues playing

#### Scenario: Play current song then stop
- **WHEN** repeat mode is play-current-song-then-stop and the current track completes
- **THEN** playback remains on the current track at the end and publishes a non-playing state
- **AND** playback does not reset the position to the beginning
- **AND** playback does not advance to another track

#### Scenario: Play current list then stop
- **WHEN** repeat mode is play-current-list-then-stop and a non-final effective track completes
- **THEN** playback advances to the next effective track and continues playing
- **WHEN** the final effective track completes
- **THEN** playback remains on the final track at the end and publishes a non-playing state
- **AND** playback does not wrap to the first track

#### Scenario: Manual transport respects repeat boundaries
- **WHEN** the user manually requests previous or next
- **THEN** playback follows the current effective order
- **AND** playback wraps at boundaries only when repeat mode is playlist repeat

### Requirement: Shuffle playback mode
The system SHALL support a shared shuffle mode for playing songs inside the current queue in a generated random order.

#### Scenario: Enable shuffle
- **WHEN** shuffle is enabled for a non-empty queue
- **THEN** the controller generates a shuffled effective order containing every queued track exactly once
- **AND** the current track remains the active track
- **AND** the visible library or browse list order is not changed

#### Scenario: Navigate while shuffled
- **WHEN** shuffle is enabled and the user requests previous, next, or the current track completes
- **THEN** playback navigation follows the shuffled effective order subject to the selected repeat mode

#### Scenario: Disable shuffle
- **WHEN** shuffle is disabled
- **THEN** playback navigation returns to the queue's original order from the current track

#### Scenario: Queue changes while shuffled
- **WHEN** the queue changes while shuffle is enabled
- **THEN** the controller regenerates the shuffled effective order for the new queue
- **AND** the selected/current track remains active when it still exists in the new queue
