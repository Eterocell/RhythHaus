## ADDED Requirements

### Requirement: Playlist hub spacing parity
The playlist hub SHALL use the same 20dp horizontal content inset and system-top-only content padding policy as Library home, while preserving its own navigation control and Now Playing bottom clearance.

#### Scenario: Playlist hub at top of a screen
- **WHEN** a user opens the playlist hub
- **THEN** its top and horizontal content alignment SHALL match Library home without an additional safe-content inset

#### Scenario: Now Playing bar is visible
- **WHEN** a user scrolls playlist hub content with the Now Playing bar visible
- **THEN** the final content SHALL retain the existing bottom clearance above the bar

### Requirement: Playlist tab contrast and text fit
The Saved and Queue controls SHALL explicitly set contrasting active and inactive foreground/background colors from the active Haus palette. Their labels and compact playlist actions SHALL use text metrics that do not crop descenders or overlap at compact widths in either theme.

#### Scenario: Light theme tabs
- **WHEN** the active Haus palette is light
- **THEN** Saved and Queue labels SHALL remain visually distinct from their button backgrounds

#### Scenario: Dark theme tabs
- **WHEN** the active Haus palette is dark
- **THEN** Saved and Queue labels SHALL remain visually distinct from their button backgrounds

#### Scenario: Compact text metrics
- **WHEN** a playlist tab or compact playlist action renders Latin descenders or localized CJK text in a compact layout
- **THEN** the text SHALL remain fully visible within its button bounds
