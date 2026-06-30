## ADDED Requirements

### Requirement: Transport and utility controls use vector icons

The shared mini-player and now-playing transport controls SHALL use a cohesive vector icon system for play, pause, previous, next, search, and settings actions instead of emoji/text glyphs.

#### Scenario: Mini-player controls render vector icons
- **WHEN** the mini-player renders play/pause, search, or settings controls
- **THEN** each control uses a Compose vector icon
- **AND** the control does not render `▶`, `⏸`, `🔍`, or `⚙️` as user-visible control text
- **AND** the existing click behavior is preserved

#### Scenario: Full now-playing transport renders vector icons
- **WHEN** the now-playing screen renders previous, play/pause, or next transport controls
- **THEN** each control uses a Compose vector icon
- **AND** the control does not render `⏮`, `▶`, `⏸`, or `⏭` as user-visible control text
- **AND** the existing playback behavior is preserved

### Requirement: Icon controls are accessible and theme-aware

Vector icon controls SHALL set explicit tint and content descriptions matching their action.

#### Scenario: Icon control accessibility
- **WHEN** a search, settings, play, pause, previous, or next icon is rendered
- **THEN** it has a content description for that action
- **AND** it uses the same theme-driven colors as the previous control styling
