## MODIFIED Requirements

### Requirement: Platform playback engines
The system SHALL implement playback through platform-specific engines hidden behind the shared playback contract for Android, iOS, and macOS/desktop JVM.

#### Scenario: Android playback engine
- **WHEN** the Android app plays a supported local track
- **THEN** playback is handled by the Android-specific engine while shared UI and state remain platform-neutral
- **AND** the product-grade Android backend SHOULD use Media3/ExoPlayer rather than platform `MediaPlayer`

#### Scenario: Android system media controls expose current track
- **WHEN** the Android app loads a playable local track with title, artist, album, and source information
- **THEN** the Android playback engine publishes that metadata through Media3 media items/sessions so Android system media controls can display the current track and offer transport controls
- **AND** transport and hardware media-button events delivered to the session SHALL be applied to the shared playback controller (see the `android-media-controls` capability)

#### Scenario: iOS system media controls expose current track
- **WHEN** the iOS app loads a playable local track with title, artist, album, elapsed position, and duration information
- **THEN** the iOS playback engine publishes that metadata through Apple's Now Playing info center so iOS system media controls can display the current track and offer transport controls
- **AND** this SHALL NOT by itself claim long-running background playback support

#### Scenario: macOS system media controls expose current track
- **WHEN** the macOS app loads a playable local track with title, artist, album, elapsed position, and duration information
- **THEN** the macOS playback helper publishes that metadata through Apple's Now Playing info center so macOS system media controls can display the current track and offer transport controls
- **AND** this SHALL NOT by itself claim long-running background playback support

#### Scenario: iOS playback engine
- **WHEN** the iOS app plays a supported local track
- **THEN** playback is handled by an iOS-native Apple audio backend while shared UI and state remain platform-neutral

#### Scenario: macOS playback engine
- **WHEN** the macOS desktop app plays a supported local track
- **THEN** playback is handled by a macOS-specific engine while shared UI and state remain platform-neutral
- **AND** the product-grade macOS backend SHOULD use native macOS audio APIs or a native bridge rather than Java Sound
