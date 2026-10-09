## ADDED Requirements

### Requirement: Sleep stops take precedence over overlapping transitions
An active sleep timer SHALL suppress new crossfade transitions. Arming a timer during an overlap SHALL retire the outgoing voice and retain the authoritative incoming occurrence before admitting future timer completion events. Retiring outgoing completions SHALL NOT count toward a newly armed timer or advance the queue.

#### Scenario: Timer already active
- **WHEN** the current occurrence nears an otherwise eligible crossfade window with a sleep timer armed
- **THEN** it uses existing ordinary completion and timer stop precedence rather than starting overlap

#### Scenario: Timer armed during overlap
- **WHEN** a listener arms a track-count timer while two voices overlap
- **THEN** outgoing playback retires and only subsequent natural completion of the authoritative current occurrence counts toward that timer
