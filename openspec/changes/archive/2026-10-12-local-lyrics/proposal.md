# Proposal

## Why

RhythHaus currently has no lyrics experience despite indexing local audio and supporting offline playback. Local synchronized lyrics and embedded lyrics make Now Playing more useful without introducing network access, accounts, or external service dependencies.

## What Changes

- Read a same-stem `.lrc` sidecar where the platform scanner can safely access a sibling file; prefer it over embedded lyric metadata.
- Extract embedded synchronized or plain lyric text through the existing TagLib boundary.
- Persist the selected local lyric text with its library track and expose it through library/playback projections.
- Add an optional Now Playing lyrics surface with current-line synchronization for valid LRC timestamps and readable plain-text lyrics otherwise.
- Keep lyrics local-only; add no remote lookup, media/tag writes, or audio-engine behavior.

## Capabilities

### New Capabilities
- `local-lyrics`: locally sourced embedded/sidecar lyric discovery, persistence, parsing, and Now Playing presentation.

### Modified Capabilities
- `local-library-scanning`: scan and persist lyrics that are available from the audio item's embedded metadata or an accessible adjacent sidecar.

## Scope Notes

- The selected lyric text is capped at 512 KiB before persistence.
- A same-stem `.lrc` sidecar takes precedence over embedded text. When no readable sidecar is available, embedded lyrics are used.
- Android MediaStore access does not provide a general sibling-file contract; MediaStore tracks use embedded lyrics only. SAF sources may use readable sibling `.lrc` documents.
- All release UI and device acceptance remains user-performed; implementation ownership still includes automated tests and builds.
