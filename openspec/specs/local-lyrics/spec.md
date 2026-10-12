# Local Lyrics

## Purpose

Provide offline synchronized or plain lyrics for locally indexed tracks without remote lookup, account access, or media-file mutation.

## Requirements

### Requirement: Local lyrics are discovered and stored with a library track
The system SHALL persist one bounded local lyric document with its stable library track, preferring a readable same-stem `.lrc` sidecar over embedded audio metadata. Optional lyric failures SHALL NOT fail or skip an otherwise readable audio track.

#### Scenario: Sidecar takes precedence
- **WHEN** a scan reads an audio file with both embedded lyrics and a readable same-stem `.lrc` sidecar
- **THEN** the persisted lyrics are the sidecar text
- **AND** the audio track remains identified by its existing source-local key

#### Scenario: Embedded lyrics fallback
- **WHEN** a readable audio file has embedded lyrics and no readable same-stem sidecar
- **THEN** the embedded lyric text is persisted

#### Scenario: MediaStore embedded lyrics
- **WHEN** Android scans a MediaStore content URI with embedded lyrics
- **THEN** embedded lyrics are extracted through the readable content descriptor
- **AND** no sibling-file lookup is attempted

#### Scenario: Missing or oversized lyric candidate
- **WHEN** a sidecar is missing, unreadable, malformed, or larger than 512 KiB
- **THEN** the scanner falls back to valid embedded lyrics when available
- **AND** otherwise stores the audio track without lyrics
- **AND** scanning continues without a lyric-specific scan error

#### Scenario: Existing database migrates
- **WHEN** a database created before local lyrics is opened
- **THEN** the migration preserves track identity, source, scan facts, favorites, history, metadata corrections, and playlists
- **AND** existing tracks have no lyrics until a later scan reads them

### Requirement: LRC text is parsed safely for synchronized presentation
The system SHALL parse supported LRC timestamps and offsets for synchronized presentation while preserving plain lyric text and ignoring malformed timing syntax.

#### Scenario: Multiple timestamps and offset
- **WHEN** one LRC line has multiple valid timestamps and a global offset
- **THEN** each timestamp produces an entry with the same lyric text at its adjusted non-negative position
- **AND** entries are ordered by effective position while ties preserve source order

#### Scenario: Mixed valid and malformed lines
- **WHEN** an LRC document contains valid timestamps, malformed timestamps, and plain lines
- **THEN** valid timed entries remain synchronized
- **AND** plain lyric text remains visible
- **AND** malformed timestamp syntax is not interpreted as a timed entry

### Requirement: Now Playing exposes local lyrics without changing playback
Now Playing SHALL provide an optional lyrics surface only when the current track has non-empty lyrics. Timed lyrics SHALL identify the line active at the current playback position and update as playback position changes. Untimed lyrics SHALL be readable as plain text. Opening or closing lyrics SHALL NOT change playback, queue, position, or system-media state. Lyrics SHALL be displayed locally with no network request.

#### Scenario: Timed lyric follows playback
- **WHEN** Now Playing displays valid timed lyrics and playback position crosses a lyric timestamp
- **THEN** the corresponding line becomes the active semantic line
- **AND** the lyric view follows the current line without issuing a seek or transport command

#### Scenario: Plain lyrics remain readable
- **WHEN** the selected lyric text has no valid timestamps
- **THEN** Now Playing displays the text as a plain, scrollable lyric document

#### Scenario: No lyrics available
- **WHEN** the current track has no selected non-empty lyrics
- **THEN** Now Playing does not expose an empty lyrics action or surface

#### Scenario: Lyrics surface is dismissed
- **WHEN** the listener opens and closes the lyrics surface
- **THEN** the existing playback state, queue occurrence, and position remain unchanged
