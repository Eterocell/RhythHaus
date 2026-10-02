# Playlist Interoperability Specification

## Purpose

Exchange static saved playlists as bounded UTF-8 M3U, M3U8, or PLS documents while preserving ordered duplicate entries, previewing metadata matches, and keeping library access and playback unchanged.

## Requirements

### Requirement: Export static playlists as M3U or PLS

The system SHALL export a selected static saved playlist as UTF-8 M3U/M3U8 or PLS, preserving entry order and duplicate occurrences. M3U output SHALL include `#EXTM3U` and one `#EXTINF` record per entry; PLS output SHALL include indexed `FileN`, `TitleN`, `LengthN` records and a correct `NumberOfEntries`. Smart playlist rules SHALL NOT be serialized as static interoperable entries, and the export surface SHALL explain that limitation.

#### Scenario: Export a playlist with duplicates
- **WHEN** a static playlist contains the same track occurrence twice
- **THEN** the selected format contains two ordered records for that track
- **AND** the export does not alter the saved playlist or playback queue

#### Scenario: Export cannot resolve an entry
- **WHEN** a playlist entry no longer references a readable library track
- **THEN** export fails with a localized recoverable error
- **AND** no partial document is presented as successful

### Requirement: Import M3U, M3U8, and PLS with preview matching

The system SHALL accept UTF-8 M3U/M3U8 and PLS documents within the existing bounded document limits, parse ordered entries, and match each entry to the current library using effective title, artist, album, and duration metadata. A unique match is restorable; no match and multiple matches are shown as distinct preview issues. Paths and embedded location strings SHALL NOT override metadata matching or grant new source access.

#### Scenario: Preview mixed match results
- **WHEN** an imported document contains unique, missing, and ambiguous entries
- **THEN** the preview reports each category and retains source order
- **AND** confirmation remains available for playlists containing at least one unique match

#### Scenario: Reject malformed or oversized input
- **WHEN** an input exceeds limits or violates the supported M3U/PLS grammar
- **THEN** no repository mutation occurs
- **AND** the user receives a specific recoverable validation error

### Requirement: Confirm import atomically

The system SHALL create confirmed imported playlists through the existing atomic ordered-entry repository operation. Unmatched and ambiguous entries SHALL be omitted, duplicate source occurrences SHALL remain duplicated, and a cancelled or failed confirmation SHALL leave playlists and playback unchanged. Imported names SHALL be sanitized only as needed for the platform's document format and SHALL never be blank.

#### Scenario: Confirm a partially restorable playlist
- **WHEN** the user confirms a preview with unique matches and issues
- **THEN** one saved playlist is created with only unique matches in original order
- **AND** the result reports created playlists and omitted entries

#### Scenario: Cancel after preview
- **WHEN** the user dismisses the preview
- **THEN** no playlist or entry is created
- **AND** a later import starts with a fresh preview

#### Scenario: Library changes before confirmation
- **WHEN** the current library revision no longer matches the import preview
- **THEN** confirmation is rejected without creating any playlist
- **AND** the user can open the document again to obtain a fresh preview

### Requirement: Format selection preserves JSON backup behavior

The document surface SHALL expose separate RhythHaus JSON backup and interoperability actions. Interoperability export SHALL require one selected static playlist and an explicit M3U, M3U8, or PLS format. Platform document adapters SHALL use the selected format's extension and supported content types rather than routing interoperability documents through JSON decoding. Cancellation or failure SHALL release the operation gate so a subsequent operation is possible. Existing JSON backup validation and content SHALL remain unchanged; smart playlist rules SHALL remain excluded from both JSON v1 and interoperable documents.

#### Scenario: Export the selected format
- **WHEN** the user selects a saved playlist and PLS, M3U, or M3U8 export
- **THEN** only that playlist is encoded in the selected format and offered with its corresponding extension
- **AND** JSON backup actions continue to use the original JSON backup behavior

#### Scenario: Cancel platform selection
- **WHEN** the user cancels the platform document picker
- **THEN** no playback or playlist mutation occurs
- **AND** the user can start a new import or export
