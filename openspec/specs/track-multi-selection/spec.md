# Track Multi-Selection

## Purpose

Define page-scoped, accessible track selection and its contextual playlist action on supported library surfaces.

## Requirements

### Requirement: Eligible page-scoped track selection
The system SHALL support page-scoped track selection on Library home Songs, album detail, artist detail, and Search. A long press MUST start selection with that track selected and MUST NOT start playback. Saved-playlist detail, Queue, and Now Playing MUST NOT gain this selection mode.

#### Scenario: Start selection without playback
- **WHEN** the user long-presses a track on an eligible page outside selection mode
- **THEN** the system SHALL enter selection for that page with the track selected and MUST NOT dispatch the normal playback action

#### Scenario: Reject unsupported selection surfaces
- **WHEN** the user views saved-playlist detail, Queue, or Now Playing
- **THEN** the system MUST preserve the existing interactions without exposing page-scoped selection

### Requirement: Checkbox and row activation behavior
While selection is active, the system SHALL show a checkbox before every visible track artwork on the owning page. Activating either the row or checkbox MUST toggle that track exactly once without playback. Now-playing state and selection state MUST remain independently visible and accessible.

#### Scenario: Toggle from the row
- **WHEN** the user activates a track row during selection
- **THEN** the system SHALL toggle only that track's selected state and MUST NOT start playback

#### Scenario: Select the current track
- **WHEN** the currently playing track is selected
- **THEN** the system SHALL expose both now-playing and selected states without conflating their visuals or semantics

### Requirement: Selection lifetime and visible ordering
Selection MUST clear on explicit cancellation, final deselection, applicable Back action, route change, leaving the owning page, or changing Library home away from Songs. Search query changes MUST remove selected IDs no longer visible. Ordered submission MUST follow the current visible list and MUST NOT depend on set iteration.

#### Scenario: Reconcile a Search query
- **WHEN** a Search query change removes selected tracks from the visible results
- **THEN** the system MUST drop those hidden IDs and exit selection if none remain

#### Scenario: Submit in visible order
- **WHEN** selected track IDs are confirmed from an eligible page
- **THEN** the system MUST submit each selected ID once in that page's current visible order

### Requirement: Contextual selection bar
While selection is active, the system SHALL replace the Now Playing bar with one contextual bar in the same safe-area and responsive-width slot. The bar MUST expose selected count, Cancel, and Add to Playlist; the two bottom bars MUST NOT be visible or interactive simultaneously.

#### Scenario: Replace Now Playing during selection
- **WHEN** selection starts while Now Playing would otherwise be visible
- **THEN** the system SHALL render only the contextual selection bar and preserve sufficient list-content clearance

#### Scenario: Cancel from the contextual bar
- **WHEN** the user activates Cancel
- **THEN** the system SHALL clear selection and restore the route-appropriate Now Playing behavior

### Requirement: Accessible non-gesture selection
Each eligible row MUST expose an accessible action to enter selection without requiring long press. During selection, checkbox state and toggle actions, selected count, Cancel, and Add to Playlist MUST have localized semantics and MUST NOT rely on color alone.

#### Scenario: Enter selection with assistive technology
- **WHEN** an assistive-technology user invokes the row's selection action
- **THEN** the system SHALL enter the same page-scoped selection state as a long press
