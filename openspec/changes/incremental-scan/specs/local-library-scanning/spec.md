# Spec Delta

## ADDED Requirements

### Requirement: Persisted scan change summary
A completed explicit scan MUST persist counts for added, modified, unchanged, and missing tracks and MUST retain at most 100 affected-row details per category. A cancelled or failed scan MUST NOT replace the latest authoritative summary.

#### Scenario: Unchanged rescan
- **WHEN** a completed scan observes the same source-local identities with unchanged known size and modification metadata
- **THEN** the summary reports those tracks as unchanged and preserves their track IDs.

#### Scenario: Added and modified media
- **WHEN** a completed scan observes a new source-local identity or an existing identity with changed known size or modification time
- **THEN** it reports added or modified respectively and keeps the existing ID for modified media.

#### Scenario: Missing media is non-destructive
- **WHEN** a completed scan does not observe a previously persisted source-local identity
- **THEN** it reports missing without deleting the track, favorite, history, metadata override, or source.

### Requirement: Localized scan summary presentation
Settings MUST show the latest completed summary with localized labels, bounded affected paths, and an explicit distinction between missing-file reporting and the existing Remove missing action. Summary controls MUST expose readable semantics and MUST remain reachable in compact layouts.

#### Scenario: Failed or cancelled scan
- **WHEN** a scan fails or is cancelled
- **THEN** the prior completed summary remains visible and the failed/cancelled result does not claim authoritative change counts.
