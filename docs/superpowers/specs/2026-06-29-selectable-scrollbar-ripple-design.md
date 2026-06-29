# Selectable Scrollbar and Ripple Feedback Design

Date: 2026-06-29
Route: openspec+superpowers
Status: approved design input

## User Request

UI/UX changes:

1. The drill-down scrollbar is too thin to select.
2. Main visible controls and lists should show ripple effects when clicked.

## Current Context

RhythHaus uses shared Compose Multiplatform UI for Android, iOS-hosted Compose, and desktop/macOS. The relevant shared UI files are under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.

Current drill-down album/artist screens in `App.kt` render a custom scroll indicator beside a `LazyColumn`. The indicator is visual-only: a `BoxWithConstraints` aligned to the right edge, `3.dp` wide, with a moving thumb based on `firstVisibleItemIndex`. It cannot be tapped or dragged, so it is difficult to select.

Main visible clickables are mostly custom styled surfaces using `Modifier.clickable` after `clip` / `background` / `border`, so press feedback is inconsistent or absent. Existing invisible overlay blockers in `SearchScreen` and `SettingsScreen` intentionally use `.clickable(enabled = false, onClick = {})` only to absorb touches and must not show ripple.

## Goals

- Make the drill-down scrollbar easier to hit and select.
- Allow tapping or dragging the right-edge scroll area to move the album/artist track list proportionally.
- Add visible ripple/press feedback to main custom clickable controls and list/card surfaces.
- Keep the visual style consistent with the current high-contrast Haus palette.
- Keep the change shared Compose only.

## Non-goals

- Do not add dependencies.
- Do not change native iOS SwiftUI wrapper navigation or platform-specific files.
- Do not replace Miuix `Button` or `Slider` behavior.
- Do not add ripple to invisible overlay touch blockers.
- Do not redesign spacing, typography, colors, or navigation.
- Do not change list ordering, playback behavior, library scanning, or media controls logic.

## Design

### Scrollbar

Replace the `3.dp` visual-only drill-down scroll indicator with a right-edge scroll scrubber:

- Use a wider hit area of `24.dp` aligned to the right edge of the drill-down list container.
- Draw a narrower visible thumb inside that hit area, approximately `6.dp` wide, so the UI still looks lightweight while the touch target is usable.
- Preserve the current vertical placement and thumb-height concept: the thumb remains proportional enough to indicate scroll position and travels across the available height.
- Support both tap and vertical drag inside the hit area.
- Map the pointer y-position to a target list index using the current `LazyListState.layoutInfo.totalItemsCount` and visible item count.
- Use `rememberCoroutineScope` and `listState.animateScrollToItem(targetIndex)` for tap/drag jumps so list movement is smooth.
- Keep the scrubber attached only to album/artist drill-down screens, where the current custom indicator exists.
- Avoid interfering with the new left-edge swipe-back gesture by keeping this interaction on the right edge only.

### Ripple Feedback

Add a shared helper for visible custom clickables, tentatively named `Modifier.hausClickable(...)`.

The helper should:

- Wrap Compose foundation clickable with a remembered `MutableInteractionSource`.
- Use a bounded ripple with a subtle Haus-colored indication, such as `HausInk.copy(alpha = 0.14f)` on light surfaces and the same bounded ripple for dark chips/buttons unless implementation finds the contrast too low.
- Keep existing semantics and content descriptions at call sites.
- Keep existing shape clipping at call sites, so ripples stay within the current rounded/circular visual bounds.
- Leave invisible overlay blockers unchanged.

Apply ripple feedback to main visible custom clickable components/lists:

- Drill-down back chip in `App.kt`.
- `TrackRow` rows in `App.kt`.
- `AlbumCard` cards in `App.kt`.
- `ArtistRow` rows in `App.kt`.
- Expand/collapse developer panel header if still visible and custom clickable.
- `NowPlayingBar` surface and its visible search/settings/play-pause controls.
- Full `NowPlayingScreen` back chip and transport control boxes.
- `SearchScreen` visible back chip and result rows.
- `SettingsScreen` visible back chip.

Do not apply the helper to:

- `SearchScreen` and `SettingsScreen` root overlay blockers using `.clickable(enabled = false, onClick = {})`.
- Miuix `Button` instances in `SettingsScreen` and `BrowseModePicker`.
- Miuix `Slider` in `NowPlayingScreen`.
- Non-clickable artwork, labels, progress indicators, or decorative elements.

## Acceptance Criteria

- Album/artist drill-down scrollbar has a visibly similar thumb but a wider right-edge hit area that can be selected by tapping or dragging.
- Tapping or dragging the scrollbar area scrolls the drill-down list to a proportional position.
- Main visible custom clickable list/card/control surfaces show ripple feedback while preserving current shapes and colors.
- Invisible overlay blockers remain behavior-only and show no ripple.
- No iOS Swift files, platform-specific sources, dependency files, or Material/Miuix dependency versions are changed.
- `./gradlew :shared:compileKotlinJvm --configuration-cache` succeeds.

## Risks and Mitigations

- Dragging the scrollbar could conflict with vertical list scrolling. Mitigation: keep the scrubber hit area only on the far right edge and avoid attaching pointer input to the full list.
- Ripple contrast could be too strong on dark controls. Mitigation: use a subtle bounded Haus-colored ripple and keep existing background colors unchanged.
- Some clickable surfaces are Miuix components that already own interaction styling. Mitigation: apply the helper only to custom `Modifier.clickable` surfaces.
- Existing local edits in `progress.md` and `App.kt` may be present. Mitigation: inspect `git status --short` before implementation, stage only task files, and preserve unrelated hunks.

## Verification Plan

Focused compile verification:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Manual visual checks after implementation:

- In an album or artist drill-down list, tap the lower half of the right-edge scrollbar target and confirm the list scrolls downward.
- Drag inside the right-edge scrollbar target and confirm the thumb and list move proportionally.
- Tap visible rows/cards/buttons and confirm ripple feedback appears without layout shifts.
- Open search/settings overlays and confirm the overlay touch blocker still prevents background clicks without showing a ripple.
