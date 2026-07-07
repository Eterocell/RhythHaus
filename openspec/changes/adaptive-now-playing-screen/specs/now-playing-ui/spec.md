## ADDED Requirements

### Requirement: Adaptive expanded Now Playing layout

The expanded Now Playing screen SHALL render a compact vertical layout on phone-sized windows and a split artwork/controls layout on wide tablet/desktop-sized windows.

#### Scenario: Compact width preserves current vertical Now Playing layout
- **WHEN** the expanded Now Playing overlay is shown below the adaptive wide threshold
- **THEN** artwork, metadata, status, progress, playback mode controls, and transport controls are arranged in the existing vertical order
- **AND** left-edge swipe back, drag-to-collapse, and back dismissal behavior are preserved

#### Scenario: Wide width shows artwork and controls side by side
- **WHEN** the expanded Now Playing overlay is shown at least 840dp wide, or at least 600dp wide with height / width less than 1.2
- **THEN** the artwork or accent fallback is shown in a left pane
- **AND** track metadata, status, progress, shuffle/repeat controls, and transport controls are shown in a right pane
- **AND** the controls keep the same callbacks and content descriptions as compact mode

#### Scenario: Wide Now Playing does not add queue scope
- **WHEN** wide Now Playing is rendered
- **THEN** no queue, up-next, lyrics, or extra detail pane is introduced
- **AND** playback behavior remains limited to the existing seek, mode, and transport controls

#### Scenario: No external adaptive dependency is introduced
- **WHEN** the adaptive Now Playing implementation is built
- **THEN** it uses local shared Compose layout primitives
- **AND** it does not add `miuix-navigation3-adaptive` or Navigation3 adaptive dependencies
