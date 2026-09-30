# Spec Delta

## ADDED Requirements

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
