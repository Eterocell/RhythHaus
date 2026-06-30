# Music Progress Scrubber Design

Date: 2026-06-30
Route: openspec+superpowers
Status: approved design input

## User Request

Reimagine the now-playing progress slider so it behaves like a music-player scrubber instead of a generic value slider.

Current flaws:

1. A single click/tap cannot move playback directly to the selected destination.
2. Dragging/sliding causes playback to seek through multiple intermediate positions before the final destination.

## Current Context

RhythHaus renders the expanded now-playing screen in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`.

The current implementation uses Miuix `Slider`:

- `value = progressFraction`
- `onValueChange = { playbackController.seekTo((durationMillis * fraction).toLong()) }`

That makes every value-change event a real engine seek. This is unsuitable for audio playback because scrub gestures emit many intermediate values, causing multiple audible jumps and unnecessary backend work. It also inherits Miuix slider tap semantics, which currently do not satisfy the desired click-to-seek behavior.

RhythHaus uses shared Compose Multiplatform UI for Android, iOS-hosted Compose, and desktop/macOS. The change should stay in shared Compose common code and preserve the existing Haus visual language.

## Goals

- Replace the now-playing progress control with a music-focused scrubber.
- Support click/tap-to-seek: one pointer press at a track position seeks directly to that destination.
- Support drag preview: while dragging, the thumb and elapsed-time label preview the dragged position locally.
- Seek exactly once at the end of a drag, using the final destination.
- Prevent real playback progress updates from fighting the user's thumb while scrubbing.
- Preserve the existing playback controller API and platform engine API.
- Keep the implementation shared-first and testable.

## Non-goals

- Do not add new dependencies.
- Do not change platform playback engines.
- Do not add waveform rendering, buffered-range rendering, chapter markers, lyrics sync, haptics, or audio preview while dragging.
- Do not change mini progress behavior in `NowPlayingBar` beyond continuing to display passive progress.
- Do not change native iOS SwiftUI screens or platform-specific source files.
- Do not replace the whole now-playing layout, track metadata layout, or transport controls.
- Do not add Material/Material3 UI usage to new shared UI code.

## Design

### Interaction model

Use a custom shared Compose scrubber instead of Miuix `Slider` for the expanded now-playing screen.

Behavior:

- When the user taps/clicks a point on the scrubber rail, compute the fraction from the pointer x-position and call `playbackController.seekTo(targetMillis)` exactly once.
- When the user starts a drag, enter a local scrubbing state.
- During the drag, update a local preview fraction/position for the rail fill, thumb, and elapsed time label.
- During the drag, do not call `seekTo` for intermediate movement events.
- On drag release, call `seekTo(finalTargetMillis)` exactly once.
- On drag cancellation, leave playback unchanged and resume displaying real playback progress.
- While scrubbing, the UI displays the local preview position instead of incoming `playbackState.positionMillis` so progress updates cannot pull the thumb away from the pointer.
- After release/cancel, the UI returns to real playback state.

### Visual design

Create a small dedicated composable, tentatively `MusicProgressScrubber`, in shared common code.

Visual contract:

- Full-width touch target with at least 44 dp height.
- A slim rounded rail using `HausLine`.
- A filled rounded segment using `HausPulse`.
- A visible thumb/handle large enough to communicate draggability without overpowering the screen.
- Preserve the surrounding elapsed/duration labels and Haus typography.
- Keep the control visually calm and music-player-like; no decorative animation or extra icons.

### Testable logic

Extract pure mapping helpers so behavior can be covered without Compose UI gesture tests:

- Clamp a fraction to `0f..1f`.
- Convert a pointer x-position and track width to a fraction.
- Convert a fraction and duration to a target millisecond position.

The composable should use those helpers for tap and drag calculations. Tests should prove edge clamping and the single-seek contract through a small interaction-state helper where practical.

### API shape

`MusicProgressScrubber` should accept state and callbacks rather than owning playback:

- `positionMillis: Long`
- `durationMillis: Long`
- `onSeek: (Long) -> Unit`
- optional `modifier: Modifier = Modifier`

It may accept colors only if necessary, but the initial implementation should use the existing Haus palette directly to avoid unnecessary surface area.

`NowPlayingScreen` should compute `displayPositionMillis` from the scrubber's internal preview state by letting the scrubber own the elapsed label row, or by passing a preview callback only if needed. Prefer the smallest API that keeps the component understandable.

## Acceptance Criteria

- The expanded now-playing screen no longer uses Miuix `Slider` for progress seeking.
- A single click/tap on the progress rail calls `seekTo` once with the clicked destination.
- A drag updates the visible progress/thumb locally while dragging.
- A drag calls `seekTo` exactly once on release with the final destination.
- A canceled drag does not call `seekTo`.
- Incoming playback progress does not override the local preview while scrubbing.
- Position calculations clamp to the valid range `0..durationMillis`.
- No platform-specific source files are changed.
- No new dependencies are added.
- `NowPlayingBar` remains a passive mini progress indicator.
- The new UI keeps Haus colors and touch targets suitable for mobile and desktop pointer input.

## Verification Plan

Focused automated checks:

```bash
./gradlew :shared:jvmTest --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Full repository verification before completion:

```bash
./init.sh
```

Manual visual/interaction checks:

- Open the expanded now-playing screen.
- Tap near the middle of the progress rail and confirm playback jumps once to the tapped position.
- Drag the progress thumb and confirm the elapsed label/filled rail follow the pointer without audible intermediate jumps.
- Release after dragging and confirm playback jumps once to the final position.
- Cancel/abandon a drag if supported by the platform gesture and confirm no seek is sent.
- Confirm the mini now-playing bar still displays passive progress.

## Risks and Mitigations

- Compose Multiplatform pointer behavior may differ between Android, iOS-hosted Compose, and desktop. Mitigation: use foundation pointer input primitives in common code and keep pure math isolated in tests.
- A too-small rail could remain hard to hit. Mitigation: separate the visual rail from a taller 44 dp touch target.
- The UI could look inconsistent with Miuix components. Mitigation: use existing Haus color constants and typography, and keep the surrounding now-playing layout unchanged.
- Playback state may lag the final released seek briefly. Mitigation: keep local preview during the gesture and return to engine state after release; the controller/engine remains source of truth after the seek.
