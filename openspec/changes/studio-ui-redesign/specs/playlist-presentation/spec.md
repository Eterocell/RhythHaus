# Playlist presentation delta

## Purpose

Define clear presentation of saved static and smart playlists, parameter editing and distinct local document operations while retaining all existing repository, ordering and transaction contracts.

## ADDED Requirements

### Requirement: Saved-list destination
The Playlists primary destination SHALL show saved static and smart lists with localized type distinctions, not a Queue/Saved tab hub. Create MUST offer the two supported types. Existing opening, naming, renaming, deleting, adding, ordering and removal operations MUST remain reachable; moving queue to the player MUST NOT alter ongoing playback.

#### Scenario: Browse mixed list types
- **WHEN** the listener opens Playlists containing static and smart lists
- **THEN** both types MUST be distinguishable and open their correct existing detail behavior without first choosing a Saved tab

### Requirement: Progressive smart-rule editing
Smart-rule editing SHALL separate rule choice from only its applicable parameters. Long artist, album and saved-source choices MUST scroll independently of reachable confirm/cancel actions. Compact editing MUST use a near-full-height surface; wide editing MUST have constrained width/height. Existing single-rule types, validation, ordering, duplicates and missing-source behavior MUST remain unchanged.

#### Scenario: Edit a rule with many options
- **WHEN** the listener edits an artist/album/saved-source rule with a long parameter list at 200% font scale
- **THEN** the selected parameter and confirm/cancel actions MUST remain reachable without adding rule composition or losing the draft

### Requirement: Distinct document operations
JSON backup/restore SHALL remain distinct from single-static-playlist M3U/M3U8/PLS import/export with explicit format choice. Existing Settings entries MUST remain; corresponding playlist entries MUST delegate to the same operation owner and platform bridge. Preview, matching, revision validation, cancellation, atomic application and retry semantics MUST remain intact; imported paths MUST NOT grant source access.

#### Scenario: Return from cancelled export
- **WHEN** a JSON or interoperable document operation is cancelled or fails
- **THEN** no partial data MUST be applied, the real outcome MUST be shown, and a later request MUST be available through the same exclusive operation owner

#### Scenario: Distinguish backup coverage
- **WHEN** the listener prepares JSON backup or a single-playlist interoperable export
- **THEN** the UI MUST explain their different coverage, including the existing exclusion of smart rules from static JSON backup, without relabeling them as interchangeable backups

### Requirement: Accessible list mutation feedback
List detail and picker/editor overlays SHALL share song-row hierarchy while preserving occurrence-specific identity, ordered selection and stale-response protection. Reordering MUST have an accessible alternative to drag; confirmation, saving and errors MUST retain draft context and block duplicate submission. Unsupported page-scoped library selection MUST NOT be introduced into saved-list detail or Queue.

#### Scenario: Duplicate occurrences and stale confirmation
- **WHEN** a listener edits repeated entries or dismisses an operation before its response arrives
- **THEN** operations MUST preserve distinct entry IDs and a stale response MUST NOT mutate a replacement draft or playback context
