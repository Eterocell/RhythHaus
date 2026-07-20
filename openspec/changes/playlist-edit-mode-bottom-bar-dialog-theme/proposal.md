## Why

Saved-playlist rows permanently expose mutation controls and therefore differ from the familiar album and artist track-list experience. Playlist detail also does not participate in the established bottom-bar scroll behavior, and dialogs can show a light-looking exterior in dark mode.

## What Changes

- Make playlist detail use the album/artist track-row presentation and playback behavior by default.
- Add a list-wide playlist edit mode entered by long-pressing any row. Edit mode exposes drag, move-up, move-down, and remove controls; tapping outside the editable list or pressing Back exits it.
- Connect playlist-detail scroll position and measured bottom clearance to the existing app-shell Bottom Bar hide/reveal policy.
- Make `HausDialog` and `HausLazyDialog` exterior/scrim presentation visibly resolve from the active light or dark theme.

## Capabilities

### New Capabilities
- `playlist-detail-editing`: Controls the playlist detail default presentation, transient whole-list edit mode, and accessible reorder/removal interactions.
- `playlist-scroll-chrome`: Applies the shared album/artist Bottom Bar scroll visibility and clearance contract to playlist detail.
- `theme-aware-dialog-exterior`: Requires dialog exterior/scrim presentation to follow the active Haus theme.

### Modified Capabilities
None.

## Impact

- Shared Compose UI in `PlaylistScreens.kt`, track-row/detail scroll helpers, `LibraryRoutes.kt`, and `LibraryAppShell.kt`.
- Shared dialog presentation in `HausDialog.kt` and palette resolution in the theme package.
- Focused common/JVM UI and presentation-policy tests; no persistence schema, public API, dependency, or platform-specific media change.
