# Smart Playlists

## Purpose
Provide durable named single-rule playlists whose ordered contents follow the current local library, favorites, playback history, or a static saved playlist without changing those sources or active playback.

## ADDED Requirements

### Requirement: Named durable smart rules
The system SHALL let a listener create a nonblank named smart playlist, rename it, change its rule, and delete it with confirmation. The available single rules SHALL be Favorites, Recently Played (latest 10, 25 or 50 distinct library tracks with a play record), Recently Added (latest 10, 25 or 50 library tracks), exact Artist, exact Artist+Album, or membership in a selected static saved playlist including playlists created from Queue. The rule SHALL be persisted across restart separately from static playlist entries. A failed create, edit, or delete SHALL retain the editor or confirmation and leave confirmed state unchanged. No rule-management operation SHALL change playback, library files/tags, or source access.

#### Scenario: Persist and reopen a rule
- **WHEN** the listener creates a smart playlist named "Road trips" using a selected static saved playlist and restarts the app
- **THEN** the named rule SHALL remain available and derive its current entries from the saved playlist, not a creation-time track snapshot

#### Scenario: Reject invalid rule edits
- **WHEN** a listener attempts a blank name, an unsupported recency count, or a source ID not belonging to an existing static saved playlist
- **THEN** no smart-playlist definition SHALL be created or changed and the edit surface SHALL remain actionable

### Requirement: Deterministic dynamic membership
The system SHALL compute smart-playlist rows from the current authoritative Library projection and confirmed static saved-playlist snapshot. Favorites, Artist, and Album SHALL contain each eligible current track once, sorted case-insensitively by title then track ID; album matching SHALL use the selected artist and album together. Recently Played SHALL sort recorded tracks by descending `lastPlayedAtEpochMillis`, then track ID, and take N; Recently Added SHALL sort library tracks by descending `createdAtEpochMillis`, then track ID, and take N. A saved-playlist rule SHALL keep the source's current entry order and repeat occurrences, omitting track IDs no longer present in the Library. Track identity SHALL remain stable across a rescan. No clock cutoff or background materialization SHALL be introduced.

#### Scenario: React to new history and library changes
- **WHEN** a recorded play or scan changes the authoritative Library state while a smart detail is open
- **THEN** its matching rows SHALL update in that state publication and preserve the deterministic rule ordering

#### Scenario: Follow edits to a saved Queue playlist
- **WHEN** a referenced static playlist is reordered, appended to, or loses an entry
- **THEN** the derived smart playlist SHALL reflect the new ordered occurrence list without changing the source playlist or the active playback queue

#### Scenario: Source or track disappears
- **WHEN** a source saved playlist is deleted
- **THEN** the smart playlist SHALL remain named and editable, show an empty missing-source notice, and permit choosing another rule
- **WHEN** a library track is removed
- **THEN** no derived row SHALL refer to that removed track

### Requirement: Browse and play smart lists without manual entry edits
The Saved tab SHALL visibly distinguish smart rules from static playlists, expose creation and a read-only derived detail, and allow playing a selected row using the complete currently visible ordered rows as the queue. It SHALL NOT offer static entry append, remove, or reorder operations on a smart list. Empty lists SHALL have a localized message. The creation/editor/detail and confirmation UI SHALL work with keyboard and accessible names in English and Simplified Chinese at 600×400 dp; modal Back SHALL close the current editor or confirmation before changing the Shared route. Deletion SHALL invalidate only the confirmed deleted destination; stale or failed outcomes MUST NOT dismiss an unrelated route or new modal.

#### Scenario: Start playback from a derived row
- **WHEN** a listener selects the second row of a smart playlist with visible tracks A, B, A
- **THEN** playback SHALL select the second occurrence and keep A, B, A in its queue while the smart rule and source playlist remain unchanged

#### Scenario: Dismiss and retry failed changes
- **WHEN** a rule mutation fails and the listener cancels or retries
- **THEN** the failure SHALL not close the modal or mutate an unrelated new draft; cancelling SHALL leave the original rule intact

### Requirement: Preserve static playlist and backup compatibility
Smart playlist definitions SHALL be stored separately from `playlist` and `playlist_entry`. The v1 JSON backup SHALL continue to import/export static saved playlists only, with a visible smart-list explanation that its rules are not included. Existing v1–v4 databases, legacy version-zero databases, static playlist rows, favorites, and play history MUST survive the schema migration. Unsupported or corrupt stored rule data MUST surface a recoverable read failure rather than silently returning invented empty rows.

#### Scenario: Upgrade an existing library
- **WHEN** a version-4 database holding static playlists and play history opens after upgrade
- **THEN** those existing rows SHALL remain intact, the new smart-list table SHALL exist, and creating a smart playlist SHALL work without altering static data

#### Scenario: Back up while smart lists exist
- **WHEN** the listener exports a v1 playlist backup
- **THEN** only static saved playlists SHALL be exported, and the UI SHALL state that smart-list rules are excluded
