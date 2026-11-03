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

Settings SHALL include an Appearance section with one accessible System/Light/Dark segmented control and four selectable palette previews. The selected mode and palette SHALL be visibly and semantically identifiable. Settings SHALL expose an Android Monet switch only on Android API 31 or newer.

#### Scenario: Appearance choices are selected accessibly
- **WHEN** Settings is open
- **THEN** the current mode and curated palette are visually and semantically distinct from unselected choices
- **AND** selecting another mode or palette immediately updates the preview after persistence succeeds

#### Scenario: Monet control is version-gated
- **WHEN** Settings is open
- **THEN** the Monet switch is shown only when the current platform is Android API 31 or newer
- **AND** iOS, macOS/JVM, Android API 29, and Android API 30 expose the curated palette choices without a Monet switch

### Requirement: Persisted curated palette selection

RhythHaus SHALL offer the curated palettes Moss, Bay, Clay, and Iris, each with complete light and dark semantic roles. Moss SHALL be the default. The selected palette SHALL persist independently from theme mode and Android Monet intent in the existing DataStore Preferences store.

#### Scenario: Palette and appearance mode resolve independently
- **WHEN** the user selects Bay and then selects Dark
- **THEN** the Bay palette's dark values are active
- **AND** the persisted palette remains Bay
- **AND** the persisted mode is Dark

#### Scenario: Palette selection survives restart
- **WHEN** the user selects Clay, exits, and restarts RhythHaus
- **THEN** Clay is selected and resolves using the restored appearance mode

#### Scenario: Invalid or missing palette identifier
- **WHEN** the persisted palette identifier is missing or unknown
- **THEN** Moss is selected without crashing or changing the stored theme mode

### Requirement: Opt-in Android dynamic colors

On Android API 31 or newer, RhythHaus SHALL optionally resolve the active light/dark Android dynamic color scheme into the Haus roles specified by the design-token mapping. Monet SHALL default off. Dynamic colors SHALL NOT replace the stored curated-palette identifier; warning and success roles SHALL continue to use fixed semantic values from that curated palette.

#### Scenario: User enables Monet
- **WHEN** Monet is enabled on supported Android
- **THEN** the active Haus scheme uses the platform's current light or dark dynamic colors for mapped roles
- **AND** the selected curated palette identifier and theme mode remain unchanged

#### Scenario: Android appearance changes while Monet is enabled
- **WHEN** Android's active light/dark configuration or dynamic scheme changes
- **THEN** RhythHaus resolves the corresponding current dynamic scheme without requiring an app restart
- **AND** the selected curated palette remains available as fallback

#### Scenario: Dynamic scheme is unavailable
- **WHEN** Monet is enabled on supported Android but the platform provider cannot supply a dynamic scheme
- **THEN** RhythHaus renders the selected curated palette
- **AND** Settings indicates that the curated fallback is active
- **AND** the Monet intent remains persisted until the user changes it

### Requirement: Curated fallback preserves user intent

RhythHaus SHALL resolve a curated palette on platforms without Monet support and whenever Monet is disabled. A temporary unsupported platform or failed Android dynamic-color resolution SHALL NOT overwrite the selected curated palette or silently clear a persisted Monet request.

#### Scenario: Select a curated palette while Monet is active
- **WHEN** the user selects Iris while Monet is enabled
- **THEN** Iris becomes the selected curated palette
- **AND** Monet is disabled

#### Scenario: Turn Monet off
- **WHEN** the user disables Monet
- **THEN** RhythHaus immediately restores the last selected curated palette in the active light/dark mode

#### Scenario: Return to supported Android after fallback
- **WHEN** a persisted Monet request is temporarily unresolved on Android API 29–30 or another unsupported platform and the user returns to supported Android
- **THEN** RhythHaus resolves Monet again using the preserved request and the same curated fallback

### Requirement: Accessible semantic palette contrast

Every curated palette SHALL keep normal text, action content, and semantic status content at a minimum 4.5:1 contrast ratio against their specified surfaces and containers. Focus indicators and essential component boundaries SHALL maintain at least 3:1 contrast against adjacent surfaces. Alpha colors SHALL be evaluated after compositing over the final rendered surface. Android dynamic colors SHALL map only the roles named in the design-token mapping; warning and success roles remain curated.

#### Scenario: Curated contrast values remain within the approved thresholds
- **WHEN** the light and dark values for Moss, Bay, Clay, and Iris are evaluated
- **THEN** every specified text, action, and status pair meets 4.5:1
- **AND** every specified focus and essential-boundary pair meets 3:1

### Requirement: Appearance persistence failure is recoverable

If persisting an appearance choice fails, RhythHaus SHALL keep the last persisted appearance active, preserve unrelated Settings state, and expose a retry action without crashing.

#### Scenario: Appearance write fails
- **WHEN** saving a mode, curated palette, or Monet choice fails
- **THEN** the active appearance returns to the last persisted value
- **AND** Settings communicates the failure and offers retry
- **AND** playback, navigation, and other Settings values remain unchanged
