## ADDED Requirements

### Requirement: Shared playback control contract
The system SHALL expose a shared playback control contract that supports loading a local track, play, pause, stop, seek, and selecting a track from a queue without duplicating business logic per platform.

#### Scenario: Play a selected local track
- **WHEN** a user selects a playable local track and requests playback
- **THEN** the system starts playback through the current platform engine and publishes a playing state for the selected track

#### Scenario: Pause current playback
- **WHEN** audio is playing and the user requests pause
- **THEN** the system pauses playback and publishes a paused state without losing the current track or position

#### Scenario: Seek within current track
- **WHEN** a user seeks to a valid position in the current track
- **THEN** the system moves playback to that position and publishes the updated position

### Requirement: Observable playback state
The system SHALL publish playback state from a shared model that includes current track identity, queue identity when available, playback status, current position, duration when known, buffering state when known, and user-visible errors.

#### Scenario: Playback progress updates
- **WHEN** a track is playing
- **THEN** the shared playback state updates position over time so the shared UI can render progress

#### Scenario: Playback error is visible
- **WHEN** a platform engine cannot load or play the selected local track
- **THEN** the shared playback state exposes a user-visible error and returns to a recoverable non-playing state

### Requirement: Platform playback engines
The system SHALL implement playback through platform-specific engines hidden behind the shared playback contract for Android, iOS, and macOS/desktop JVM.

#### Scenario: Android playback engine
- **WHEN** the Android app plays a supported local track
- **THEN** playback is handled by the Android-specific engine while shared UI and state remain platform-neutral

#### Scenario: iOS playback engine
- **WHEN** the iOS app plays a supported local track
- **THEN** playback is handled by the iOS-specific engine while shared UI and state remain platform-neutral

#### Scenario: macOS playback engine
- **WHEN** the macOS desktop app plays a supported local track
- **THEN** playback is handled by the JVM/macOS-specific engine while shared UI and state remain platform-neutral

### Requirement: Foreground playback lifecycle
The system SHALL support foreground in-app playback for the first implementation and SHALL handle app/screen lifecycle events enough to avoid leaking platform player resources.

#### Scenario: Player resources are released
- **WHEN** the app or playback controller is disposed
- **THEN** the platform player releases resources and stops progress updates

#### Scenario: Background playback is not promised initially
- **WHEN** the app moves to background during the first playback implementation
- **THEN** the system does not guarantee continued background playback unless a later background-audio change has been specified and implemented

### Requirement: Shared now-playing UI integration
The system SHALL allow the shared Compose UI to render now-playing controls from the shared playback state and dispatch user playback intents to the shared controller.

#### Scenario: Controls reflect playback status
- **WHEN** playback status changes between playing, paused, buffering, stopped, or error
- **THEN** the shared now-playing UI reflects the current status and available actions

#### Scenario: UI remains shared across platforms
- **WHEN** playback controls are displayed on Android, iOS, and macOS
- **THEN** the product UI is implemented primarily in shared Compose code rather than separately in each platform app module
