## ADDED Requirements

### Requirement: Shared solid dialog shell
The system SHALL provide one common-main dialog shell used by every existing Clear Library, source-removal, playlist, playlist-picker, and queue-confirmation modal. The shell SHALL render an opaque `HausColors.current.panel` panel and SHALL NOT use a liquid-glass or transparent panel treatment.

#### Scenario: Existing dialog opens
- **WHEN** a user opens any existing application modal
- **THEN** its content SHALL be rendered inside the shared solid dialog shell

#### Scenario: Opaque dialog panel
- **WHEN** a dialog is visible over a backdrop with visual content
- **THEN** the dialog panel SHALL use the active solid panel color without backdrop translucency

### Requirement: Theme-aware dialog dim
The shared dialog shell SHALL render a restrained ink dim in light mode and a visibly light dim derived from the active palette in dark mode.

#### Scenario: Dark theme dialog
- **WHEN** the active Haus palette is dark and a dialog opens
- **THEN** the scrim SHALL be light-colored rather than ink-black

#### Scenario: Light theme dialog
- **WHEN** the active Haus palette is light and a dialog opens
- **THEN** the scrim SHALL retain a restrained ink dim treatment

### Requirement: Accessible bounded dialog interaction
The shared dialog shell SHALL expose dialog semantics with a pane title and dismiss action, dismiss when its scrim is tapped, preserve panel interaction without scrim dismissal, and bound long content in a scrollable panel.

#### Scenario: Accessibility dismissal
- **WHEN** assistive technology invokes the dialog dismiss action
- **THEN** the dialog SHALL call its existing dismiss callback exactly once

#### Scenario: Long localized dialog content
- **WHEN** a dialog body exceeds the compact window's available height
- **THEN** the panel SHALL remain within its maximum height and provide scrolling for the body content

### Requirement: Modal behavior preservation
The migration to the shared dialog shell SHALL preserve each dialog's existing localized copy, callback wiring, validation/error display, selection state, and destructive-action semantics.

#### Scenario: Playlist creation validation failure
- **WHEN** a user confirms a blank playlist name
- **THEN** the existing validation message and entered draft state SHALL remain visible in the shared dialog

#### Scenario: Clear Library confirmation
- **WHEN** a user confirms Clear Library
- **THEN** the existing clear-library callback SHALL execute and the dialog SHALL close according to its current behavior
