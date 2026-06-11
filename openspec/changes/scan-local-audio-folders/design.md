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
