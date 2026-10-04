# Design: incremental scan summaries

## Decision
Reuse the existing App-owned scan coordinator and persisted `ScanSession`. A completed scan compares source-local identity plus size/modification metadata against the source's pre-scan tracks. The bounded `ScanChangeSummary` is stored with the session and rendered by the existing Settings outcome panel. Missing entries are reported only; existing explicit remove-missing remains the sole deletion path.

## Boundaries
- No watcher, startup scan, or permission changes.
- Details are capped at 100 per category and counts remain authoritative beyond the cap.
- Cancelled and failed sessions do not publish an authoritative summary.
- Migration is additive and nullable for existing sessions.

## Review corrections
- Each session owns only its own successful summary. The latest completed summary is selected separately from the latest terminal report, including on restart; a failed/cancelled report must not inherit another scan's counts.
- A changed size or modification time counts as modified only when both observations are known. Recoverably unreadable but observed MediaStore rows are not missing.
- Persisted details must round-trip arbitrary path text, including separator characters. Missing details retain source-local paths rather than ambiguous basenames.
- Legacy version-zero databases are classified using both table presence and the summary column before migration; a pre-summary database must receive migration 6.
