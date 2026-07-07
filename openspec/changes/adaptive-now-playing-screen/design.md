# Design: Adaptive Now Playing Screen

## Overview

`NowPlayingScreen` will choose between compact and wide rendering based on the same adaptive thresholds used by the Library list/detail shell. Compact rendering keeps the existing vertical column. Wide rendering uses a local Compose `Row`: artwork/accent visual on the left, controls and metadata on the right.

## Adaptive policy

Add a small Now Playing helper rather than renaming the existing Library helper in this slice:

- `NowPlayingAdaptiveLayoutMode.Compact`
- `NowPlayingAdaptiveLayoutMode.Split`
- `nowPlayingAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float)`

The helper mirrors the current Library thresholds:

- wide/split at width >= 840dp;
- wide/split at width >= 600dp when height / width < 1.2;
- compact otherwise.

This makes Now Playing tests explicit while keeping the existing Library helper stable.

## Screen composition

`NowPlayingScreen(...)` keeps its public call shape and root responsibilities:

- root `Surface` fills the overlay;
- root keeps `leftEdgeSwipeBack(onBack)`;
- track status, duration, position, artwork bitmap, shuffle state, repeat content description, and shuffle content description are derived once and passed into private child composables.

Private presentational composables should split the repeated UI:

- `NowPlayingArtworkPane(...)` renders the artwork card or gradient/title fallback.
- `NowPlayingControlsPane(...)` renders title, artist/album, optional track number, status, scrubber, shuffle/repeat row, and transport controls.
- `CompactNowPlayingLayout(...)` preserves the current vertical layout and spacing.
- `WideNowPlayingLayout(...)` arranges artwork and controls side by side.

## Compact layout

Compact mode remains the current structure and spacing:

1. Safe content padding.
2. 20dp horizontal padding.
3. 18dp top spacer.
4. Full-width square artwork card.
5. Existing metadata, status, progress, playback mode, and transport controls.
6. Existing bottom spacer.

## Wide layout

Wide mode uses a local `Row` with two panes:

- artwork pane: left side, vertically centered, square, rounded, uses the same artwork or fallback gradient;
- controls pane: right side, vertically centered where space allows, uses the same callbacks/content descriptions as compact;
- no bottom `NowPlayingBar` appears inside the expanded screen;
- no queue/up-next pane is introduced.

## Verification

Focused verification should cover the new helper and compile the shared UI. Completion verification should include the standard JVM/desktop/Android/iOS commands unless an exact blocker is recorded.
