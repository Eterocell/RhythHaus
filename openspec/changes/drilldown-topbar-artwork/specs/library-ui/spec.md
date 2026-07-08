## ADDED Requirements

### Requirement: Drill-down top bars show representative artwork

Album and artist drill-down track-list top bars SHALL show a representative embedded artwork image when one is available from the tracks in that detail list.

#### Scenario: Album detail top bar has artwork
- **GIVEN** an album detail route is displayed
- **AND** at least one track in that album has embedded artwork bytes that decode successfully
- **WHEN** the drill-down Miuix top bar is rendered
- **THEN** the top bar shows that artwork as a full-width square top bar before scroll and a compact rectangular top bar after scroll
- **AND** the album title remains the Miuix expanded large title and collapsed title
- **AND** the back navigation action remains available

#### Scenario: Artist detail top bar has artwork
- **GIVEN** an artist detail route is displayed
- **AND** at least one track for that artist has embedded artwork bytes that decode successfully
- **WHEN** the drill-down Miuix top bar is rendered
- **THEN** the top bar shows representative artwork as a full-width square top bar before scroll and a compact rectangular top bar after scroll
- **AND** the artist title remains the Miuix expanded large title and collapsed title
- **AND** the back navigation action remains available

#### Scenario: No artwork is available
- **GIVEN** an album or artist detail route is displayed
- **AND** no track in that detail list has decodable embedded artwork bytes
- **WHEN** the drill-down Miuix top bar is rendered
- **THEN** no artwork placeholder is added to the top bar
- **AND** the current title, back action, glass chrome, and list behavior remain unchanged

### Requirement: Artwork-filled top bars use cached artwork decoding

Drill-down top-bar artwork SHALL use the existing artwork decode cache for the artwork-filled top-bar surface.

#### Scenario: Compact artwork decoding
- **GIVEN** representative artwork bytes are passed to the drill-down top-bar chrome
- **WHEN** the image is decoded for the top bar
- **THEN** it uses `decodeArtworkCached`
- **AND** it uses cached decoding for this full-width top-bar artwork surface
- **AND** no persisted schema or dependency change is required

### Requirement: Scope remains limited to shared Library UI

The change SHALL preserve existing drill-down behavior outside the supplementary artwork image.

#### Scenario: Existing behavior is preserved
- **GIVEN** the change is complete
- **WHEN** the scoped diff is reviewed
- **THEN** changed implementation source is limited to shared Library Compose UI
- **AND** there are no Gradle dependency changes
- **AND** there are no scanner, TagLib, SQLDelight schema, playback-engine, or platform-source changes
- **AND** Miuix scroll behavior, nested scroll connection, left-edge swipe back, track rows, and Now Playing bar behavior remain unchanged

### Requirement: Artwork-filled top bars remove glass blur

When a drill-down top bar renders album or artist artwork as its background, it SHALL NOT apply the Miuix glass/blur surface to that artwork top bar.

#### Scenario: Decodable artwork is present
- **WHEN** the album or artist drill-down top bar has decodable artwork
- **THEN** the top bar renders the artwork as the full-width square top bar before scroll and compact rectangular top bar after scroll
- **AND** the top bar does not apply the glass/blur effect
- **AND** the back button has a circular background surface
- **AND** the album or artist title has a chip-style background in expanded and collapsed states

#### Scenario: No decodable artwork is present
- **WHEN** no artwork candidate decodes successfully
- **THEN** the top bar keeps the existing glass title presentation
- **AND** no artwork placeholder is rendered
