## ADDED Requirements

### Requirement: Library home has no nested top bar
The Library home SHALL render its existing in-content header without adding a scroll-triggered, static, blurred, or collapsing top bar above it.

#### Scenario: Library home is at the top of the list
- **WHEN** the Library home is rendered at its initial scroll position
- **THEN** the existing Library header is the first page-identification content below the system status-bar safe area
- **AND** no separate top bar is rendered

#### Scenario: Library home is scrolled
- **WHEN** the Library home list scrolls beyond its first item
- **THEN** no nested top bar, blur layer, title row, or collapse animation appears
- **AND** the existing Now Playing bar scroll-visibility behavior remains available

### Requirement: Drill-down chrome remains unchanged
Album and artist drill-down screens SHALL retain their existing Miuix scroll chrome independently of the Library home change.

#### Scenario: Album or artist detail is opened
- **WHEN** an album or artist detail route is rendered
- **THEN** its existing `DrillDownMiuixScrollChrome` behavior remains available
- **AND** artwork transitions, title presentation, back navigation, safe-start inset, and Miuix scroll behavior are unchanged

### Requirement: Settings uses compact safe-area-aware spacing
Settings SHALL retain system safe-area padding and SHALL use 16 dp horizontal page padding, 8 dp vertical page padding, 12 dp inter-item spacing, and 8 dp final bottom content padding.

#### Scenario: Settings is rendered on a safe-area device
- **WHEN** the Settings page is laid out
- **THEN** system status bars, display cutouts, navigation bars, and desktop-safe content regions remain avoided
- **AND** the app-owned page padding and spacing use the exact compact values

#### Scenario: Settings controls remain usable
- **WHEN** the compact spacing is applied
- **THEN** the existing top bar, appearance preference, source controls, scan status, buttons, and dialogs retain their behavior
- **AND** existing 44 dp interactive targets and accessibility labels remain unchanged

### Requirement: Scope remains limited to shared layout behavior
The change SHALL remain limited to the shared Library home and Settings layout behavior.

#### Scenario: Scoped diff is reviewed
- **WHEN** implementation is complete
- **THEN** no dependency, database, scanner, playback-engine, navigation-model, or platform-integration change is present
- **AND** no Windows or Linux product support is added
