## ADDED Requirements

### Requirement: Scrollable screens with NowPlayingBar control bar visibility

On every scrollable screen that renders a `NowPlayingBar`, the application SHALL hide the bar when the user scrolls or drags downward into content, and SHALL show the bar when the user scrolls or drags upward toward earlier content.

#### Scenario: Downward Library/Home scroll hides the NowPlayingBar
- **GIVEN** the user is on the main Library/Home list
- **AND** the `NowPlayingBar` is visible
- **WHEN** the user scrolls or drags downward into the list beyond the jitter threshold
- **THEN** the `NowPlayingBar` hides with a bottom-exit animation
- **AND** the hidden bar does not intercept pointer input

#### Scenario: Upward Library/Home scroll shows the NowPlayingBar
- **GIVEN** the user is on the main Library/Home list
- **AND** the `NowPlayingBar` is hidden
- **WHEN** the user scrolls or drags upward toward earlier content beyond the jitter threshold
- **THEN** the `NowPlayingBar` shows with a bottom-enter animation
- **AND** the bar's playback controls reflect the current playback state

#### Scenario: Downward track-list scroll hides the NowPlayingBar
- **GIVEN** the user is on an album or artist track-list screen
- **AND** the `NowPlayingBar` is visible
- **WHEN** the user scrolls or drags downward into the track list beyond the jitter threshold
- **THEN** the `NowPlayingBar` hides with a bottom-exit animation
- **AND** the hidden bar does not intercept pointer input

#### Scenario: Upward track-list scroll shows the NowPlayingBar
- **GIVEN** the user is on an album or artist track-list screen
- **AND** the `NowPlayingBar` is hidden
- **WHEN** the user scrolls or drags upward toward earlier track-list content beyond the jitter threshold
- **THEN** the `NowPlayingBar` shows with a bottom-enter animation

#### Scenario: Search result scrolling controls the root NowPlayingBar
- **GIVEN** the user is on Search with matching results
- **WHEN** the user scrolls the result list downward beyond the jitter threshold
- **THEN** the root `NowPlayingBar` hides with a bottom-exit animation
- **WHEN** the user scrolls the result list upward beyond the jitter threshold
- **THEN** the root `NowPlayingBar` shows with a bottom-enter animation

#### Scenario: Tiny scroll jitter does not toggle visibility
- **GIVEN** the user is on a scrollable screen that renders a `NowPlayingBar`
- **WHEN** the observed list scroll position changes only within the jitter threshold
- **THEN** the `NowPlayingBar` visibility remains unchanged

#### Scenario: Existing bar interactions are preserved when visible
- **GIVEN** the `NowPlayingBar` is visible
- **WHEN** the user taps the bar or drags upward on the bar with a track loaded
- **THEN** the existing Now Playing expand behavior remains available
- **AND** playback, Search, and Settings controls keep their existing behavior
