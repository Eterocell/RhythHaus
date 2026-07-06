## ADDED Requirements

### Requirement: Nested scroll frosted chrome

Library and track-list pages SHALL render a scroll-reactive top chrome layer that creates a Backdrop/Haze-style frosted depth effect as content scrolls underneath it.

#### Scenario: Library scroll activates frosted chrome
- **WHEN** the user scrolls the Library/Home list downward from the top
- **THEN** the top chrome progressively becomes more visible
- **AND** the existing Library content, scanner, browse mode, playback, and NowPlayingBar behavior remain available

#### Scenario: Track-list scroll activates frosted chrome
- **WHEN** the user scrolls an album or artist track-list page downward from the top
- **THEN** the top chrome progressively becomes more visible
- **AND** the existing back control, track selection, playback, scrollbar, and NowPlayingBar behavior remain available

#### Scenario: Top position remains visually quiet
- **WHEN** a supported scrollable page is at the top of its list
- **THEN** the top chrome is visually quiet and does not obscure the page header
