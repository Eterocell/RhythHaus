# Proposal

## Why
The Queue tab lets listeners manage the current playback sequence but cannot keep it for later. Saving that sequence as a playlist makes an existing, deliberately arranged queue reusable without introducing another persistence model.

## What Changes
- Add an accessible, localized Save queue as playlist action to the nonempty Queue tab.
- Capture the current and upcoming queue occurrences in visible order, including repeated tracks; exclude previously played occurrences.
- Reuse the existing playlist naming dialog and atomic `createWithEntries` repository operation, with success/failure feedback.
- Preserve the active playback queue, progress, repeat/shuffle modes, timer, and media files.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `saved-playlists`: Queue-tab snapshot creation as an ordered, persistent saved playlist.

## Impact
`:feature:playlists:impl` Queue tab and resources; `:shared` playlist route adapter and focused tests. No schema migration, new dependencies, platform-specific integrations, or playback-engine changes.
