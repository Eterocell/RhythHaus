# Playback Failure Recovery

## ADDED Requirements

### Requirement: Structured playback failure state

The system SHALL publish a platform-neutral playback failure kind with every terminal playback error.

#### Scenario: Factual native classification
- **WHEN** a platform engine has reliable evidence of a missing local file, lost access, unsupported format, or decoder failure
- **THEN** it publishes the corresponding failure kind
- **AND** it preserves a listener-safe message and optional diagnostic cause

#### Scenario: Opaque native failure
- **WHEN** a platform engine cannot reliably classify its failure
- **THEN** it publishes `Unknown`
- **AND** it SHALL NOT label the failure as missing, access-lost, unsupported, or decoder failure by inference alone

#### Scenario: Structured load failure propagation
- **WHEN** a platform load both reports a structured failure and completes exceptionally
- **THEN** the shared controller retains that structured failure
- **AND** it SHALL NOT replace it with a generic asynchronous error

### Requirement: Retry failed playback

The system SHALL allow a listener to retry the current failed queue occurrence.

#### Scenario: Retry succeeds
- **WHEN** the current occurrence is in error and the listener requests retry
- **THEN** the controller reloads that same occurrence with autoplay
- **AND** a successful load clears the failure

#### Scenario: Retry unavailable
- **WHEN** there is no current error occurrence or playback commands are disabled
- **THEN** retry SHALL not mutate queue, playback engine, or persisted playback state

### Requirement: Skip or remove a failed queue occurrence

The system SHALL provide non-destructive recovery from a failed queue occurrence.

#### Scenario: Skip failed occurrence
- **WHEN** the current occurrence is in error and an effective-order successor exists
- **THEN** skip loads that successor with autoplay
- **AND** it SHALL NOT wrap from the effective-order end

#### Scenario: Remove failed occurrence with successor
- **WHEN** the listener removes the current failed occurrence and an effective-order successor exists
- **THEN** the controller removes exactly that occurrence from the queue
- **AND** it loads the successor with autoplay
- **AND** it checkpoints the changed queue

#### Scenario: Remove final failed occurrence
- **WHEN** the listener removes the current failed occurrence and no effective-order successor exists
- **THEN** the controller removes exactly that occurrence
- **AND** clears the platform engine
- **AND** publishes idle state with no current occurrence and the remaining queue preserved

#### Scenario: Recovery never deletes library content
- **WHEN** the listener skips or removes a failed occurrence
- **THEN** the system SHALL NOT delete a media file, library record, or source

### Requirement: Error-only Now Playing recovery controls

The shared Now Playing UI SHALL expose recovery controls only for an active playback error.

#### Scenario: Active error
- **WHEN** a current queue occurrence has `PlaybackStatus.Error` and a failure
- **THEN** Now Playing renders accessible Retry, Skip, and Remove from queue actions
- **AND** it explains the failure without exposing raw platform exception types

#### Scenario: Non-error playback state
- **WHEN** playback is idle, loading, buffering, playing, paused, or stopped
- **THEN** Now Playing SHALL render no recovery action, pointer target, or accessibility node
