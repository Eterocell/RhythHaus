## Context

Saved playlist detail currently renders every row as an always-editable control surface. That differs from the established album and artist track-list interaction, where a row is primarily a playback target. The playlist detail list also needs to participate in the same app-shell Bottom Bar behavior as the other library lists, including measured clearance for the bar's visible height. The shared dialog shells already resolve their panel and scrim from Haus palette values, but the dark-theme exterior contract must be explicit so that a dialog never presents a light-looking dark-mode backdrop.

This change is shared-first and limited to common Compose presentation/state policy and OpenSpec requirements. It does not change playlist persistence, schema, route shape, playback occurrence identity, or platform integrations.

## Goals / Non-Goals

**Goals:**

- Make the default saved-playlist detail row visually and behaviorally match the album/artist track-list row: artwork, title, artist/album metadata, duration, and click-to-play for the selected `PlaylistEntry` occurrence.
- Add a transient, page-wide edit mode entered by long-pressing any playlist row, with drag, move-up, move-down, and remove actions available for every editable row.
- Make edit-mode exit precedence explicit for taps outside the editable list and Back, including before route navigation.
- Preserve duplicate playlist entries as independently editable and playable occurrences by using `PlaylistEntry.id` as the identity/key for all row operations.
- Provide labeled move controls and destructive confirmations so reordering/removal do not depend on drag or color-only cues.
- Reuse the album/artist scroll-down hide and scroll-up reveal policy and the existing measured Bottom Bar clearance contract without introducing a playlist-specific scroll policy.
- Require both `HausDialog` and `HausLazyDialog` panel, exterior, and scrim colors to resolve visibly from the active light/dark Haus palette.

**Non-Goals:**

- Changing playlist storage, migration, repository semantics, route definitions, playback queue semantics, or duplicate-entry persistence.
- Adding new edit actions beyond reorder and removal.
- Redesigning the app shell, album/artist scroll behavior, Haus palette values, or platform-specific UI.

## Decisions

### Default row mode and occurrence playback

Playlist detail SHALL render a non-editing row using the established track-list presentation and metrics. A normal click SHALL create playback from the visible ordered playlist entries and select the clicked `PlaylistEntry.id`, so repeated references to the same library track remain distinct queue occurrences. Default rows SHALL not expose mutation controls.

Long-pressing any row SHALL activate one page-wide edit mode. Edit mode SHALL be represented as transient screen state, not persisted playlist state, and SHALL make the controls apply to all currently visible editable rows. Row identity, Compose keys, drag targets, move commands, remove confirmations, and stale-result reconciliation SHALL use `PlaylistEntry.id`, never only `trackId` or row index.

### Edit controls and exit precedence

Each editable row SHALL expose a drag handle, labeled move-up and move-down controls, and a labeled remove control. Move controls SHALL be disabled at the corresponding list boundary. Remove SHALL retain the existing confirmation requirement. Drag is an enhancement only: keyboard, switch-access, and assistive-technology users SHALL be able to complete reorder and removal through the labeled controls.

While edit mode is active, a tap outside the editable playlist list SHALL exit edit mode and consume that interaction rather than navigating. Back SHALL first exit edit mode; only a subsequent Back may invoke playlist navigation. This precedence applies before route pop/back-gesture completion as well as button/system Back handling.

### Shared playlist scroll/chrome contract

Playlist detail's scroll state SHALL feed the same existing scroll reducer/callback used by album and artist detail. Downward scrolling hides the active Bottom Bar and upward scrolling reveals it according to that shared policy. The list SHALL append the measured active Bottom Bar clearance supplied by the app shell, using the same content identity and measurement contract as album/artist, so the final row can scroll clear of the bar. No copied threshold, independent hide/reveal state, or hard-coded playlist-only clearance is permitted.

### Theme-aware dialog exterior

`HausDialog` and `HausLazyDialog` SHALL consume the active `HausColors.current` palette for their opaque panel and scrim/exterior. Light mode SHALL retain the restrained ink dim; dark mode SHALL use a dark palette-derived exterior rather than a light-looking backdrop. The panel SHALL remain visibly consistent with the active dark or light surface. Both dialog variants must implement the same policy.

## Risks / Trade-offs

- [Always-visible controls regress the familiar row experience] → Keep mutation controls absent until the explicit long-press transition.
- [Duplicate tracks target the wrong occurrence] → Use `PlaylistEntry.id` for keys and every edit/playback command; test duplicate IDs with distinct entry IDs.
- [Gesture-only editing excludes users] → Keep semantic move-up, move-down, and remove actions available in edit mode.
- [Playlist chrome diverges from album/artist] → Route playlist scroll events through the existing shared callback and pass the shell's measured clearance unchanged.
- [Dark dialogs still look light outside the panel] → Test both dialog variants against both active palette families and assert the dark exterior is palette-derived and not light-looking.

## Verification

- Pure common/JVM policy tests for default/edit row actions, duplicate `PlaylistEntry.id` identity, boundary availability, edit-mode Back/outside-tap precedence, and shared playlist scroll/clearance behavior.
- Focused dialog presentation tests for `HausDialog` and `HausLazyDialog` in light and dark palettes.
- `openspec validate playlist-edit-mode-bottom-bar-dialog-theme --strict`.
- Relevant shared JVM and platform compile/test gates after implementation.
