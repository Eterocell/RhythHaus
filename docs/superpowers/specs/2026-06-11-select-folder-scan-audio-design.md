# Select Folder to Scan Audio Design

Date: 2026-06-11

## Context

RhythHaus currently supports manual local-audio import by selecting one or more audio files. Android uses `ActivityResultContracts.OpenMultipleDocuments()` with `audio/*`, macOS/JVM uses a native AWT `FileDialog` for multi-select audio files, and iOS currently reports that import needs a document-picker bridge. The app already has shared playback state and platform playback engines that consume `AudioSource.FilePath` or `AudioSource.Uri`.

This design replaces the single/multiple-file import flow with a durable local-library scanning subsystem. It is intentionally a new product/architecture change rather than a small picker swap, because recursive folder scanning, persistent library storage, source access, rescan behavior, and iOS constraints are platform-sensitive.

## Goals

- Let users select a folder/source to scan for audio instead of choosing individual audio files.
- Scan selected sources recursively.
- Persist the local library in a shared KMP database, using SQLDelight or an equivalent KMP persistence layer.
- Provide a library manager UI with progress, cancel/retry, rescan, remove-missing, and scan error report behavior.
- Support Android, macOS/JVM, and iOS with platform-appropriate source semantics.
- Keep playback routed through the existing shared playback controller and platform playback engines.

## Non-goals

- Apple Music/media-library access on iOS. This is future scope and should be represented by a separate OpenSpec change later.
- Arbitrary iOS Files app folder access in this first scanner design. iOS uses an app-local music folder for now.
- Windows/Linux support.
- Rewriting playback engines unless scan-produced sources reveal a specific playback gap.
- Replacing the native TagLib metadata wrapper with a custom Kotlin parser.

## Product behavior

RhythHaus should move from “choose audio files” to “add a music folder/source.” The user chooses one or more library sources, the app recursively scans supported audio files, extracts available metadata through the existing TagLib-backed metadata seam where supported, stores discovered tracks in a shared database, and presents them in the shared library UI.

Platform meanings:

- Android: the user chooses a folder/tree through Storage Access Framework. RhythHaus persists tree URI permission and recursively scans document children.
- macOS/JVM: the user chooses a folder through a Finder-style native folder picker. RhythHaus recursively scans filesystem paths.
- iOS: RhythHaus provisions and scans an app-local music folder. Users import/copy files into app storage, then RhythHaus recursively scans that app-local folder. Apple Music/media-library access remains future scope.

## Architecture

Shared code owns durable state, orchestration, persistence contracts, and Compose UI state.

Core shared concepts:

- `LibrarySource`: identifies a scan source, such as Android tree URI, macOS folder path, or iOS app-local folder.
- `LibraryTrack`: persistent row for a discovered playable audio file. It stores source id, stable content/location key, display title, artist, album, duration, file size/modified time when available, and the `AudioSource` needed for playback.
- `ScanSession` and `ScanProgress`: represent idle, scanning, cancelling, completed, cancelled, and failed states plus counts for folders visited, files discovered, tracks imported/updated, skipped files, and latest error or latest scanned item.
- `LibraryRepository`: shared persistence API backed by SQLDelight or an equivalent KMP database. It stores sources, tracks, scan sessions, and scan errors/skipped files.
- `LibraryScanner`: shared orchestration that asks platform scanner implementations to enumerate audio candidates, enriches them with metadata, upserts rows into the repository, records skipped-file errors, and publishes progressive scan state.

Platform code stays behind explicit seams:

- `PlatformFolderPicker`: chooses or provisions a library source.
- `PlatformAudioScanner`: recursively enumerates audio candidates for a source.
- `PlatformSourceAccess`: handles persisted access/security constraints, including Android URI grants, macOS folder path/bookmark strategy, and iOS app-local folder provisioning.

Existing playback engines continue to consume persisted `AudioSource.FilePath` or `AudioSource.Uri` values from selected `LibraryTrack` rows.

## Data flow

