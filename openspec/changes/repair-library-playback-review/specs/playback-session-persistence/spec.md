## ADDED Requirements

### Requirement: Restore the complete accepted queue from the library database
RhythHaus MUST store the ordered occurrence and track IDs, current occurrence, position and repeat/shuffle modes in its existing SQLDelight database. The legacy preference codec limits SHALL apply to legacy import only. A committed session header MUST distinguish a successfully imported empty queue from an import that has never completed.

#### Scenario: Large queue with repeated tracks survives restart
- **WHEN** a 10,001-occurrence queue containing repeated tracks is persisted, the process restarts and all tracks are available
- **THEN** every occurrence is restored in order, the selected occurrence and position are preserved, and playback is paused
- **AND** a later smaller queue can also be persisted and restored

#### Scenario: Legacy import is interrupted
- **WHEN** a legacy session exists and its import fails before database commit
- **THEN** no partial session or import marker becomes durable and a subsequent launch can import it once
- **AND** a later cold start never replaces a committed SQL session with stale preferences

### Requirement: Position updates do not rewrite the queue
Progress checkpoints MUST carry the current occurrence and revision without materializing queue entries. The coordinator MUST serialize structural writes and matching position updates across restore, reconcile and flush barriers.

#### Scenario: Long playback followed by queue replacement
- **WHEN** one minute of position updates arrives for an unchanged queue
- **THEN** no queue occurrence rows are rewritten
- **AND** a delayed update cannot overwrite progress for a newer occurrence or queue revision
