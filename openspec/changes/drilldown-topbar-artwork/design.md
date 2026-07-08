# Design: Drill-Down Top-Bar Artwork

## Overview

Album and artist detail routes already resolve their `AlbumGroup` or `ArtistGroup` and pass the grouped tracks to `DrillDownView`. The smallest safe change is to derive ordered artwork byte candidates at that route boundary and let the existing drill-down chrome render it.

## Data Flow

`LibraryRouteContent`:
1. Resolve album or artist group.
2. Read `tracks.mapNotNull { it.artworkBytes }`.
3. Pass the result as `topBarArtworkCandidates` to `DrillDownView`.

`DrillDownView`:
1. Preserve existing selected track and scroll behavior.
2. Forward `topBarArtworkCandidates` to `DrillDownMiuixScrollChrome`.

`DrillDownMiuixScrollChrome`:
1. Decode with `remember(topBarArtworkCandidates) { topBarArtworkCandidates.firstNotNullOfOrNull { it.decodeArtworkCached() } }`.
2. If non-null, render an `Image` in the Miuix `TopAppBar` `actions` slot.
3. If every candidate fails to decode, keep the existing glass title top bar and render no artwork placeholder.

## UI Behavior

The Miuix `TopAppBar` remains the owner of `title`, `largeTitle`, `scrollBehavior`, back navigation, and bottom divider. Artwork is supplementary action-side content. It is clipped to a small rounded rectangle and crop-filled, matching the app's existing artwork usage while avoiding a new placeholder state.

## Risks

- The action-side slot may visually crowd very long titles on narrow screens. Miuix already measures title width against action width, so the title should ellipsize rather than overlap.
- Manual visual QA is still needed to tune exact size/padding if the default Miuix action layout differs across platforms.

Scope correction 2026-07-08: “Show album image” means the image fills the drill-down top bar. When artwork is present, `DrillDownMiuixScrollChrome` must not apply the blur/glass surface to that top bar; it instead overlays a readability scrim, a circular back-button surface, and title chips for expanded/collapsed states.
