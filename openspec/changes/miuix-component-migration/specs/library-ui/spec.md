## ADDED Requirements

### Requirement: Miuix-first standard shared UI

RhythHaus shared Compose UI SHALL prefer Miuix components for standard controls, rows, settings, cards, dialogs, popups, and buttons when a Miuix component preserves the existing behavior and semantics.

#### Scenario: Suitable standard component exists
- **WHEN** a shared UI element is a standard settings row, dropdown, button, card, dialog, popup, or clickable row
- **AND** a Miuix component can preserve the element's current behavior, labels, enabled/disabled state, accessibility semantics, and layout intent
- **THEN** the UI element uses the Miuix component instead of a hand-rolled bare Compose implementation

#### Scenario: Product-specific custom UI does not have a clean Miuix fit
- **WHEN** a UI element implements music-specific artwork rendering, equalizer visuals, scrubber gestures, bottom-sheet gestures, edge-swipe gestures, adaptive shell behavior, or glass/backdrop wrappers
- **THEN** it may remain custom
- **AND** the implementation records why a Miuix component was not a suitable fit if the element was explicitly evaluated during the migration

### Requirement: Settings appearance selection uses Miuix

The Settings appearance selection SHALL use a Miuix dropdown/preference-style component instead of the current custom expanding dropdown when the Miuix component is available in commonMain.

#### Scenario: User selects a theme mode
- **WHEN** the user opens Settings and changes appearance mode to System, Light, or Dark
- **THEN** the selection is routed through the existing `onThemeModeSelected` behavior
- **AND** the displayed selected mode, label, and description match the existing localized resources
- **AND** the selection UI closes or commits selection according to the Miuix component's normal interaction model

#### Scenario: Miuix dropdown requires popup host support
- **WHEN** the chosen Miuix dropdown/preference component requires a popup host or scaffold
- **THEN** Settings provides the required Miuix host structure
- **AND** route-level Settings screen dismissal and layout behavior are preserved

### Requirement: Miuix dependencies stay compatible

Any newly added Miuix module SHALL use the existing project Miuix version reference and SHALL NOT introduce incompatible older Miuix artifacts into Android builds.

#### Scenario: Adding a Miuix module
- **WHEN** a Miuix module such as `miuix-preference` or `miuix-icons` is added
- **THEN** it uses `version.ref = "miuix"` from `gradle/libs.versions.toml`
- **AND** Android debug assembly is run to check for duplicate classes or manifest incompatibilities

#### Scenario: Adaptive dependency remains excluded
- **WHEN** this migration adds or evaluates Miuix component dependencies
- **THEN** it does not add `top.yukonga.miuix.kmp:miuix-navigation3-adaptive`

### Requirement: Existing UI behavior is preserved

Miuix component migration SHALL preserve existing Settings, Search, Library, Clear Library, playback, route animation, bottom bar, and glass/status-bar behavior.

#### Scenario: Search UI is migrated selectively
- **WHEN** Search UI pieces are evaluated for Miuix migration
- **THEN** query focus, placeholder behavior, clear action, filtering, result selection, now-playing highlighting, and equalizer display remain unchanged

#### Scenario: Library rows are migrated selectively
- **WHEN** Track or Artist rows are evaluated for Miuix migration
- **THEN** artwork/artist marks, selected state, content descriptions, click behavior, duration/metadata display, and spacing remain unchanged

#### Scenario: Clear Library dialog is migrated selectively
- **WHEN** Clear Library dialog UI is evaluated for Miuix migration
- **THEN** the dialog still participates in the existing in-window route animation
- **AND** cancel and clear actions preserve their current behavior
