## ADDED Requirements

### Requirement: Artwork collapse follows scroll distance
Artwork-backed album and artist Track List pages SHALL collapse the expanded artwork toward the collapsed TopBar height by the same number of pixels as the consumed upward scroll distance before normal track-list scrolling begins.

#### Scenario: Partial upward drag
- **WHEN** the user drags upward by less than the remaining artwork collapse range
- **THEN** the artwork and track-content position SHALL both move upward by that consumed distance and the track list SHALL NOT consume that portion

#### Scenario: Collapse range exhausted
- **WHEN** upward movement reaches the collapsed TopBar height
- **THEN** subsequent upward movement SHALL scroll the track list normally without shrinking the artwork below the collapsed height

### Requirement: Artwork and content remain spatially coupled
Artwork-backed Track List pages MUST derive visible chrome height and track-content top placement from the same clamped collapse geometry.

#### Scenario: Intermediate collapse position
- **WHEN** the artwork is partially collapsed
- **THEN** the first track-list content SHALL begin immediately after the current visible header region without retaining the fully expanded reservation

#### Scenario: Geometry changes
- **WHEN** width, density, safe-area inset, or orientation changes
- **THEN** the current collapse offset SHALL be clamped to the new geometry and chrome and content SHALL render from the same updated snapshot

#### Scenario: Zero or inverted range
- **WHEN** expanded artwork height is less than or equal to collapsed TopBar height
- **THEN** the page SHALL render at collapsed height, consume no artwork-collapse scroll, and avoid invalid normalized progress

### Requirement: Artwork expands only at the list start
Artwork-backed Track List pages SHALL restore the collapsed artwork from positive post-scroll movement after the child list can no longer consume movement toward its start.

#### Scenario: Downward movement while list can scroll back
- **WHEN** the track list is away from its start and consumes downward movement
- **THEN** the artwork SHALL remain collapsed until the list reaches its start

#### Scenario: Downward movement at list start
- **WHEN** the list is at its start and positive movement remains available
- **THEN** the artwork and content SHALL expand together by the consumed distance up to the expanded height

### Requirement: No-artwork behavior remains Miuix-owned
Album and artist Track List pages without representative artwork SHALL retain the existing Miuix large-title nested-scroll connection, glass chrome, content-padding policy, divider, and title behavior.

#### Scenario: Open a page without artwork
- **WHEN** an album or artist drill-down has no representative artwork identity or eager artwork bytes
- **THEN** the page SHALL use the existing Miuix scroll behavior and SHALL NOT attach the app-owned artwork-collapse connection

### Requirement: Existing drill-down functions remain intact
The change SHALL preserve album and artist navigation, safe insets, back interaction, title presentation, artwork loading, track ordering, playback selection, scrollbar behavior, and Now Playing spacing.

#### Scenario: Use an artwork-backed drill-down
- **WHEN** the user navigates, selects a track, uses the back button, or scrolls after full collapse
- **THEN** those functions SHALL behave as before except for the corrected coordinated artwork-collapse motion
