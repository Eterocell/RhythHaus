# Swipe Gestures for Now Playing Sheet

**Date:** 2026-07-03
**Status:** Approved

## Overview

Add interactive swipe gestures to open and close the Now Playing detail screen:

- **Swipe down** on the Now Playing detail overlay to close it (dismiss)
- **Swipe up** on the Now Playing mini-player bar to open the detail screen

Both gestures are *interactive*: the overlay tracks the user's finger during the drag, snapping to open or closed when the finger is released past a threshold.

## Motivation

Currently the Now Playing detail screen is opened by tapping the mini-player bar, and closed via the back button or left-edge swipe. Adding vertical swipe gestures makes the experience feel more natural and fluid, matching the standard music-app pattern (Apple Music, Spotify).

## Design

### Architecture

A new `Modifier.verticalSheetGesture()` modifier — sibling to the existing `SwipeBackGesture.kt` pattern — that can be applied to both the expand overlay and the mini-player bar. One shared `Animatable<Float>` (0.0 = closed/collapsed, 1.0 = fully open/expanded) drives both the gesture tracking and the overlay's visual height.

### Gesture Mechanics

- **State:** single `Animatable<Float>` shared between overlay and bar, value in [0, 1]
- **Drag-down from open (overlay):** finger drags downward, decreasing the fraction
  - On release: if fraction < 0.7 → snap to 0 (close) and invoke `onBack`
  - Otherwise → spring back to 1 (stay open)
- **Drag-up from closed (bar):** finger drags upward, increasing the fraction
  - On release: if fraction > 0.3 → snap to 1 (open) and invoke `onExpand`
  - Otherwise → spring back to 0 (stay closed)
- **Rubber-banding:** drag past 0 or 1 applies resistance (half-speed beyond bounds), preventing a "hard stop" feel
- **Tap on bar:** the existing `hausClickable → onExpand` remains functional

### Files

| Action | File | Description |
|--------|------|-------------|
| **Create** | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/VerticalSheetGesture.kt` | New gesture modifier |
| **Modify** | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` | Wire gesture into overlay + bar; share expandProgress |
| **Modify** | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt` | Remove unused `verticalScroll` |

### VerticalSheetGesture.kt

A standalone modifier following the same pattern as `SwipeBackGesture.kt`:

```kotlin
fun Modifier.verticalSheetGesture(
    expandProgress: Animatable<Float, AnimationVector1D>,
    isOpen: Boolean,
    onSwipeExpand: () -> Unit,
    onSwipeCollapse: () -> Unit,
): Modifier
```

- Uses `detectVerticalDragGestures`
- On drag: updates `expandProgress` to track finger position
- On drag end: snaps to 0 or 1 based on thresholds
- Respects `isOpen` to determine whether this gesture is active (bar gesture active only when closed; overlay gesture active only when open)
- Rubber-banding applied when dragging beyond [0, 1]

### Changes in App.kt

**NowPlayingExpandOverlay:**
- Accept `expandProgress` as a parameter instead of owning it privately
- Apply `verticalSheetGesture` to the overlay surface with `isOpen = isVisible`
- On swipe-down past threshold → `onBack()`
- Remove the private `Animatable` initialization

**NowPlayingBar:**
- Accept `expandProgress` as a parameter
- Apply `verticalSheetGesture` to the bar surface with `isOpen = false` (bar only interactive when closed)
- On swipe-up past threshold → `onExpand()`

**LibraryHomeScreen:**
- Own the shared `Animatable<Float>` (lives outside both components, inside `LibraryHomeScreen`)
- Manage `LaunchedEffect(isVisible)` that animates expandProgress when navigation state changes (not via gesture)
- Pass expandProgress to both `NowPlayingExpandOverlay` and `NowPlayingBar`

### Changes in NowPlayingScreen.kt

- Remove `verticalScroll(rememberScrollState())` from the main `Column` modifier
- Keep `leftEdgeSwipeBack` modifier
- No other changes

### Transition Management

When navigation state toggles `LibraryRoute.NowPlaying` programmatically (e.g., tap on bar, back button press), the `LaunchedEffect` animates `expandProgress` to the target (0 or 1). When the user is mid-drag and a navigation event fires, the gesture is cancelled and the animation takes over. This is achieved by canceling the gesture coroutine when `isVisible` changes.

## Non-Goals

- No handle/pill drag indicator at the top of the sheet
- No partial-open/intermediate states
- No two-finger or multi-touch gestures
- No scrollable content in NowPlayingScreen (the `verticalScroll` is removed as it was only for a debug panel no longer in use)

## Testing

- Unit test: `verticalSheetGesture` modifier with thresholds, rubber-banding, and snap behavior (using fake `Animatable`)
- Integration: verify that dragging past threshold triggers the correct callback
- Manual: verify on all three platforms (Android, iOS, macOS) that swipe gestures feel natural and don't conflict with other interactions

## Acceptance Criteria

1. Swiping down on the Now Playing detail overlay with enough distance (past 0.7 threshold) closes it
2. Swiping down a small amount and releasing springs back to open
3. Swiping up on the mini-player bar with enough distance (past 0.3 threshold) opens the detail screen
4. Swiping up a small amount and releasing springs back to closed
5. Rubber-banding resistance when dragging beyond the open/closed extremes
6. Tap on the bar still opens the detail screen
7. Left-edge swipe back still closes the detail screen
8. Back button still closes the detail screen
9. No scrollable content in NowPlayingScreen (verticalScroll removed)
10. Works on Android, iOS, and macOS desktop
