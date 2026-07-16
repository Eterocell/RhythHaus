## ADDED Requirements

### Requirement: Artwork collapse follows scroll distance
Artwork-backed album and artist Track List pages SHALL represent the artwork collapse range before track content in one lazy list, advancing the artwork/content boundary by the same number of pixels as upward list travel until the collapsed TopBar height is reached.

#### Scenario: Partial upward movement
- **WHEN** the user scrolls upward by less than the remaining artwork collapse range
- **THEN** the visible artwork region and following track-content boundary SHALL move upward by that distance while track rows SHALL NOT independently advance through their normal scrolling range

#### Scenario: Collapse range exhausted
- **WHEN** upward movement reaches the collapsed TopBar height
- **THEN** subsequent upward movement SHALL scroll track rows normally without shrinking the artwork below the collapsed height

### Requirement: Artwork and content remain spatially coupled
Artwork-backed Track List pages MUST represent expanded artwork, collapsed chrome, and track-content placement in one ordered lazy item sequence derived from the same current geometry.

#### Scenario: Intermediate collapse position
- **WHEN** the artwork is partially collapsed
- **THEN** the first track-list content SHALL remain immediately after the current visible header region without fixed expanded padding, translated viewport compensation, or a transient gap

#### Scenario: Geometry changes
- **WHEN** width, density, safe-area inset, or orientation changes
- **THEN** the artwork ranges and normalized progress SHALL be recomputed from current geometry without a stale independent collapse offset

#### Scenario: Zero or inverted range
- **WHEN** expanded artwork height is less than or equal to collapsed TopBar height
- **THEN** the page SHALL render one collapsed sticky artwork region, omit the upper collapse range, and avoid invalid normalized progress

### Requirement: Artwork expands only at the list start
Artwork-backed Track List pages SHALL restore collapsed artwork naturally as reverse list movement traverses the artwork items and returns to the list start.

#### Scenario: Reverse movement while rows can scroll back
- **WHEN** the track list is away from its start and reverse movement traverses track rows
- **THEN** the artwork SHALL remain collapsed until the artwork portion of the lazy sequence becomes visible

#### Scenario: Reverse movement reaches artwork
- **WHEN** reverse movement reaches the artwork portion of the lazy sequence
- **THEN** the artwork and content boundary SHALL expand by that movement up to the expanded height, reaching full expansion at item zero offset zero

### Requirement: Artwork pages have one vertical input owner
Artwork-backed Track List pages SHALL use one `LazyColumn` and its `LazyListState` as the sole vertical scroll and input owner.

#### Scenario: Scroll while pointing at artwork
- **WHEN** the user scrolls with a wheel or trackpad over any non-button artwork pixel
- **THEN** the same lazy list that owns the track rows SHALL receive the movement and advance the artwork collapse range

#### Scenario: Prevent competing scroll surfaces
- **WHEN** the artwork-backed page is composed
- **THEN** it SHALL NOT use an artwork nested-scroll adapter, a sibling vertical scrollable, dynamic content padding, translated viewport compensation, or a full-size overlay that intercepts artwork-zone scrolling

### Requirement: Expanded and collapsed artwork remain visually continuous
Artwork-backed Track List pages SHALL render the expanded square and collapsed sticky artwork toolbar from aligned portions of one square image placement.

#### Scenario: Fully expanded artwork
- **WHEN** the list is at item zero offset zero
- **THEN** the upper and lower artwork regions SHALL form one seamless square whose height equals the available drill-down width

#### Scenario: Fully collapsed artwork
- **WHEN** upward movement exhausts the collapse range
- **THEN** the lower artwork region SHALL remain pinned at the system inset plus 56 dp toolbar height while rows scroll normally

### Requirement: Collapsed artwork chrome becomes solid
The sticky artwork toolbar SHALL progressively apply a solid `HausColors.paper` background as collapse progress increases and SHALL NOT use RhythHaus liquid-glass/Miuix blur.

#### Scenario: Expanded artwork
- **WHEN** collapse progress is zero
- **THEN** the solid background SHALL be transparent so the aligned artwork remains visually continuous

#### Scenario: Partial collapse
- **WHEN** collapse progress is between zero and one
- **THEN** the solid background opacity SHALL equal the clamped collapse progress while preserving title and back-control contrast

#### Scenario: Pinned collapsed toolbar
- **WHEN** collapse progress reaches one
- **THEN** the sticky toolbar SHALL show a fully opaque `HausColors.paper` background

#### Scenario: Bottom bar remains unchanged
- **WHEN** artwork chrome is expanded, partially collapsed, or pinned
- **THEN** the bottom/Now Playing bar SHALL retain its pre-existing rendering and backdrop behavior

### Requirement: No-artwork behavior remains Miuix-owned
Album and artist Track List pages without representative artwork SHALL retain the existing Miuix large-title nested-scroll connection, glass chrome, content-padding policy, divider, and title behavior.

#### Scenario: Open a page without artwork
- **WHEN** an album or artist drill-down has no resolved representative artwork bytes
- **THEN** the page SHALL use the existing Miuix scroll behavior and SHALL NOT compose the artwork lazy-item sequence

### Requirement: Existing drill-down functions remain intact
The change SHALL preserve album and artist navigation, safe insets, back interaction, title presentation, artwork loading, track ordering, playback selection, scrollbar behavior, and Now Playing spacing.

#### Scenario: Use an artwork-backed drill-down
- **WHEN** the user navigates, selects a track, uses the back button, uses the scrollbar, or scrolls after full collapse
- **THEN** those functions SHALL behave as before except for the corrected coordinated artwork-collapse motion
