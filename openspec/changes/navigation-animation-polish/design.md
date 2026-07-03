# Design: Navigation Animation Polish

## 1. Predictive back on Android

Keep `NavigationEventInfo.None` as the info. The `NavigationEventState.transitionState` already exposes `NavigationEventTransitionState.InProgress(latestEvent, direction)` during a predictive back gesture. Extract `latestEvent.progress` (0f to 1f) from `transitionState` and drive a horizontal offset on the current route content so it slides leftward as the drag progresses. On completion, `onBackCompleted` pops the route. On cancel, `onBackCancelled` snaps the offset back to zero.

The manifest already has `android:enableOnBackInvokedCallback="true"`.

## 2. Bottom bar outside navigation animation

Restructure `LibraryHomeScreen` root layout as a `Box`:
1. `AnimatedContent` route wrapper (main content area), filling the screen.
2. `NowPlayingBar` at `Alignment.BottomCenter`, outside `AnimatedContent`.

The bottom bar stays fixed during all route transitions. State remains shared via the same `playbackState` and `selectedTrack`.

## 3. Bottom bar expand to Now Playing

Render Now Playing as an overlay outside `AnimatedContent`, at the same level as the bottom bar. Use an `Animatable<Float>` (0f = bar, 1f = full screen; expand `tween(300)`, collapse `tween(250)`) driven by the Now Playing route active state. Animate:
- Panel offset/height from compact bar to full screen.
- Corner radius from bar shape to square corners.
- Bar content fades out; Now Playing content fades in.

Use `Animatable` or `animateFloatAsState` driven by the Now Playing route's active state (0f = bar, 1f = full screen). On back, reverse the animation. Preserve `leftEdgeSwipeBack` and `BackChip` for closing.

## Verification

Automated:
- `openspec validate navigation-animation-polish --strict`
- Focused tests for any new pure helpers.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`

Manual:
- Android 13+ predictive back gesture visual progress.
- Bottom bar fixed during route transitions on all platforms.
- Bottom bar expand/collapse animation on all platforms.
Record that `NavigationEventInfo.Slide` does not exist in `navigationevent` 1.1.x; use `NavigationEventInfo.None` and read `navState.transitionState` for progress.