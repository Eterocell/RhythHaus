# Playlist Detail Editing Specification

## Purpose

Define playback-first playlist detail rows, whole-page occurrence-safe editing, accessible mutation controls, and edit-first navigation behavior.

## Requirements

### Requirement: Familiar default playlist rows
Saved-playlist detail SHALL render each row in its default state using the album/artist track-list presentation: artwork, title, artist/album metadata, and duration. A normal row click SHALL play the visible playlist occurrence represented by that row, and default rows MUST NOT expose drag, move, or remove controls.

#### Scenario: Play a selected playlist occurrence
- **WHEN** the user clicks a default playlist row
- **THEN** the system SHALL start playlist playback at that row's `PlaylistEntry.id` occurrence in the current visible order

#### Scenario: Default row has no mutation controls
- **WHEN** a saved-playlist detail page is first shown and edit mode is inactive
- **THEN** each row SHALL present only track-list playback/content affordances and SHALL hide mutation controls

### Requirement: Whole-page long-press edit mode
Long-pressing any saved-playlist row SHALL activate one page-wide edit mode. While active, every editable row SHALL expose drag, move-up, move-down, and remove controls; normal row clicks MUST NOT start playback. Edit mode SHALL use `PlaylistEntry.id` for row keys, targets, mutations, and confirmations so duplicate library tracks remain independently targetable.

#### Scenario: Long-press activates editing for all rows
- **WHEN** the user long-presses any playlist row
- **THEN** the page SHALL enter edit mode and expose the edit controls on every editable row

#### Scenario: Duplicate tracks remain independently editable
- **WHEN** two rows reference the same library track but have different `PlaylistEntry.id` values
- **THEN** moving or removing one row SHALL affect only the occurrence identified by its own `PlaylistEntry.id`

#### Scenario: Edit-mode row click does not play
- **WHEN** the user clicks a row while page-wide edit mode is active
- **THEN** the system SHALL keep edit mode active and SHALL NOT start playback from that click

### Requirement: Accessible reorder and removal alternatives
Edit mode SHALL provide semantic move-up, move-down, and remove actions for every editable row in addition to drag. Move-up and move-down MUST be disabled at list boundaries, and removal MUST require confirmation. The controls MUST identify the affected occurrence/track and MUST NOT rely on color alone.

#### Scenario: Move control respects boundaries
- **WHEN** an editable row is first or last in the ordered playlist
- **THEN** its corresponding boundary move control SHALL be unavailable while the opposite move remains available when applicable

#### Scenario: Reorder without dragging
- **WHEN** an assistive-technology user invokes a labeled move-up or move-down action
- **THEN** the system SHALL reorder that `PlaylistEntry.id` occurrence and expose the updated row as a targetable row

#### Scenario: Remove requires confirmation
- **WHEN** the user invokes remove for an editable row
- **THEN** the system SHALL request confirmation before removing that row occurrence

### Requirement: Edit mode exits before navigation
While playlist edit mode is active, a tap outside the editable playlist list SHALL exit edit mode and consume the tap. Back SHALL exit edit mode before any playlist route navigation or back-gesture completion; navigation MAY occur only after edit mode is already inactive.

#### Scenario: Outside tap exits editing
- **WHEN** the user taps outside the editable playlist list while edit mode is active
- **THEN** the system SHALL exit edit mode without navigating away from the playlist detail page

#### Scenario: Back exits editing first
- **WHEN** the user presses Back while edit mode is active
- **THEN** the system SHALL exit edit mode and remain on the playlist detail page
