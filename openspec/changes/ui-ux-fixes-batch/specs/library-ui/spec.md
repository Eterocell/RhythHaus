## ADDED Requirements

### Requirement: Empty Home exposes import action

When the local library has no tracks, Home SHALL display a primary Add music folder action without requiring the user to open Settings.

#### Scenario: Empty library onboarding
- **WHEN** the library contains zero tracks
- **THEN** Home displays an Add music folder action
- **AND** the action uses the existing platform folder picker launcher
- **AND** Settings remains available from the bottom bar

### Requirement: Album grid adapts to available width

Album browsing SHALL adapt the number of album columns to the available shared Compose width.

#### Scenario: Phone-width album grid
- **WHEN** the album section width is under 560dp
- **THEN** album cards render in 2 columns

#### Scenario: Tablet-width album grid
- **WHEN** the album section width is at least 560dp and under 900dp
- **THEN** album cards render in 3 columns

#### Scenario: Desktop-width album grid
- **WHEN** the album section width is at least 900dp
- **THEN** album cards render in 4 columns

### Requirement: Songs browse mode

The shared library browser SHALL provide Songs alongside Albums and Artists.

#### Scenario: Browse all songs
- **WHEN** the user selects Songs browse mode
- **THEN** Home displays individual track rows for the current library tracks
- **AND** tapping a song row starts or toggles playback using the full-library queue
- **AND** Albums and Artists browse modes remain available

### Requirement: Search result selection returns to origin

Search result selection SHALL start playback and dismiss the Search route.

#### Scenario: Select search result
- **WHEN** Search is open from any route
- **AND** the user taps a matching track result
- **THEN** the track starts playback using the full-library queue
- **AND** Search dismisses to the route that opened it

### Requirement: Search query can be cleared

Search SHALL provide a visible clear action while the query is non-empty.

#### Scenario: Clear search query
- **WHEN** the Search query contains text
- **THEN** a clear action is visible in the search field
- **WHEN** the user activates the clear action
- **THEN** the query becomes empty
- **AND** result count/results are hidden until a new non-blank query is entered

### Requirement: Compact controls meet touch target floor

Shared compact navigation/action controls SHALL provide at least a 44dp effective hit target where they are used as standalone controls.

#### Scenario: Back chip touch target
- **WHEN** the shared BackChip renders
- **THEN** its effective height is at least 44dp
- **AND** it keeps the visible `‹ Back` label

#### Scenario: Bottom bar search and settings touch targets
- **WHEN** the mini now-playing bottom bar renders Search and Settings controls
- **THEN** each control has an effective width and height of at least 44dp
- **AND** the icon content descriptions remain Search and Settings

### Requirement: Developer panels are not user-facing

Normal user-facing UI SHALL NOT render TagLib developer metadata panels.

#### Scenario: Expanded now playing is normal user UI
- **WHEN** Now Playing renders for a library track
- **THEN** it does not show `DEV · TagLib`
- **AND** it does not show a TagLib property map panel
- **AND** playback controls, artwork, title, artist/album, and scrubber remain visible
