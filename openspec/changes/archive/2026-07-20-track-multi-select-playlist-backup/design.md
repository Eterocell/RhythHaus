## Context

Eligible track lists currently expose a right-side single-track Add to Playlist action. The playlist repository already accepts an ordered list of track IDs, but picker state and row UI are modeled around one track. There is no shared page-scoped selection state or contextual bottom action bar.

Playlist data and local-library tracks share SQLDelight storage, but Android, iOS, and JVM use different drivers and lifecycle owners. Copying an active SQLite main file is unsafe when committed WAL state or active connections exist. Cross-device playlists also cannot rely on device-local IDs or file paths.

## Goals / Non-Goals

**Goals:**

- Provide one shared, route-scoped selection state for Library Songs, album detail, artist detail, and Search.
- Keep playback and selection state independent and render exactly one active bottom bar.
- Reuse the existing picker and ordered repository mutations for multi-track add.
- Export and import all saved playlists through a strict, portable logical format.
- Match only unique destination tracks and import all eligible playlists atomically.
- Keep platform code limited to system document panels and bounded byte I/O.

**Non-Goals:**

- Selection on saved-playlist detail, Queue, or Now Playing.
- Additional selection actions, cross-route selection, cloud sync, or media transfer.
- Raw database snapshot, replacement, reopening, or migration.
- Guessing ambiguous matches or retaining unavailable playlist entries.
- Windows or Linux support.

## Decisions

### Page-scoped selection owner

`LibraryAppShell` owns a pure selection state keyed by the current eligible page. Long press starts selection; row activation toggles while active; zero selections, cancellation, Back, browse-mode changes, and route changes clear it. Search query changes reconcile against visible result IDs so hidden selections cannot be submitted.

This centralized owner is preferred over independent screen-local reducers because it gives navigation, Back handling, picker completion, and the bottom-bar slot one source of truth. A cross-route selection basket was rejected because its lifetime would be surprising and would complicate visible ordering.

### Separate row states and one bottom slot

Track rows receive separate now-playing, selection-mode, and selected inputs. A real checkbox appears before artwork only in selection mode. Long press must not dispatch playback, and accessibility exposes a non-gesture selection action.

The contextual selection bar uses the same measured safe-area slot as Now Playing. Selection has precedence, so both bars can never remain visible or interactive. The bar shows selected count, Cancel, and Add to Playlist only.

### Generalize the existing picker

Picker state changes from one track ID to an ordered non-empty list. Confirmation passes that list unchanged to `append` or `createWithEntries`; dismissing the picker retains selection, mutation failure retains picker and selection, and success closes both. The playlist-detail searchable browser remains unchanged.

### Logical version-1 backup instead of SQLite copying

The interchange is canonical compact UTF-8 JSON named with `.rhythhaus-playlists.json` and MIME type `application/vnd.rhythhaus.playlists+json`. Root key order is `format`, `version`, `exportedAtEpochMillis`, `playlists`, `checksumCrc32`; playlist keys are `name`, `entries`; entry keys are `title`, `artist`, `album`, `durationSeconds`. The discriminator is `rhythhaus-playlist-backup`, version is integer `1`, and CRC32 is exactly eight lowercase hexadecimal characters calculated over the canonical root payload without the checksum field.

CRC32 detects accidental corruption but is not authentication. A strict fixed-schema parser is used so duplicate keys, unknown fields, malformed UTF-8, non-integer numbers, and trailing content are rejected rather than normalized by a permissive decoder.

### Bounded decoding and export validation

Version 1 permits at most 4 MiB, 1,000 playlists, 10,000 entries per playlist, 100,000 total entries, and 1,024 Unicode code points per name or metadata string. Duration is an integer from 0 through 604,800 seconds. A source entry whose current library track is missing, has no duration, or has an invalid duration makes export fail before the save panel opens.

### Unique portable matching

Title, artist, and album are normalized by trimming Unicode whitespace, collapsing each non-empty whitespace run to one ASCII space, and locale-independent lowercasing. Version 1 retains punctuation, diacritics, and compatibility distinctions. Duration must be known and within an inclusive plus-or-minus two seconds. Exactly one candidate restores; zero is unmatched; more than one is ambiguous. The importer never weakens the key or chooses the first candidate.

### Preview, naming, revision, and transaction

Import fully validates and plans before writing. The preview records an authoritative library revision plus per-playlist and total restorable, unmatched, and ambiguous counts. A changed revision invalidates confirmation.

Every import creates new playlists. Conflicts use localized `Imported`/`导入` suffixes and incrementing numbers against existing and earlier planned names. Playlists with no restorable entries are skipped. One new `PlaylistRepository.importPlaylists` call creates every eligible playlist and ordered duplicate entry in one transaction or none.

### Platform document seam

Common code models success, cancellation, unavailable integration, oversized input, and failure. Android uses create/open document contracts without broad storage permission; iOS uses a retained `UIDocumentPickerViewController` bridge with balanced security-scoped access and temporary-file cleanup; JVM/macOS uses native AWT save/load panels. Reads stop at 4 MiB plus one byte. Platform adapters never access `rhythhaus.db`.

## Risks / Trade-offs

- **Long press can conflict with click dispatch** → Use one combined-click contract and executable semantics/dispatch tests proving no playback side effect.
- **Bottom bars can overlap or leave invisible hit targets** → Give the app shell one mutually exclusive measured slot and verify pointer/semantic visibility.
- **Metadata-only matching can miss renamed tracks or produce ambiguity** → Report both outcomes and never guess; retain the source file for later retry.
- **Strict JSON increases implementation effort** → Keep the parser fixed to the small version-1 schema and test all malformed/boundary cases.
- **CRC32 is not tamper protection** → Treat imported files as untrusted bounded data; describe CRC only as integrity detection.
- **Library changes after preview** → Bind plans to an authoritative revision and reject stale confirmation before repository mutation.
- **Swift-exported bridge names may differ** → Compile the bridge early and adjust only to generated signatures before downstream UI integration.
- **Platform picker tests do not prove runtime presentation** → Require Android/iOS/JVM runtime QA and report unavailable targets as unverified.

## Migration Plan

1. Add pure selection and backup contracts behind tests.
2. Replace eligible row actions and add the contextual bar without changing playlist-detail behavior.
3. Add the strict codec, matcher, import plan, and repository transaction API. No SQLDelight schema migration is required.
4. Add platform document adapters and Settings orchestration.
5. Run strict OpenSpec, JVM, Android, desktop, and iOS gates plus runtime/visual QA.

Rollback removes the new UI paths, backup adapters, and repository API while leaving persisted data valid because no schema changes occur. Playlists already imported remain ordinary local playlists.

## Open Questions

None. Implementation-time Swift bridge spellings and bottom-bar measurement plumbing are compile-time integration details, not unresolved product behavior.
