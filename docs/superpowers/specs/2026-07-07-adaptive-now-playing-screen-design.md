# Adaptive Now Playing Screen Design

## Summary

RhythHaus will make the expanded Now Playing screen adaptive on tablet, desktop, and other wide windows while preserving the existing compact phone layout. Compact mode keeps the current vertical full-screen Now Playing UI. Wide mode uses a two-pane layout: large artwork/accent visual on the left and track metadata, progress, playback modes, and transport controls on the right.

## Current context

- `NowPlayingScreen.kt` currently renders one vertical column inside a full-screen `Surface`.
- `NowPlayingExpandOverlay` in `App.kt` owns the sheet-style expansion/collapse animation and calls `NowPlayingScreen(...)` when the bottom bar expands.
- Library adaptive layout already uses local width/height thresholds through `libraryAdaptiveLayoutModeFor(widthDp, heightDp)`:
  - compact below 840dp unless medium landscape applies;
  - list-detail at width >= 840dp;
  - list-detail at width >= 600dp when height / width < 1.2.
- The project intentionally avoids `miuix-navigation3-adaptive`; wide shells are implemented locally with Compose Row/Box.

## Goals

- Add a wide Now Playing layout using option A: artwork/accent visual pane on the left, metadata/progress/controls pane on the right.
- Preserve current compact phone Now Playing visual structure and behavior.
- Preserve sheet expansion, drag-to-collapse, left-edge swipe back, system/predictive back dismissal, playback controls, shuffle/repeat controls, seek behavior, artwork fallback, and current strings/content descriptions.
- Reuse the same adaptive threshold policy as the Library wide layout.
- Keep implementation in shared Compose common code.

## Non-goals

- No queue/up-next pane in this change.
- No redesign of Now Playing controls, icons, playback semantics, media engine, or metadata model.
- No Navigation3/adaptive dependency.
- No platform-specific native Now Playing UI.
- No artwork decoding/cache overhaul.

## Design

### Adaptive mode helper

Add a small Now Playing-specific helper around the existing threshold policy:

- `NowPlayingAdaptiveLayoutMode { Compact, Split }`
- `nowPlayingAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float): NowPlayingAdaptiveLayoutMode`

The helper should intentionally mirror the existing Library thresholds so future tweaks can be tested explicitly for Now Playing without forcing a generic rename of the Library helper in this slice.

### Compact layout

Compact mode preserves the current `NowPlayingScreen` layout:

1. Safe content padding and horizontal 20dp padding.
2. Artwork card full width with square aspect ratio.
3. Track title, artist/album, optional track number.
4. Status label.
5. Progress scrubber.
6. Shuffle/repeat row.
7. Previous/play-pause/next transport row.

No compact spacing/control behavior should intentionally change.

### Wide split layout

Wide mode uses one local `Row` inside the existing `Surface`:

- Left artwork pane:
  - uses the same artwork bitmap or gradient/title fallback as compact;
  - is vertically centered;
  - constrains artwork to a square size that fits the pane instead of stretching to full window height;
  - keeps rounded corners and existing visual tone.
- Right controls pane:
  - uses a `Column` centered vertically where possible;
  - contains the same title/metadata/status/progress/mode/transport content as compact;
  - uses wider text line limits where appropriate but keeps truncation for long metadata;
  - remains reachable and readable on medium landscape windows.

The screen should keep `leftEdgeSwipeBack(onBack)` at the root so compact and wide layouts dismiss the overlay the same way.

### Code organization

Refactor `NowPlayingScreen.kt` locally to avoid duplicating control logic:

- Keep public `NowPlayingScreen(...)` signature unchanged unless adding optional internal parameters is required for testing.
- Extract private presentational composables such as:
  - `NowPlayingArtworkPane(...)`
  - `NowPlayingControlsPane(...)`
  - `CompactNowPlayingLayout(...)`
  - `WideNowPlayingLayout(...)`
- Keep playback actions wired to the existing `PlaybackController` methods.
- Keep existing `PlaybackModeButton` and `statusLabel` behavior.

## Acceptance criteria

- Compact width renders the same vertical Now Playing structure as today.
- Wide width renders artwork/accent visual on the left and metadata/progress/playback controls on the right.
- Existing expanded overlay animation and dismiss gestures still route through `NowPlayingExpandOverlay` and `onBack`.
- Shuffle, repeat, seek, previous, play/pause, next controls keep the same callbacks and content descriptions.
- No queue/up-next UI is introduced.
- No `miuix-navigation3-adaptive` or Navigation3 adaptive dependency is introduced.
- Focused tests cover the Now Playing adaptive threshold helper.
- Common/JVM compile and existing focused navigation tests pass; Android debug assembly and iOS simulator tests are run before completion unless a blocker is recorded.

## Risks and mitigations

- `NowPlayingScreen.kt` may become repetitive if compact and wide layouts duplicate markup. Mitigation: extract artwork and controls panes before adding the split layout.
- Wide landscape height may be tight. Mitigation: keep the controls pane vertically centered but allow reasonable scrolling only if implementation proves content can overflow; do not add scrolling preemptively if compile/runtime layout remains safe.
- Artwork decode currently happens directly in `NowPlayingScreen`. Mitigation: preserve current behavior and do not broaden into cache/artwork pipeline work in this slice.