1. User chooses “Add music folder/source.”
2. Platform picker returns a `LibrarySource`.
3. Shared scanner creates a scan session and starts recursive enumeration.
4. Platform scanner emits candidate audio files.
5. Shared scanner enriches candidates with metadata through the existing metadata reader where supported.
6. Repository upserts the source, tracks, scan session, and skipped-file errors.
7. UI observes repository state and scan progress, updating progressively.
8. Playback uses the persisted `AudioSource` from the selected track.

## Database model

The database should include, at minimum:

- Sources: id, platform kind, display name, persisted handle/path/URI, created time, last scan time, access status.
- Tracks: id, source id, stable source-local key, playback source type/value, title, artist, album, duration, file size, modified time, last seen scan id, created time, updated time.
- Scan sessions: id, source id, status, started time, completed time, folders visited, files visited, tracks added, tracks updated, files skipped, cancellation flag or terminal reason.
- Scan errors: id, scan id, source-local key or display path, reason, recoverability, timestamp.

Track upserts should be keyed by source plus a stable file identity/path/URI. Rescan should update existing rows rather than duplicating tracks. Remove-missing should delete or mark tracks whose source-local key was not seen in the latest successful scan for that source.

## UI behavior

Empty state:

- Primary action: “Add music folder” on Android/macOS.
- iOS action: “Set up app music folder” or equivalent copy/import guidance.
- Explain that RhythHaus scans local audio recursively and stores the library locally.

Active scan:

- Show scan status, folders/files visited, tracks added/updated, skipped files, and latest scanned path/name when available.
- Provide Cancel.
- Allow playback of already discovered tracks while scan continues once progressive results exist.

Completed scan:

- Show total tracks, last scan time, skipped/error count, and actions for Rescan, Add another source/folder, Remove missing files, and View scan report.

Error handling:

- Unreadable files are skipped and recorded rather than failing the whole scan.
- Lost source access is surfaced with a “Reconnect folder/source” action.
- Metadata failures fall back to filename-derived display data.
- Cancel leaves already imported tracks intact and marks the scan session cancelled.

## Implementation boundaries

This should become a new OpenSpec change, likely `scan-local-audio-folders`, rather than expanding the completed `import-local-audio` slice directly.

Shared-first boundaries:

- Persistent library entities, repository interface, scan state, scan orchestration, supported-audio filtering rules, and shared Compose library manager UI live in `shared/src/commonMain`.
- Common tests cover recursive scan orchestration with fake platform scanners, database upserts, cancellation, skipped-file errors, remove-missing behavior, and UI-facing state formatting.

Platform-specific boundaries:

- Android implements SAF tree picker, persisted URI permission, recursive `DocumentFile`/ContentResolver traversal, and URI playback handles.
- macOS/JVM implements native folder picker and filesystem traversal.
- iOS implements app-local music source provisioning and scanning. Arbitrary Files app folder access and Apple Music/media-library integration remain future OpenSpec changes.

Migration boundaries:

- The current `ImportedAudioFile` path can remain temporarily while the new library manager is built.
- End state should make folder/source scanning the primary path and update UI copy from “Choose audio files” to “Add music folder” / “Manage library.”
- The existing playback controller and platform engines should remain stable unless scanner-produced sources expose a concrete playback issue.

## Verification plan

Common unit tests:

- Recursive scanner orchestration with fake platform scanner.
- Progressive scan state updates.
- Cancellation.
- Metadata fallback.
- Database upsert and no-duplicate behavior.
- Remove-missing behavior.
- Scan error report aggregation.

Platform-focused checks:

- Android compile and focused tests around SAF source representation/traversal helpers where practical.
- macOS/JVM tests for filesystem traversal using temporary folders.
- iOS simulator tests for app-local source provisioning/scanning logic.

Harness verification:

```bash
./init.sh
openspec validate scan-local-audio-folders --strict
```

## Risks and open decisions

- Adding SQLDelight or an equivalent KMP database changes the dependency/tooling surface and must be planned explicitly.
- Android SAF recursive traversal can be slow and URI permission handling must be robust.
- macOS persisted folder access may need a bookmark/security-scoped strategy if sandboxed packaging is introduced later.
- iOS app-local folder UX needs clear copy so users understand this is not Apple Music library access.
- Mobile rich metadata still depends on native TagLib packaging/wiring status per platform; fallback metadata must remain acceptable.
