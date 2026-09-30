# Proposal

## Why

Scanned tags are currently the only title, artist, album, and disc/track ordering metadata shown by RhythHaus. A listener cannot correct incorrect or missing tags without editing the source audio outside the app, and later scans would otherwise replace a corrected display value.

## What Changes

- Allow editing title, artist, album, track number, and disc number for an indexed track inside the app; allow individual fields or all fields to revert to scanned metadata.
- Store only listener-specified overrides keyed to stable library track ID; keep scanned tags and playable file paths unmodified.
- Apply effective metadata consistently to Library browsing, sorting/grouping, playlists, search, queue creation, and new playback selections; reconcile current playback presentation without restarting its audio or changing queue identity.
- Retain overrides across scans of the same source-local track; cascade deletion with removed tracks. Show errors on rejected writes and preserve an unsaved edit draft.
- Supply English and Simplified Chinese UI/accessibility labels. Device UI/listening validation remains user-owned.

## Capabilities

### New Capabilities
- `track-metadata-overrides`: editable, reversible, app-local track metadata projected across the library and playback surfaces.

### Modified Capabilities
- `local-library-scanning`: scanned raw tags and listener overrides have separate lifecycles; track removal also removes overrides.

## Impact

SQLDelight schema and v5→v6 migration in `:core:database`; typed repository contracts and persistence in `:feature:library`; app-owned authoritative publication and playback reconciliation in `:shared`; shared Compose Library editing UI and resources. No original media/tag write, no platform-specific editor, and no change to playlist-backup v1 format.
