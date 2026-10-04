# Incremental scan experience design

## Goal
Make every explicit source scan explain what changed without changing the existing source coordinator, track identity, or remove-missing safety.

## Decisions
- A scan remains explicit and uses the existing App-owned coordinator.
- Each terminal successful scan publishes counts for added, modified, unchanged, and currently missing tracks, plus bounded affected-row details.
- `modified` means the same source identity was observed with changed size or modification timestamp; unknown metadata does not imply modified.
- `missing` is observational only. It never deletes rows; Remove missing remains a separate explicit operation against the latest completed scan.
- Failed and cancelled scans do not publish an authoritative change summary.
- The summary is persisted with the scan session so Settings can show it after restart.
- No background watcher, automatic scan, new permission, or platform-specific comparison algorithm is added.

## Ownership
The database owns summary persistence and migration. Library scanning owns classification from the pre-scan snapshot and observed events. Shared owns publication and Settings lifetime. Settings owns localized rendering and the existing remove-missing action.

## Acceptance
A rescan of unchanged media reports unchanged; a changed file reports modified while retaining its track ID; a new file reports added; an absent prior file reports missing but remains playable/visible until explicit removal; cancellation/failure leaves the previous terminal summary unchanged; summaries are bounded and localized.
