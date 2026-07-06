# Design: Scroll Bar Visibility for Screens With NowPlayingBar

## Overview

Attach scroll-direction tracking to each scrollable list surface that can show a `NowPlayingBar`: the main Library/Home `LazyColumn`, Search results, and album/artist track-list drill-down screens. Use a single hoisted `isNowPlayingBarVisible` state in `LibraryHomeScreen`, and render each existing `NowPlayingBar` path through an animated bottom enter/exit wrapper. Keep `NowPlayingBar` itself presentational and preserve its existing tap and drag-up expansion behavior.

## Scroll direction decision

Add a pure helper that compares previous and current list scroll positions:

- A deeper current position means the user moved down the list, so the bar should hide.
- An earlier current position means the user moved up the list, so the bar should show.
- Movement below a small threshold keeps the current visibility to avoid jitter.

The helper should live in common source near the navigation/UI state helpers and be covered by common JVM tests.

## Compose wiring

In `LibraryHomeScreen`:

1. Keep `isNowPlayingBarVisible` and the previous scroll position hoisted at the screen owner level.
2. Share one local updater that converts list scroll positions into visibility decisions via the pure helper.
3. Feed the updater from:
   - the main Home `LazyColumn` state;
   - the Search results `LazyColumn` state when Search has results;
   - the album/artist `DrillDownView` track-list `LazyColumn` state.
4. Pass the current visibility into any child composable that renders its own `NowPlayingBar`.

The visibility state stays in `LibraryHomeScreen`, because it coordinates all bar render paths and should not be owned by `NowPlayingBar`.

## Animation

Wrap every `NowPlayingBar` render path in a bottom enter/exit animation. The bar should slide in from the bottom when becoming visible and slide out toward the bottom when hidden. Fade may be combined with the slide if it matches existing Compose animation style.

When hidden, the bar must not intercept pointer input. Existing `NowPlayingBarContentPadding` should remain unless implementation proves it creates an unacceptable empty gap; preserving readable list content near the bottom is more important than removing every pixel of space.

## Gesture and navigation preservation

The existing bar-specific upward drag (`verticalSheetGesture`) remains active only when the bar is visible. Tapping the bar still expands Now Playing. Search and Settings buttons keep routing through the existing callbacks. Now Playing overlay, back handling, playback state, route transitions, and platform-specific behavior are unchanged.

## Verification

- Add common tests for the pure scroll-direction visibility helper.
- Run focused helper/navigation tests.
- Run common JVM compile/test checks and relevant broad verification.
- Validate the OpenSpec change.
