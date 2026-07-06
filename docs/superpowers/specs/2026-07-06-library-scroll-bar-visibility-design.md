# Scroll Bar Visibility Design

## Context

`LibraryHomeScreen` has multiple `NowPlayingBar` render paths: the root fixed bar for Home/Search/Settings-style routes, and a drill-down bar inside album/artist track-list screens. The previous implementation only handled the main Library/Home list, but the required behavior is broader: every scrollable screen that has a `NowPlayingBar` should hide it on downward scrolling and reveal it on upward scrolling.

## Goals

- Hide `NowPlayingBar` when normal scroll direction moves downward into content on any scrollable screen that renders a bar.
- Show `NowPlayingBar` when normal scroll direction moves upward toward earlier content.
- Apply the behavior to the main Library/Home list, Search result list, and album/artist track-list screens.
- Animate the bar in and out from the bottom so the transition feels navigational rather than abrupt.
- Preserve existing tap-to-expand and upward-drag-to-expand behavior when the bar is visible.
- Preserve existing Now Playing expanded overlay, back handling, playback controls, search/settings navigation, scanner, library, and theme behavior.
- Avoid adding dependencies.

## Non-goals

- No native platform navigation migration.
- No new navigation library or gesture dependency.
- No change to Now Playing screen layout or playback semantics.
- No change to Android predictive back, visible back chips, left-edge swipe back, or route transition semantics.
- No persisted user setting for bar visibility.

## Design

### 1. Scope: all scrollable screens with NowPlayingBar

The visibility behavior is attached to the scrollable content that coexists with a `NowPlayingBar`:

- Home Library list: controls the root fixed bar.
- Search results list: controls the root fixed bar while Search is open.
- Album/artist track lists: control the local drill-down bar rendered by `DrillDownView`.

Settings currently has no scrollable list and does not need additional tracking, though the root fixed bar continues to respect the shared hoisted visibility state.

### 2. Scroll direction model

Use a small pure helper that converts list scroll positions into a visibility decision. Inputs are previous and current list positions represented as `(firstVisibleItemIndex, firstVisibleItemScrollOffset)`, plus current visibility. The helper computes scroll movement:

- If the current position is deeper than the previous position, the user moved down the list, so the bar should hide.
- If the current position is earlier than the previous position, the user moved up the list, so the bar should show.
- If movement is below a small pixel threshold, keep current visibility to prevent jitter from tiny offset changes.

This helper is testable in `commonTest` without Compose UI instrumentation.

### 3. Compose integration

In `LibraryHomeScreen`:

- Keep one hoisted `isNowPlayingBarVisible` boolean and one previous scroll position.
- Use a shared updater function that applies the pure helper and updates the previous position.
- Feed that updater from Home, Search results, and `DrillDownView` track-list `LazyListState` changes.
- Pass current visibility into `DrillDownView`, because it owns the local album/artist bar render path.
- Keep `NowPlayingBar` itself presentational; it only receives visibility through animated wrappers.

### 4. Bar animation

Render every `NowPlayingBar` path through an enter/exit animation at the bottom edge. The animation should slide vertically from/to the bottom and may fade in/out. When hidden, the bar should not intercept pointer input.

`NowPlayingBarContentPadding` should continue to protect list content from being hidden behind the bar. If implementation shows a persistent excessive empty gap while the bar is hidden, the padding can be animated or made conditional on visibility, but the first priority is preserving readable bottom content and avoiding layout jumps.

### 5. Gesture interaction

When the bar is visible:

- Tapping the bar still opens Now Playing.
- Dragging up on the bar still expands Now Playing via the existing `verticalSheetGesture`.
- Playback/search/settings buttons keep their current callbacks.

When the bar is hidden:

- The bar is off-screen/not visible and should not intercept touch.
- Scrolling up on the active list reveals it again.
- Now Playing expanded overlay behavior is unchanged if already open.

## Risks

- Scroll direction detection can flicker if it responds to very small offset changes; the helper should use a small threshold and tests should cover jitter.
- Sharing one previous scroll position across routes means a route switch seeds the next screen with the prior route position; the first tiny/no-op observation should not cause visible flicker because jitter is ignored, but manual validation is still useful.
- Hiding the fixed bar may expose the existing bottom spacer as a visual gap. The implementation should preserve content readability first and only adjust padding if the gap is clearly excessive.
- Compose `LazyListState` observation must avoid launching excessive coroutines or causing recomposition loops.
- Manual visual validation is still useful because automated tests can verify state decisions and compilation, not the feel of the animation.

## Acceptance criteria

- On the Library/Home list, scrolling or dragging down into the list hides the `NowPlayingBar`.
- On the Library/Home list, scrolling or dragging up toward earlier content shows the `NowPlayingBar`.
- On album/artist track-list screens, scrolling or dragging down hides the local `NowPlayingBar`.
- On album/artist track-list screens, scrolling or dragging up shows the local `NowPlayingBar`.
- On Search with results, result-list scrolling down hides the root fixed `NowPlayingBar`, and scrolling up shows it.
- Tiny scroll-position jitter does not toggle the bar.
- The hide/show transition is animated from the bottom rather than abrupt.
- Existing tap-to-expand, drag-up-to-expand, playback control, Search, Settings, route navigation, predictive/system back, and Now Playing overlay behavior are preserved.
- No new dependencies.
- Focused tests for scroll-direction visibility decisions pass.
- Relevant JVM/desktop/Android and iOS verification commands pass or blockers are recorded with exact output.
