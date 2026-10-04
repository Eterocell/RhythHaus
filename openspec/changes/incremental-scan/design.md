# Design: incremental scan summaries

## Decision
Reuse the existing App-owned scan coordinator and persisted `ScanSession`. A completed scan compares source-local identity plus size/modification metadata against the source's pre-scan tracks. The bounded `ScanChangeSummary` is stored with the session and rendered by the existing Settings outcome panel. Missing entries are reported only; existing explicit remove-missing remains the sole deletion path.

## Boundaries
- No watcher, startup scan, or permission changes.
- Details are capped at 100 per category and counts remain authoritative beyond the cap.
- Cancelled and failed sessions do not publish an authoritative summary.
- Migration is additive and nullable for existing sessions.
