## ADDED Requirements

### Requirement: Persisted theme mode selection

RhythHaus SHALL provide a Settings appearance control with exactly three theme mode choices: System, Light, and Dark. The selected mode SHALL persist across app restarts on Android, iOS, and macOS/desktop JVM.

#### Scenario: Default theme mode is System
- **WHEN** the app has no persisted theme preference
- **THEN** the selected theme mode is System

#### Scenario: User selects Light
- **WHEN** the user selects Light in Settings
- **THEN** the app uses the light Haus palette
- **AND** the persisted theme mode is Light
- **AND** restarting the app restores Light as the selected mode

#### Scenario: User selects Dark
- **WHEN** the user selects Dark in Settings
- **THEN** the app uses the dark Haus palette
- **AND** the persisted theme mode is Dark
- **AND** restarting the app restores Dark as the selected mode

#### Scenario: User selects System
- **WHEN** the user selects System in Settings
- **THEN** the app resolves the active palette from the platform/system dark-mode state
- **AND** the persisted theme mode is System

### Requirement: DataStore-backed app preference storage

RhythHaus SHALL store the theme preference using AndroidX DataStore Preferences rather than SQLDelight or the music-library database.

#### Scenario: Invalid persisted value
- **WHEN** the persisted theme value is missing or not one of `system`, `light`, or `dark`
- **THEN** the app falls back to System without crashing

### Requirement: Light and dark shared Compose palettes

The shared Compose UI SHALL render visible application surfaces, text, controls, dialogs, overlays, and cards using the active Haus palette instead of a hardcoded light-only palette.

#### Scenario: Light theme readability
- **WHEN** the active resolved theme is Light
- **THEN** the app keeps the existing warm light RhythHaus visual identity
- **AND** primary text, muted text, card backgrounds, dialog backgrounds, and accent controls remain readable

#### Scenario: Dark theme readability
- **WHEN** the active resolved theme is Dark
- **THEN** the app uses a dark background and dark surfaces
- **AND** primary text, muted text, card backgrounds, dialog backgrounds, and accent controls remain readable

### Requirement: Settings appearance selector

Settings SHALL include an Appearance section above or near the existing Manage Music section. The selector SHALL show System, Light, and Dark options and clearly indicate the currently selected option.

#### Scenario: Selected option indication
- **WHEN** Settings is open
- **THEN** the current selected theme mode is visually distinct from the unselected modes
- **AND** selecting another mode immediately updates the active app theme
