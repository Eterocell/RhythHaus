# UI design system delta

## Purpose

Define a coherent, accessible visual language across every RhythHaus music surface, including light and dark appearance, content density, input feedback and destructive actions.

## ADDED Requirements

### Requirement: Coherent active appearance
The application SHALL apply the approved Studio semantic colors, typography, spacing and shapes consistently across browsing, playback, playlists, settings, dialogs, About and onboarding. System, Light and Dark preferences MUST retain their existing persistence and resolution behavior. Content backgrounds MUST use solid surfaces rather than depend on decorative glass.

#### Scenario: Switch active appearance
- **WHEN** the listener changes appearance while a screen or dialog is open
- **THEN** its content, surrounding chrome, fields, actions and exterior MUST resolve from the same active appearance without a legacy color island

### Requirement: Legible content density
The application SHALL use system fonts, navigation labels of at least 12sp, and scalable line heights. Song rows MUST use continuous list treatment rather than individual heavy cards, with minimum 64dp row/48dp artwork on compact layouts and 56dp row/40dp artwork on desktop. Row height MUST grow when text needs more space.

#### Scenario: Enlarge text and retain actions
- **WHEN** EN or ZH content is displayed at 200% font scale with long titles or absent metadata
- **THEN** titles and essential actions MUST remain readable and reachable without reducing type to force a fixed row or navigation width

### Requirement: Semantic action and contrast
Normal text SHALL meet 4.5:1 contrast and large text, essential icons, focus indicators and interaction boundaries SHALL meet 3:1 against adjacent surfaces. Destructive actions MUST use distinct danger styling and explicit language; selection, favorite and playing states MUST have non-color indications. Mobile actions MUST meet 48dp targets, and iOS targets MUST NOT fall below 44pt.

#### Scenario: Confirm destructive maintenance
- **WHEN** the listener reviews source removal or library clearing in either appearance
- **THEN** the impact and destructive action MUST be explicit, visually separate from a primary music action, and accessible without relying on red alone

### Requirement: Truthful states and local resources
The application SHALL use feature-owned EN/ZH labels, existing real metadata and neutral artwork fallbacks. It MUST NOT show prototype illustrations, counts, lyrics, output devices or shortcut hints as working production capabilities. Busy, disabled and failed operations MUST expose their true current action availability.

#### Scenario: Artwork or operation unavailable
- **WHEN** artwork is absent or an operation is unavailable, saving or failed
- **THEN** the surface MUST retain a readable identity and accurate state without fabricated content, fake progress or an active-looking unavailable action

### Requirement: Accessible interaction layers
Keyboard focus and reading order SHALL follow task order. Inactive, obscured and exiting layers MUST remove pointer, gesture, click and accessibility actions, not merely reduce alpha. Closing a temporary layer MUST restore focus to its surviving invoking control. Large motion MUST be suppressed when reduced motion is enabled, without altering Back settlement.

#### Scenario: Close a temporary layer
- **WHEN** a menu, lyrics reader or confirmation is closed or replaced
- **THEN** only the foremost valid layer MUST accept input, stale actions MUST NOT execute, and focus MUST return to the valid originating control
