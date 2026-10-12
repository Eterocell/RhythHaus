# Spec Delta: Gapless Playback

## Purpose

Define local, cross-platform gapless queue advancement so consecutive tracks can transfer without an audible silence gap while retaining RhythHaus’s existing queue authority and failure safety.

## ADDED Requirements

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
When enabled and crossfade is Off, the playback controller SHALL prepare the effective next queue occurrence before natural completion and SHALL promote it at the outgoing end boundary without waiting for a second ordinary load.

#### Scenario: Consecutive queue tracks
- **WHEN** the current occurrence is Playing and has a supported successor
- **THEN** the successor is prepared before the current duration ends
- **AND** natural completion promotes the prepared successor without publishing an idle or stopped gap between them.

#### Scenario: Repeat and shuffle order
- **WHEN** gapless playback advances a queue
- **THEN** the successor follows the same effective repeat and shuffle order as ordinary playback
- **AND** duplicate queue occurrences remain distinct.

#### Scenario: No eligible successor
- **WHEN** the queue has no effective successor or the current/successor duration is unavailable
- **THEN** playback uses ordinary completion behavior without a failed gapless attempt.

### Requirement: Crossfade takes precedence over gapless
When both gapless and crossfade are enabled, the controller SHALL use the existing crossfade transition and SHALL NOT create an independent gapless promotion or duplicate successor load.

#### Scenario: Crossfade enabled
- **WHEN** a crossfade window is eligible
- **THEN** crossfade owns successor preparation and promotion
- **AND** disabling gapless does not interrupt that already-authoritative crossfade.

### Requirement: Gapless failures are generation-safe and queue-only
A failed prepared successor SHALL leave the outgoing occurrence authoritative until ordinary completion or an explicit recovery command settles it. Gapless cleanup SHALL not delete files, source records, favorites, history, metadata overrides, or unrelated queue occurrences.

#### Scenario: Prepared successor fails
- **WHEN** the gapless successor fails during preparation
- **THEN** outgoing playback remains authoritative and ordinary completion is retained until the outgoing occurrence ends
- **WHEN** the outgoing occurrence naturally completes with a recorded successor preparation failure
- **THEN** the failure is attributed to that successor generation
- **AND** stale callbacks cannot replace or stop a newer selection
- **AND** Retry, Skip, and Remove operate on the failed queue occurrence under the existing playback-failure contract
- **WHEN** the gapless successor fails during promotion
- **THEN** the failure is attributed to that successor generation without restoring or replaying the outgoing occurrence.

#### Scenario: Explicit transport supersedes preparation
- **WHEN** the listener selects, seeks, pauses, stops, or skips before gapless promotion
- **THEN** preparation is cancelled or retired according to the command
- **AND** no stale completion or promotion changes the explicit command’s resulting state.

### Requirement: Gapless preserves playback policy and platform contracts
Gapless promotion SHALL preserve the existing effective playback gain, ReplayGain, sleep-timer attenuation, queue state, playback progress ownership, and system-media session identity. Android, iOS, and macOS engines SHALL expose equivalent observable behavior, while platform-specific decoder limitations MAY report an ordinary supported failure.

#### Scenario: Policy composition
- **WHEN** ReplayGain or a playback-only sleep fade is active during gapless promotion
- **THEN** the promoted successor uses the same policy composition without changing system volume or media bytes.

#### Scenario: Actual two-file handoff
- **WHEN** a temporary supported media pair is played with gapless enabled
- **THEN** both files remain byte-identical and the successor begins at its own zero position as the outgoing voice reaches its end boundary.
