## ADDED Requirements

### Requirement: Adaptive list-detail layout

RhythHaus shared Compose UI SHALL use an adaptive list-detail layout on wide tablet/desktop-sized windows while preserving the existing compact one-pane layout on phone-sized windows.

#### Scenario: Compact width preserves one-pane UI
- **WHEN** the app window is below the adaptive wide threshold
- **THEN** Library, album detail, artist detail, Search, Settings, Clear Library dialog, and Now Playing render with the existing compact one-pane behavior
- **AND** existing route animation, back behavior, bottom bar behavior, and playback behavior are preserved

#### Scenario: Wide width shows list and detail panes
- **WHEN** the app window is at least 840dp wide, or at least 600dp wide with height / width less than 1.2
- **THEN** Library/Home content is visible as the list pane
- **AND** album detail or artist detail can be visible beside it as the detail pane
- **AND** a placeholder detail pane is shown when no album or artist detail is selected

#### Scenario: Wide detail selection replaces the active detail pane
- **WHEN** the app is in wide list-detail mode
- **AND** the user selects an album or artist while another album or artist detail is already visible
- **THEN** the visible detail pane changes to the newly selected album or artist
- **AND** the app does not build an unnecessary deep stack of wide-screen detail panes

### Requirement: Miuix adaptive dependency attempt preserves current Miuix modules

The implementation SHALL attempt to use `top.yukonga.miuix.kmp:miuix-navigation3-adaptive:0.8.5` without downgrading existing Miuix modules from `0.9.2`.

#### Scenario: Adaptive artifact compiles with current Miuix modules
- **WHEN** Gradle resolves and compiles the shared module with `miuix-navigation3-adaptive:0.8.5`
- **THEN** existing Miuix modules remain on `0.9.2`
- **AND** the wide list-detail shell uses the adaptive artifact APIs

#### Scenario: Adaptive artifact cannot compile without downgrade
- **WHEN** dependency resolution or compilation requires downgrading existing Miuix modules from `0.9.2`
- **THEN** the implementation records the exact Gradle error or dependency conflict
- **AND** no Miuix downgrade is applied without explicit user approval

### Requirement: Miuix blur replaces Kyant Backdrop chrome

Top nested-scroll chrome and bottom NowPlayingBar glass surfaces SHALL use Miuix blur instead of Kyant Backdrop/Shapes.

#### Scenario: Chrome uses Miuix blur with fallback tint
- **WHEN** Library/Home or album/artist detail content scrolls under the top chrome
- **THEN** the top chrome draws with Miuix blur over the recorded content layer
- **AND** the chrome remains bounded to the status bar plus toolbar height
- **AND** unsupported or disabled runtime shader paths remain readable through the fallback/tint surface

#### Scenario: Bottom bar uses one Miuix blur surface
- **WHEN** the NowPlayingBar is visible
- **THEN** it draws as a single rounded Miuix blur-backed surface
- **AND** there is no duplicate nested panel layer
- **AND** playback controls, Search, Settings, tap-to-expand, drag-up expand, navigation-bar padding, and hide/show-on-scroll behavior are preserved

#### Scenario: Kyant dependencies are removed after replacement
- **WHEN** Miuix blur replacement compiles successfully
- **THEN** Kyant Backdrop and Kyant Shapes dependencies are no longer used by shared UI
- **AND** Kyant Backdrop imports are removed from RhythHaus source files
