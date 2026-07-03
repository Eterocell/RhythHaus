# Navigation Animation Polish Design

## Context

The base navigation animation change added direction-aware `AnimatedContent` route transitions, a pure transition classifier, and in-window Clear Library dialog. Three polish items remain:

1. Android predictive back should show visual progress during the gesture, not just pop on completion.
2. The bottom bar should stay fixed during route transition animations instead of sliding/fading with the route content.
3. The bottom bar should expand into the Now Playing screen with a growth/expand animation, not fade/slide as a separate route push.

## Goals

- Provide predictive back visual progress on Android: the incoming route tracks the gesture drag; completing the gesture commits the pop; cancelling snaps back.
- Lift `NowPlayingBar` outside the `AnimatedContent` route wrapper so it remains fixed during all route transitions.
- Replace the current route-push expand from the bottom bar with a custom expand/collapse animation that visually grows the Now Playing screen from the bottom bar position.
- Preserve existing playback, scanner, library, theme, search, settings, back navigation, and iOS/desktop left-edge swipe-back behavior.
- Avoid adding dependencies.

## Non-goals

- No `SharedTransitionScope`/`SharedTransitionLayout` adoption (experimental API risk across platforms).
- No native platform navigation migration.
- No navigation-library adoption.
- No deep links or saved-state restoration.
- No screen content redesign beyond the expand/collapse mechanism.
- No changes to Now Playing screen content layout beyond receiving the expand animation wrapper.

## Design

### 1. Predictive back on Android

Current: `NavigationBackHandler` uses `NavigationEventInfo.None`. The handler pops the route on completion but ignores the gesture's intermediate progress, so there is no visual tracking during the drag.

Proposed: The `NavigationEventState` already exposes `transitionState: NavigationEventTransitionState` which is `Idle` or `InProgress(latestEvent, direction)`. During a predictive back gesture, `InProgress.latestEvent.progress` ranges from 0f to 1f, and `direction` is `TRANSITIONING_BACK`.

Collect `navState.transitionState` as Compose state. When it is `InProgress` with `TRANSITIONING_BACK`, extract `latestEvent.progress` and drive a horizontal offset on the current route content so it slides leftward as the drag progresses. The incoming (previous) route should be partially visible behind it. When the gesture completes, `onBackCompleted` pops the route. When cancelled, `onBackCancelled` resets the offset to zero and snaps back.

Implementation notes:
- Keep `NavigationEventInfo.None` as the info; the `NavigationEventInfo` is for destination identity, not animation type. `NavigationEventInfo.Slide` does not exist in the library.
- `navState.transitionState` is already observable in Compose because `NavigationEventState` uses `mutableStateOf`.
- The manifest already has `android:enableOnBackInvokedCallback="true"`.
- iOS/desktop are unaffected since the predictive back gesture pipeline is not active there.

### 2. Bottom bar outside navigation animation

Current: `NowPlayingBar` is rendered inside the Home/Settings/Search/ClearLibraryDialog branch of the `AnimatedContent`, so it slides/fades with route changes.

Proposed: Restructure `LibraryHomeScreen` so the root layout is a `Box` containing:
1. The `AnimatedContent` route wrapper (main content area), filling the screen.
2. The `NowPlayingBar` positioned at `Alignment.BottomCenter`, outside `AnimatedContent`.

The bottom bar remains fixed during all route transitions. Its content (track info, progress, play/pause, search/settings buttons) continues to update from the same shared `playbackState` and `selectedTrack` state.

The bottom bar's visibility should remain as-is: it is always visible at the bottom. The main content area's bottom padding (`NowPlayingBarContentPadding`) stays so content is not hidden behind the bar.

### 3. Bottom bar expand to Now Playing

Current: Tapping the bottom bar pushes `LibraryRoute.NowPlaying` as a route, so the full Now Playing screen enters via the standard `AnimatedContent` slide/fade transition.

Proposed: Replace the route-push expand with a custom overlay animation:
- Keep `LibraryRoute.NowPlaying` as a route for back-handling purposes but render it as an overlay on top of the bottom bar instead of inside `AnimatedContent`.
- When the Now Playing route is active, render an expandable panel that grows from the bottom bar position to fill the screen. Animate:
  - Panel offset from the bar's bottom position to fill the screen from top.
  - Corner radius from the bar's rounded shape to full-screen square corners.
  - Content opacity: the bar's compact content fades out while the Now Playing screen content fades in.
- On back (pop), reverse: the panel shrinks back to the bar shape, Now Playing content fades out, bar content fades in.
- Use an `Animatable<Float>` driven by the Now Playing route's active state to control the expand/collapse fraction (0f = bar, 1f = full screen). Animate expand with `tween(300)` and collapse with `tween(250)`.
- Preserve the existing `leftEdgeSwipeBack` gesture and visible `BackChip` on the Now Playing screen for closing.

The overlay should be rendered outside the `AnimatedContent` route wrapper, at the same level as the bottom bar, so it can grow from the bar's position without being clipped by the route animation.

When Now Playing is active and the expand animation reaches 1f, the bottom bar is hidden behind the full-screen panel. When collapsing, the bar fades back in as the panel shrinks.

## Risks

- `NavigationEventInfo.Slide` progress behavior may differ from expectation; manual Android 13+ device validation is needed.
- Lifting the bottom bar outside `AnimatedContent` changes the composition tree; ensure state hoisting and `remember` keys are correct so playback state is not lost.
- The custom expand animation is more complex than a route push; timing and clipping need manual visual validation on all platforms.

## Acceptance criteria

- On Android 13+, a nested route's predictive back gesture shows the incoming route tracking the drag; completing pops one route; cancelling snaps back.
- `NowPlayingBar` stays fixed at the bottom during all route transition animations.
- Tapping the bottom bar expands to the Now Playing screen with a growth animation from the bar; closing collapses back to the bar shape.
- No new dependencies.
- Existing back navigation, left-edge swipe, playback, scanner, library, and theme behavior preserved.
- Focused tests and broad JVM/desktop/Android/iOS verification pass.