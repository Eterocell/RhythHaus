# Vector Icon Controls Spec

## Decision

Replace emoji/text glyphs in transport/search/settings controls with Material vector icons.

## Requirements

- Mini-player play/pause/search/settings controls use vector icons, not emoji/text glyphs.
- Full now-playing previous/play-pause/next controls use vector icons, not emoji/text glyphs.
- Existing click behavior, playback behavior, navigation behavior, sizing, and colors remain unchanged unless a minor icon-size adjustment is needed for visual centering.
- Each icon has a meaningful content description.
- Album artwork fallback initials/music-note text are out of scope for this change.

## Dependency choice

Use Material Icons, because Miuix 0.9.2 only exposes Search in the available icon package while Material Icons cover all required transport/search/settings actions coherently.
