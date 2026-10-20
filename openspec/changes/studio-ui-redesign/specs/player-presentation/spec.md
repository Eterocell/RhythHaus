# Player presentation delta

## Purpose

Define task-focused mini and full playback surfaces with accessible core controls, truthful local lyrics, sleep-timer and queue access, without changing playback selection or recovery semantics.

## ADDED Requirements

### Requirement: Playback-only mini-player
The mini-player SHALL exist only for a valid selected track, including paused/loading/failed tracks. It MUST prioritize artwork, identity and independent play/pause, with compact next action when space permits; desktop MUST expose queue. It MUST NOT display a user-volume control without an existing supported capability. Identity activation/expansion MUST NOT be triggered by transport actions. Search and Settings MUST NOT be mini-player actions.

#### Scenario: Pause without expanding
- **WHEN** the listener activates play/pause in the mini-player
- **THEN** only that playback request MUST execute, while activating track identity MUST open the full player independently

### Requirement: One primary transport group
The full player SHALL present artwork, identity/favorite, progress, previous/play-pause/next, then secondary actions. It MUST have one primary transport group, preserve existing seek/timing/repeat/shuffle semantics and report current states without color alone. Compact layouts MUST prioritize controls over artwork size; wide split and an optional queue column MUST require sufficient real space.

#### Scenario: Short viewport with enlarged text
- **WHEN** the full player is shown at 200% text scale or in a short landscape window
- **THEN** artwork MUST yield space and essential playback, Back and progress actions MUST remain reachable without shrinking touch targets

### Requirement: Conditional local lyrics reader
Lyrics SHALL have a consistent named entry only while the current track has nonempty local lyrics. The reader MUST retain existing sidecar/embedded precedence, plain and timed presentation, follow-position behavior and supported errors. Leaving lyrics MUST return to the player before dismissing it through shared Back arbitration; track changes MUST NOT retain stale lyrics.

#### Scenario: No lyrics on a replacement track
- **WHEN** playback moves from a track with lyrics to a track without lyrics
- **THEN** no stale text or empty lyrics action MUST remain and playback MUST NOT be changed by the reader

### Requirement: Visible timer and recovery states
Timer and queue SHALL have named entries. An active timer MUST expose concise status and existing duration/current/N-completion, fade and cancellation options. Loading MUST NOT be shown as Playing. Playback failure MUST preserve the structured error and existing retry, non-wrapping skip and queue-only removal, without silently deleting library/media or dismissing the error as a fix.

#### Scenario: Recover a failed occurrence
- **WHEN** a selected occurrence fails and the listener chooses an available recovery action
- **THEN** the existing controller operation MUST execute with its original admission semantics and the UI MUST continue reflecting authoritative playback state

### Requirement: Dedicated queue surface
Queue SHALL open from the player as a near-full-height compact layer or a sufficiently wide side panel. It MUST preserve all existing queue segments, occurrence identity, duplicates, reorder/remove/clear and accessible alternatives; current/upcoming emphasis MUST NOT remove other accessible segments. Saving MUST delegate the unchanged current-plus-upcoming snapshot transaction. The UI MUST NOT maintain an independent authoritative queue.

#### Scenario: Repeated track occurrences
- **WHEN** the queue contains repeated track IDs in different occurrences
- **THEN** every occurrence MUST remain separately addressable in the displayed order and mutations MUST target the intended occurrence

#### Scenario: Close queue before player
- **WHEN** Back is requested with a compact queue layer above the player
- **THEN** shared arbitration MUST close the foremost queue/modal target first, retaining the selected player and queue state
