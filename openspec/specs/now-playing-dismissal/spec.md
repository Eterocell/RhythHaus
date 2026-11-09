# now-playing-dismissal Specification

## Purpose

Defines a downward-dismiss gesture for the expanded Now Playing Controls surface while preserving playback, nested player panels, protected overlays, and existing platform Back behavior.

## Requirements

### Requirement: Controls surface supports deliberate downward dismissal
The expanded Now Playing Controls surface SHALL support downward dismissal from its non-interactive surface, including artwork and unused space. Buttons, seek and other controls SHALL retain their own input; a short incomplete drag SHALL settle back to the expanded player.

#### Scenario: Drag from artwork or unused Controls space
- **WHEN** the user begins a predominantly downward drag on non-interactive Controls content and crosses the dismissal threshold or performs a qualifying downward fling
- **THEN** the expanded player SHALL dismiss to the existing Now Playing dock without changing the selected track or playback state

#### Scenario: Tap, horizontal gesture, or short downward drag
- **WHEN** the user taps a control, drags predominantly horizontally, or releases a downward drag below the dismissal threshold
- **THEN** the control's existing action or the appropriate scroll behavior SHALL win and an incomplete dismissal SHALL settle back to Controls

### Requirement: Nested player surfaces retain gesture ownership
Queue, Lyrics, and Timer surfaces SHALL keep their existing nested interaction and Back semantics. Their gestures SHALL NOT be interpreted as dismissal of the entire expanded player.

#### Scenario: Dismiss the Queue panel
- **WHEN** the user drags the Queue panel handle or header downward past its panel-dismiss threshold
- **THEN** the Queue panel SHALL close to Controls while the expanded player remains open

#### Scenario: Reorder or scroll Queue content
- **WHEN** the user scrolls, drags, or reorders content within the Queue body
- **THEN** the Queue SHALL retain that interaction and SHALL NOT dismiss the expanded player

#### Scenario: Scroll Lyrics or Timer content
- **WHEN** the user scrolls Lyrics or Timer content vertically
- **THEN** that content SHALL scroll without dismissing the expanded player, and its existing Back or close action SHALL return to Controls

### Requirement: Protected foreground surfaces and existing Back authority take precedence
A higher-priority dialog, editor, or protected overlay SHALL prevent a drag on the underlying player from dismissing that player. Platform Back and explicit close actions SHALL continue through the existing Shared arbitration; this change SHALL NOT add a new platform navigation authority or a new iOS interactive-back gesture.

#### Scenario: Protected overlay covers the player
- **WHEN** a protected confirmation, naming, editing, or other higher-priority overlay is active above the expanded player
- **THEN** the covered player SHALL NOT accept a drag that dismisses it and the foreground overlay SHALL retain Back and input priority

#### Scenario: Platform Back while a nested surface is open
- **WHEN** the user invokes platform Back with a Queue, Lyrics, Timer, or higher-priority layer active
- **THEN** the existing Shared Back arbitration SHALL resolve the foremost eligible layer before the expanded player is dismissed

### Requirement: Dismissal cancellation restores a stable player
A cancelled pointer gesture or interrupted drag SHALL return to the prior stable player surface. Completing or cancelling a dismissal SHALL NOT stop playback, clear or mutate the queue, pop an unrelated route, or leave stale input blocking the revealed surface.

#### Scenario: Pointer cancellation or geometry change during drag
- **WHEN** the pointer is cancelled, the window geometry changes, or a higher-priority overlay appears before dismissal settles
- **THEN** the drag SHALL cancel or settle to the valid foreground state without residual translation, scrim, or input interception

#### Scenario: Player is dismissed successfully
- **WHEN** a Controls dismissal completes
- **THEN** the previous route and its scroll position SHALL be revealed, the existing Now Playing dock SHALL be available after settlement, and playback and queue contents SHALL remain unchanged
