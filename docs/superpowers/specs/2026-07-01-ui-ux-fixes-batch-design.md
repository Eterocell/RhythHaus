# UI/UX Fixes Batch Design

## Context

The current RhythHaus shared Compose UI has several source-backed UX issues: the primary first-run import action is hidden in Settings even though an `ImportAudioCard` exists, the album grid is hardcoded to two columns, normal users can reach TagLib developer panels, Search starts playback but leaves the user stranded in the overlay, several small navigation/action controls are below comfortable touch-target size, and browsing only supports Albums and Artists.

This batch implements the highest-value fixes that are small enough to land together without changing playback, scanner, database, or platform integration behavior.

## Goals

- Make first-run/empty-library onboarding obvious from Home with an Add music folder call to action.
- Make the album grid adapt between phone, tablet, and desktop widths.
- Remove developer TagLib panels from normal user-facing Now Playing and Home flows.
- Improve Search result behavior by closing Search after a result starts playback and by adding an in-field clear action.
- Increase the effective hit targets for compact back/search/settings controls to at least 44dp.
- Add a Songs browse mode so users can browse tracks directly without entering album/artist detail first.
- Keep all work in shared Compose/common code unless a test requires otherwise.

## Non-goals

- No native SwiftUI, Android, or desktop-specific UI rewrite.
- No new dependencies.
- No scanner, metadata extraction, playback engine, MediaSession, audio-session, or persistence schema changes.
- No playlists, genres, recently added, queue management redesign, or stable album identity redesign in this batch.
- No live visual QA claims without device/screenshot evidence.

## Design

### Home onboarding

When the library is empty, Home should show a primary `ImportAudioCard` above the browse section. It should reuse the existing Add music folder button and copy rather than creating a second import path. The bottom bar can continue to provide Settings/Search navigation, but it should not be the only path to first-run import.

### Adaptive album grid

Replace the hardcoded `albums.chunked(2)` rendering with a tiny pure helper that maps the available width to a column count. The shared UI will use `BoxWithConstraints` around the album section and render album rows using that count. Initial breakpoints:

- width under 560dp: 2 columns
- 560dp to under 900dp: 3 columns
- 900dp and wider: 4 columns

This keeps the current phone shape while using desktop/tablet space better. A common test will cover the breakpoint helper.

### Songs browse mode

Extend `BrowseMode` from Albums/Artists to Albums/Artists/Songs. Songs mode renders `TrackRow` entries from `snapshot.tracks` in library order and wires clicks to the same playback queue behavior used by Home. This gives users a direct all-tracks path without inventing folders/genres yet.

### Search polish

Search keeps filtering title/artist/album. The search field gains a clear action shown only when the query is not empty. Result selection keeps the current behavior of setting the whole library queue and starting playback, then dismisses Search so the user returns to the route that opened it with the mini-player updated.

### Touch target polish

`BackChip` should use a minimum 44dp height while preserving the same visual identity. Search and Settings controls in `NowPlayingBar` should use 44dp hit boxes with the existing icon sizes and tints. The play/pause mini button is already 36dp and visually central; this batch focuses on the smaller navigation/action controls identified in review.

### Developer panel removal

The normal UI should not render TagLib developer metadata panels. Remove the expanded Now Playing `DeveloperTrackPanel` call and remove dead developer-only composables/imports from `App.kt` / `NowPlayingScreen.kt` if they become unused. This does not remove TagLib reading from scanner or metadata code.

## Acceptance criteria

- Empty Home renders a visible Add music folder path without requiring Settings.
- Album column count is tested and adapts to width.
- Browse mode includes Songs and can start playback from a song row.
- Search result click starts playback and dismisses Search.
- Search clear action empties the query.
- BackChip and bottom-bar Search/Settings controls have at least 44dp effective hit targets.
- User-facing UI no longer renders `DEV · TagLib` or TagLib property panels in normal flows.
- Focused tests and broad JVM/desktop/Android verification pass, or blockers are recorded exactly.

## Risks

- `App.kt` is large; keep edits targeted and avoid broad file splitting.
- Compose UI behavior is hard to prove without screenshots/devices; automated verification covers pure helpers and compilation, while manual visual validation remains recommended.
- Songs mode reuses the existing full-library queue. That matches current Home/NowPlaying behavior and avoids queue-model changes.
