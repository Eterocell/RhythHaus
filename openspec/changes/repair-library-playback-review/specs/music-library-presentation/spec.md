## ADDED Requirements

### Requirement: Browse stored covers by visible row
Search MUST resolve stored artwork for visible results without loading artwork BLOBs in routine list queries. Albums MUST compose cards in lazy rows bounded by the visible viewport and prefetch area.

#### Scenario: Persisted cover appears in Search
- **WHEN** a matching stored track has artwork but its projected list row has no eager bytes
- **THEN** Search loads the local cover by track ID and displays it decoratively; missing or undecodable data displays the neutral fallback

#### Scenario: Large album collection
- **WHEN** the same viewport opens a collection of 100 or 1,000 albums
- **THEN** initial cover requests stay bounded by its displayed rows; scrolling to a distant release loads its cover and resizing keeps albums reachable

### Requirement: Use the same album identity in Library and CarPlay
Album membership MUST be keyed by album title and nonblank album artist, falling back to track artist. Library and CarPlay SHALL resolve albums, details and ordered queues consistently. Existing database records MUST be refreshable by rescan; smart-playlist artist rules SHALL remain unchanged.

#### Scenario: Collisions and compilations
- **WHEN** separate artists release albums with the same title alongside a compilation tagged with a common album artist
- **THEN** both surfaces separate the unrelated releases and keep the compilation together; untagged albums consistently use track artist
