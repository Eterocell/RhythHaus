# Gapless Playback Specification

## Purpose

Define local, cross-platform gapless queue advancement so consecutive tracks can transfer without an audible silence gap while retaining RhythHaus's existing queue authority and failure safety.

## Requirements

### Requirement: Gapless playback can be enabled and persisted
The listener SHALL be able to choose gapless playback Off or On. The choice SHALL persist across process restart and SHALL be restored before playback commands or session restoration can begin.

#### Scenario: Default and restart
- **WHEN** no gapless preference exists
- **THEN** gapless playback is Off
- **WHEN** the listener enables gapless and restarts the app
- **THEN** the restored controller uses gapless policy before restoring the playback session.

#### Scenario: Policy changes during playback
- **WHEN** gapless is disabled while a successor is being prepared
- **THEN** stale preparation is cancelled without changing the current occurrence or playback position.

### Requirement: Enabled gapless playback prepares and promotes the next occurrence
When enabled and crossfade is Off, the controller SHALL prepare a supported effective successor within its bounded lead window and promote it at the outgoing end boundary without waiting for a second ordinary load.

#### Scenario: Consecutive queue tracks
- **WHEN** the current occurrence is Playing and has a supported successor
- **THEN** the successor is prepared before the current duration ends
- **AND** natural completion promotes it without publishing an idle or stopped gap between them.

#### Scenario: Repeat, shuffle, and duplicate occurrences
- **WHEN** gapless playback advances a queue
- **THEN** the successor follows effective repeat and shuffle order
- **AND** duplicate media tracks remain distinct queue occurrences.

#### Scenario: No eligible successor
- **WHEN** the queue has no effective successor or either duration is unavailable
- **THEN** ordinary completion behavior remains unchanged.

### Requirement: Crossfade takes precedence over gapless
When both gapless and crossfade are enabled, crossfade owns eligible successor preparation and promotion. Gapless SHALL NOT create a duplicate preparation or independent promotion.

#### Scenario: Crossfade enabled
- **WHEN** a crossfade transition is eligible
- **THEN** it owns successor preparation and promotion
- **AND** disabling gapless does not interrupt the already-authoritative crossfade.

### Requirement: Gapless failures are generation-safe and queue-only
Preparation failure SHALL preserve outgoing playback until its natural completion; failure after successor start SHALL be attributed to the successor occurrence. Cleanup SHALL not mutate media, library records, or unrelated queue occurrences.

#### Scenario: Preparation fails
- **WHEN** successor preparation fails
- **THEN** outgoing playback continues without repeated preparation attempts during that run
- **WHEN** outgoing playback naturally completes
- **THEN** the failure is attributed to the successor generation and existing Retry, Skip, and Remove recovery applies.

#### Scenario: Explicit transport supersedes preparation
- **WHEN** the listener selects, seeks, pauses, stops, or skips before promotion
- **THEN** preparation is cancelled or retired
- **AND** no stale callback changes the explicit command's resulting state.

### Requirement: Gapless preserves playback and platform policy
Gapless SHALL preserve effective playback gain, ReplayGain, sleep-timer attenuation and completion counts, queue state, progress ownership, and system-media session identity across Android, iOS, and macOS.

#### Scenario: Policy composition
- **WHEN** ReplayGain or a playback-only sleep fade is active during promotion
- **THEN** the promoted successor uses the same gain composition without changing system volume or media bytes.

#### Scenario: Track-count completion
- **WHEN** an outgoing occurrence naturally completes during gapless handoff
- **THEN** it is charged once to an active TrackCount timer before the successor is admitted
- **AND** a retiring duplicate completion does not decrement it again.

#### Scenario: Temporary media handoff
- **WHEN** a supported temporary media pair is played with gapless enabled
- **THEN** the successor starts at its own zero position at the outgoing end boundary
- **AND** both media files remain byte-identical.
