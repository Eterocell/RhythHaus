# Playlist Edit Mode, Bottom Bar, and Dialog Theme Design

## Summary

RhythHaus will make saved-playlist detail read like the existing album and artist track lists until the user explicitly enters a transient, page-wide editing mode. Playlist scrolling will report through the existing library-shell Bottom Bar visibility and measured-clearance contract. `HausDialog` and `HausLazyDialog` will use a dark-theme exterior/scrim that is visibly dark and palette-derived instead of a light-looking background.

## Goals

- Match the default playlist row to the existing album/artist track-list layout: artwork, title, artist/album metadata, duration, and click-to-play.
- Keep drag, move-up, move-down, and remove controls hidden until the user long-presses any playlist row.
- Enter one page-wide edit mode, exit it on a tap outside the editable list or on Back, and require a second Back to navigate.
- Preserve duplicate entries as independent occurrences using `PlaylistEntry.id` for row keys, playback selection, drag/reorder, and removal.
- Reuse the existing album/artist Bottom Bar scroll-down hide, scroll-up reveal, and shell-measured content-clearance behavior.
- Make the exterior/scrim and panel of both Haus dialog variants visibly follow the active palette in light and dark themes.

## Non-Goals

- Changing playlist persistence, database schema, queue semantics, route definitions, or media integrations.
- Adding playlist edit operations beyond reorder and removal.
- Changing the existing Bottom Bar policy, theme palette definitions, or album/artist detail design.

## User Interaction

### Default playlist detail

Each playlist row uses the shared track-list visual treatment and remains a playback target. It has no drag handle, reorder buttons, or removal control. Clicking a row starts playback at the clicked visible playlist occurrence.

### Playlist edit mode

Long-pressing any playlist row enters edit mode for the entire playlist page. Every editable row then exposes a drag handle, move-up, move-down, and remove control. Boundary move controls are disabled. Drag is optional enhancement: labeled move controls remain available as accessible non-drag alternatives, and removal keeps its existing confirmation.

Tapping outside the editable list consumes that interaction and exits edit mode. Back exits edit mode before it is allowed to navigate away. Row clicks in edit mode do not start playback.

### Bottom Bar

Playlist detail reports its `LazyListState` through the same shell callback/reducer used by album and artist detail. It uses the shell's active measured Bottom Bar clearance as list content padding; it does not define a separate threshold, visibility state, or fixed footer height.

### Dialog theme exterior

Both `HausDialog` and `HausLazyDialog` resolve their opaque panel and exterior/scrim from `HausColors.current`. In light mode the exterior remains a restrained palette-derived ink dim. In dark mode the exterior is dark and palette-derived, so it cannot appear as a light background around the dark dialog panel.

## Technical Design

1. Extract or reuse the established track-row visual content without coupling playlist entry identity to bare `Track` identity.
2. Add transient playlist-detail edit-mode state and gate row controls/gesture behavior on it.
3. Keep all playback and mutation commands keyed by `PlaylistEntry.id`, including duplicate-track entries.
4. Route playlist scrolling and measured clearance into the existing library shell instead of duplicating `DrillDownView`'s artwork-specific screen structure.
5. Centralize dialog presentation policy so both dialog variants resolve equivalent light/dark panel and scrim values.

## Verification

- Focused policy/UI tests for default/edit row presentation, whole-page long press, entry-ID duplicate safety, boundary actions, outside-tap/Back precedence, and shared Bottom Bar scrolling/clearance.
- Dialog presentation tests for `HausDialog` and `HausLazyDialog` in both palettes.
- Strict OpenSpec validation and relevant shared JVM/platform checks after implementation.

## Implementation Slices

1. Add row/edit-state and dialog/scroll presentation contracts with focused tests.
2. Implement the shared default row treatment and page-wide playlist edit mode.
3. Wire playlist scroll state and bottom clearance into the app shell.
4. Correct dialog exterior palette behavior for both dialog variants.
5. Run focused and platform verification, then capture available runtime theme and interaction evidence.
