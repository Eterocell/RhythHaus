## ADDED Requirements

### Requirement: Backdrop liquid glass chrome

Library/Home and album/artist track-list pages SHALL render the scroll-reactive top chrome with Kyant0 Backdrop liquid-glass effects instead of a plain scrim or Haze effect.

#### Scenario: Library scroll activates Backdrop top chrome
- **WHEN** the user scrolls the Library/Home list downward from the top
- **THEN** the top chrome progressively becomes visible using a Backdrop glass surface
- **AND** the chrome remains bounded to the status bar plus toolbar height
- **AND** the existing Library content, scanner, browse mode, playback, and route navigation behavior remain available

#### Scenario: Track-list scroll activates Backdrop top chrome
- **WHEN** the user scrolls an album or artist track-list page downward from the top
- **THEN** the top chrome progressively becomes visible using a Backdrop glass surface
- **AND** the chrome remains bounded to the status bar plus toolbar height
- **AND** the existing back control, track selection, playback, scrollbar, and NowPlayingBar behavior remain available

#### Scenario: Top position remains visually quiet
- **WHEN** a supported scrollable page is at the top of its list
- **THEN** the top chrome is visually quiet and does not obscure the page header

### Requirement: Backdrop liquid glass bottom bar

The shared bottom `NowPlayingBar` SHALL render its rounded container with Kyant0 Backdrop liquid-glass effects while preserving current mini-player behavior.

#### Scenario: Track-loaded bottom bar uses Backdrop glass
- **WHEN** a track is loaded and the bottom `NowPlayingBar` is visible
- **THEN** the bar's rounded card surface uses a Backdrop glass effect
- **AND** artwork, track text, progress, play/pause, tap-to-expand, and drag-up expansion behavior remain unchanged

#### Scenario: Empty-library bottom bar uses Backdrop glass
- **WHEN** no track is loaded and the bottom bar renders Search and Settings controls
- **THEN** the bar's rounded card surface uses a Backdrop glass effect
- **AND** Search and Settings controls remain available with their existing content descriptions and hit targets

#### Scenario: Bottom bar scroll visibility remains unchanged
- **WHEN** scroll direction hides or shows the bottom bar
- **THEN** the existing bottom-enter/bottom-exit animation and pointer-interception behavior remain unchanged
- **AND** the Backdrop effect does not change navigation-bar padding or list bottom clearance
