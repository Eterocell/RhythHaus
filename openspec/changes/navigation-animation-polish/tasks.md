# Tasks

- [x] 1. Add predictive back visual progress on Android.
  - [x] Read `navState.transitionState` (`NavigationEventInfo.None` retained; `NavigationEventInfo.Slide` does not exist in navigationevent 1.1.x) and extract `InProgress.latestEvent.progress` when direction is `TRANSITIONING_BACK`.
  - [x] Feed progress into a horizontal offset applied to the `AnimatedContent` container so the current route tracks the drag.
  - [x] Cancel resets progress to 0 naturally (derived fresh from transitionState each recomposition); completion pops one route via existing `onBackCompleted`.
  - [x] Preserve iOS/desktop left-edge swipe and visible back behavior (unaffected; gesture pipeline is Android-only).

- [x] 2. Lift NowPlayingBar outside AnimatedContent.
  - [x] Restructured `LibraryHomeScreen` root as a `Box` with `AnimatedContent` (route content) plus `NowPlayingBar` as a fixed sibling.
  - [x] Bottom bar stays fixed during all route transitions.
  - [x] Shared playback/selection state preserved (same `playbackState`, `selectedTrack`, `snapshot.tracks`).

- [x] 3. Replace Now Playing route push with custom expand/collapse animation.
  - [x] Added `NowPlayingExpandOverlay` composable rendered outside `AnimatedContent`, anchored to the bottom via `Alignment.BottomCenter`.
  - [x] Animated panel growth (`fillMaxHeight(fraction)`), corner radius shrink, and content opacity via `Animatable<Float>` (expand `tween(300)`, collapse `tween(250)`).
  - [x] On back, `Animatable` animates back to 0, collapsing to bar shape.
  - [x] Preserved `leftEdgeSwipeBack` and `BackChip` inside `NowPlayingScreen` for closing (unchanged).

- [x] 4. Verify and record evidence.
  - [x] Ran `openspec validate navigation-animation-polish --strict`: pass.
  - [x] Ran focused `LibraryNavigationTest`: pass.
  - [x] Ran broad JVM/desktop/Android verification: pass.
  - [x] Ran iOS simulator tests: pass.
  - [x] Updated `progress.md` with exact commands, outcomes, changed files, risks, and next owner.