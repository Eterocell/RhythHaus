# Design: Vector Icon Controls

## Decision

Use Material Icons for the shared Compose control icons:

- Play: `Icons.Filled.PlayArrow`
- Pause: `Icons.Filled.Pause`
- Previous: `Icons.Filled.SkipPrevious`
- Next: `Icons.Filled.SkipNext`
- Search: `Icons.Filled.Search`
- Settings: `Icons.Filled.Settings`

Miuix 0.9.2 exposes a Search icon but does not provide the full required transport/settings set. Material Icons provide a complete cohesive set and can be rendered through Compose `Icon` with explicit tint, size, and content descriptions.

## Scope

Replace emoji/text controls in:

- `NowPlayingBar.kt` mini play/pause button and search/settings buttons.
- `NowPlayingScreen.kt` previous, play/pause, and next transport buttons.

Keep non-control fallback text such as album-art initials or fallback music-note artwork unless separately requested.

## Accessibility

Every icon control should expose a content description appropriate to its action: Play, Pause, Previous track, Next track, Search, and Settings. Decorative fallback artwork is not part of this change.

## Verification

- Source search should find no remaining targeted control glyphs in `NowPlayingBar.kt` or `NowPlayingScreen.kt`.
- Shared JVM compile and broad JVM/desktop/Android verification should pass.
