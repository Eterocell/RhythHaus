## Context

The approved Superpowers design calls for replacing file selection with recursive folder/source scanning across Android, macOS/JVM, and iOS. The current `import-local-audio` slice is manual file import only; this change introduces a durable local-library subsystem.

## Decisions

### Decision 1: Folder/source scanning is the primary library path

Users add a local music folder/source. RhythHaus recursively scans audio candidates, stores durable track/source/scan state, and presents a managed local library. The old file import path may remain temporarily during migration but is not the final primary UX.

### Decision 2: Shared database and repository own durable library state

Use SQLDelight or an equivalent KMP database layer. Shared code stores sources, tracks, scan sessions, and scan errors. Track upserts are keyed by source plus stable source-local identity to prevent duplicates across rescans.

### Decision 3: Shared scanner orchestrates platform enumeration

Shared code owns scan session lifecycle, progress state, cancellation, metadata enrichment, repository upserts, skipped-file recording, remove-missing, and UI-facing state. Platform code only chooses/provisions sources and enumerates candidates.

### Decision 4: Platform source semantics differ deliberately

- Android uses SAF tree URI folder access and persists URI permission.
- macOS/JVM uses a native folder picker and filesystem paths.
- iOS uses an app-local music folder for now. Apple Music/media-library access is future scope.

### Decision 5: Playback remains through existing AudioSource values

Scanner-produced tracks must carry the `AudioSource` value needed by current platform playback engines. Playback engine rewrites are out of scope unless a scanner source cannot be played through the existing seam.

## Risks

- KMP database integration changes build/dependency surface.
- Android SAF recursive traversal can be slow and permission-sensitive.
- macOS persisted access may need a bookmark/security-scoped strategy for future sandboxing.
- iOS app-local folder UX must be clear to avoid implying Apple Music library access.
- Mobile rich metadata remains limited by native TagLib packaging/wiring status.

## Verification

- `./init.sh`
- `openspec validate scan-local-audio-folders --strict`
- Focused common tests for scan orchestration, persistence semantics, cancellation, remove-missing, metadata fallback, and error aggregation.
- Platform-focused tests for macOS temp-folder traversal, Android SAF helper behavior where practical, and iOS app-local source/scanner logic.

## Ora-10/Ora-11 Amendments

The following requirements are part of this change and remain implementation work. They are not evidence that the current worktree implementation is complete.

### Atomic remove-missing contract

`removeMissingTracks(sourceId: String, requestedScanId: String): RemoveMissingTracksResult` is a repository operation, not a UI-side filter followed by independent deletes. It SHALL succeed only when `requestedScanId` identifies the same source's authoritative latest valid `Completed` scan session. A session is valid for this purpose only when it is terminal `Completed`, belongs to `sourceId`, and is the latest such session under the deterministic ordering `(completedAtEpochMillis DESC, startedAtEpochMillis DESC, id DESC)`; cancelled, failed, active, and cancelling sessions are rejected. The repository SHALL validate this precondition and delete the missing tracks in one transaction. A rejected request SHALL leave tracks unchanged.

The API module SHALL define `RemoveMissingTracksResult` and `RemoveMissingTracksRejectionReason`, with `Removed(count)` and `Rejected(reason)` branches and no ambiguous integer sentinel. Any returned track/result collections use deterministic ordering by `(completionTime, startTime, id)` where session ordering is involved. This is query/transaction behavior only and requires no schema migration.

### App-owned operation coordination

One App-owned coordinator SHALL own admission and lifecycle for `scan`, `remove-missing`, `remove-source`, and `clear`. It SHALL reject repeated clicks while an operation is admitted, await scan cancellation and terminal session persistence before any mutation repository write, and serialize mutations. Source removal and clear are serialized coordinator operations; they are not required to be new atomic repository APIs. Every admitted operation receives an operation token. Scan state/progress publications and combined library-plus-playlist mutation publications SHALL be accepted only when their token is current; stale operations cannot overwrite newer state.

The coordinator interface SHALL make these boundaries testable without Compose, including operation admission/rejection, cancellation awaiting, repository mutation sequencing, and token-guarded publication. UI callbacks remain thin delegates to this coordinator.

### Restart restoration

The repository SHALL expose a global `latestTerminalScanSession(): ScanSession?` query across `Completed`, `Cancelled`, and `Failed` sessions, ordered by `COALESCE(completedAtEpochMillis, startedAtEpochMillis) DESC`, then `startedAtEpochMillis DESC`, then `id DESC`, and SHALL load its errors. App startup SHALL restore that single latest terminal session and its errors only when the session's source still exists. Active and `Cancelling` sessions are ignored by the query for restoration. Missing sources and sessions without a terminal record restore no scan result. This global result drives the single scan outcome panel.

### Production-boundary acceptance

RED/GREEN coverage SHALL exercise the production repository and SQLDelight path, not only in-memory fakes: rejection matrix, stale scan rejection, deterministic ordering, transaction atomicity, coordinator races and stale publications, repeated clicks, restart restoration, compact and wide production UI wiring, picker cancellation/recovery, playback reconciliation, and playlist refresh. The current intended Library manager UI states and actions remain required: empty/add-source, scanning/cancel, completed/rescan/add-source/remove-missing/report, cancelled/retry, failed/retry/report, and lost-access/recovery.
