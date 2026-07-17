## ADDED Requirements

### Requirement: Queue tab with pinned current occurrence
The system SHALL render a `Queue` tab in the playlist hub. When a queue is active, the tab MUST render the current occurrence first as visually distinct and pinned, followed by upcoming occurrences. The current occurrence MUST NOT expose remove or drag-reorder controls. When no queue is active, the tab SHALL render a distinct empty state.

#### Scenario: Display an active queue
- **WHEN** playback has a current occurrence and upcoming occurrences
- **THEN** the system SHALL show the current occurrence first and only show edit controls for upcoming occurrences

#### Scenario: Display an empty queue
- **WHEN** playback has no active queue
- **THEN** the system MUST show the Queue tab's distinct empty state

### Requirement: Serialized upcoming occurrence mutations
The `PlaybackController` SHALL own serialized commands to reorder an upcoming occurrence, remove an upcoming occurrence, and clear all upcoming occurrences. A successful command MUST validate and mutate the latest controller state, publish one complete state update, and immediately checkpoint the playback session. The system MUST preserve occurrence identity and duplicate occurrences across queue edits and session serialization.

#### Scenario: Reorder an upcoming duplicate occurrence
- **WHEN** the user moves one of two upcoming duplicate-track occurrences
- **THEN** the system SHALL move only the targeted occurrence and retain both occurrences in the serialized session

#### Scenario: Clear upcoming occurrences
- **WHEN** the user confirms `Clear upcoming`
- **THEN** the system MUST remove all upcoming occurrences while leaving the current occurrence loaded and playable

### Requirement: Stale and forbidden queue commands
The controller SHALL validate command IDs and target positions inside its serialization boundary. Attempts to mutate the current occurrence, stale occurrence IDs, and invalid reorder positions MUST be no-ops that return a recoverable result. The Queue tab MUST refresh from controller state and explain that the queue changed when a command is rejected due to concurrent state change.

#### Scenario: Attempt to remove the current occurrence
- **WHEN** a remove command targets the pinned current occurrence
- **THEN** the controller MUST leave the queue unchanged and return a recoverable rejection

#### Scenario: Submit a stale reorder command
- **WHEN** an upcoming reorder command references an occurrence no longer present in the latest controller state
- **THEN** the controller MUST perform no mutation and the UI SHALL refresh from controller state with a recoverable message

### Requirement: Queue edit transport preservation
Successful upcoming queue edits SHALL preserve the current track loading, current playback position, play or pause status, repeat mode, shuffle mode, engine generation, and stale-callback protections. Queue edits MUST NOT replace or restart the current occurrence.

#### Scenario: Edit the queue during paused playback
- **WHEN** the user removes an upcoming occurrence while playback is paused at a nonzero position
- **THEN** the system MUST retain the paused status, current occurrence, and playback position

#### Scenario: Edit the queue during shuffled playback
- **WHEN** the user reorders an upcoming occurrence while shuffle and repeat modes are active
- **THEN** the system SHALL preserve both modes and the existing engine generation while applying the valid queue edit

### Requirement: Queue editing accessibility
The Queue tab SHALL provide semantic labels containing the affected track name for each editable occurrence. Reordering upcoming occurrences MUST offer labeled move-up and move-down controls in addition to drag interaction, and `Clear upcoming` MUST require explicit confirmation.

#### Scenario: Reorder an upcoming occurrence with assistive technology
- **WHEN** a user invokes the labeled move-up control for an upcoming occurrence
- **THEN** the system MUST reorder that occurrence without exposing any mutation control for the current occurrence
