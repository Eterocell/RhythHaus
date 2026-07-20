# Playlist Backup

## Purpose

Define the portable, validated, non-destructive playlist backup and restore behavior across supported platforms.

## Requirements

### Requirement: Canonical version-1 playlist backup
The system SHALL export all saved playlists in deterministic order as canonical compact UTF-8 JSON using discriminator `rhythhaus-playlist-backup`, integer version `1`, extension `.rhythhaus-playlists.json`, and MIME type `application/vnd.rhythhaus.playlists+json`. The root key order MUST be `format`, `version`, `exportedAtEpochMillis`, `playlists`, `checksumCrc32`; playlist keys MUST be `name`, `entries`; entry keys MUST be `title`, `artist`, `album`, `durationSeconds`. CRC32 MUST be eight lowercase hexadecimal characters over the canonical root payload with the checksum field omitted.

#### Scenario: Export a deterministic logical backup
- **WHEN** the user exports valid saved playlists
- **THEN** the system SHALL produce canonical ordered bytes containing playlist names and ordered portable metadata while excluding database IDs, local paths, sources, artwork, audio, and other app state

#### Scenario: Reject an unexportable entry
- **WHEN** a referenced source track is missing, has unknown duration, or has duration outside 0 through 604800 seconds
- **THEN** the system MUST fail export before opening the save panel

### Requirement: Strict bounded import validation
The system MUST reject input larger than 4 MiB, more than 1000 playlists, more than 10000 entries in one playlist, more than 100000 total entries, or strings longer than 1024 Unicode code points. It MUST reject malformed UTF-8 or JSON, duplicate or unknown keys, missing fields, non-integer numbers, invalid durations, blank playlist names, unsupported format/version, invalid CRC32, and trailing content before mutation.

#### Scenario: Reject malformed or oversized input
- **WHEN** the selected file violates any version-1 syntax, integrity, field, byte, count, string, or duration contract
- **THEN** the system MUST return a distinct recoverable validation outcome and MUST NOT mutate playlists

### Requirement: Unique portable destination matching
The system SHALL match each entry by title, artist, album, and known duration. Text normalization MUST trim Unicode whitespace, collapse each non-empty whitespace run to one ASCII space, and lowercase without the device locale while retaining punctuation, diacritics, and compatibility distinctions. Duration tolerance MUST be inclusive plus or minus two seconds. Exactly one candidate SHALL be restorable; zero SHALL be unmatched; multiple SHALL be ambiguous, and the system MUST NOT guess.

#### Scenario: Restore a unique match
- **WHEN** exactly one destination track matches all normalized text and duration constraints
- **THEN** the import plan SHALL resolve the entry to that local track ID

#### Scenario: Report an ambiguous match
- **WHEN** more than one destination track satisfies the complete matching key
- **THEN** the system SHALL report the entry as ambiguous and MUST exclude it from mutation

### Requirement: Preview and stale-plan protection
The system SHALL present a no-write preview containing per-playlist and total restorable, unmatched, and ambiguous counts plus accessible issue entries. A preview with no restorable entries MUST NOT be confirmable. The plan MUST record the authoritative library revision, and confirmation MUST reject a changed revision before repository mutation.

#### Scenario: Inspect before writing
- **WHEN** a valid backup has been decoded and matched
- **THEN** the system SHALL show the complete preview without creating or changing a playlist

#### Scenario: Reject a stale preview
- **WHEN** the authoritative library revision changes after preview and before confirmation
- **THEN** the system MUST reject confirmation without invoking the repository mutation

### Requirement: Non-destructive atomic restore
Import SHALL create only new playlists containing at least one restorable entry, preserve playlist order, entry order, and duplicate occurrences, and skip all-unmatched/all-ambiguous playlists. Name conflicts MUST use deterministic localized Imported/导入 suffixes and incrementing numbers against existing and earlier planned names. One confirmed import MUST create every eligible playlist in one repository transaction or none.

#### Scenario: Import conflicting and duplicate entries
- **WHEN** a backup contains a conflicting playlist name and repeated uniquely matched entries
- **THEN** the system SHALL create a uniquely suffixed new playlist and preserve the repeated ordered entries without changing the existing playlist

#### Scenario: Roll back a failed import
- **WHEN** any playlist or entry creation fails during confirmation
- **THEN** the system MUST leave all pre-import playlists unchanged and MUST retain no partial imported playlist or entry

### Requirement: Cross-platform system document integration
Settings SHALL expose export and import actions using system document panels on Android, iOS, and desktop JVM/macOS. Android MUST use document contracts without broad storage permission; iOS MUST use a document-picker bridge with temporary-file cleanup and balanced security-scoped access; JVM/macOS MUST use native save/load panels. Reads MUST stop at 4 MiB plus one byte. Cancellation MUST be silent and distinct from unavailable integration and I/O failure. Platform adapters MUST NOT access or replace `rhythhaus.db`.

#### Scenario: Cancel a system document panel
- **WHEN** the user cancels export or import selection
- **THEN** the system SHALL close the operation without an error and without changing playlists

#### Scenario: Complete import reporting
- **WHEN** a confirmed import succeeds
- **THEN** the system SHALL report playlists created and skipped plus entries restored, unmatched, and ambiguous
