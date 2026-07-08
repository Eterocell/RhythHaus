# Album/Artist Track List Top-Bar Artwork Design

## Context

Album and artist drill-down screens currently use `DrillDownView` with a Miuix `TopAppBar` in `LibraryChrome.kt`. The app bar shows the album or artist title as both the expanded large title and the collapsed title, but it does not show the representative embedded artwork that is already available on album cards, artist rows, track rows, compact Now Playing, and platform media metadata through `Track.artworkBytes`.

## Goal

Show the album/artist representative artwork image in the album and artist track-list top bar while preserving the current Miuix title-collapse behavior, back navigation, glass chrome, list behavior, and Now Playing bar.

## Design

Use existing shared Compose artwork data only. The route layer will derive a representative artwork from the tracks already used by the selected album or artist: all tracks in the detail group with non-null `artworkBytes`, preserving order. `LibraryRouteContent` will pass this ordered candidate list to `DrillDownView`, and `DrillDownView` will pass it to `DrillDownMiuixScrollChrome`.

`DrillDownMiuixScrollChrome` will try candidate artwork through the existing memory-only thumbnail cache (`decodeArtworkThumbnailCached`) until the first decodable thumbnail is found, then render it as a compact action-side image inside the Miuix `TopAppBar` `actions` slot. The image should be clipped with rounded corners, crop-filled, and use the existing localized artwork content descriptions. When no candidate artwork exists or all decode attempts fail, the action slot remains empty; no alphabet placeholder is added to the top bar.

The expanded large title and collapsed title remain owned by Miuix `TopAppBar(title = title, largeTitle = title, scrollBehavior = scrollBehavior)`. The artwork is a supplementary visual affordance and must not replace the title or back button.

## Scope

In scope:
- album and artist drill-down track-list top bar only;
- shared Compose UI files under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui`;
- OpenSpec/Superpowers evidence files.

Out of scope:
- scanner, TagLib, SQLDelight schema, playback models, platform media metadata, and dependency changes;
- Library home album cards/artist rows/track rows visual redesign;
- new image cache or persistent thumbnail storage;
- Windows/Linux support.

## Acceptance Criteria

- Album detail top bar shows a representative embedded album artwork image when any track in the album has decodable artwork bytes.
- Artist detail top bar shows a representative embedded artwork image when any track in the artist group has decodable artwork bytes.
- If no artwork is available or decoding fails, the top bar keeps its current title/back behavior and does not show a letter placeholder.
- The Miuix scroll behavior, nested scroll connection, glass overlay, bottom divider, left-edge swipe back, track rows, and Now Playing bar remain unchanged.
- Artwork-filled top bars may use full cached artwork decoding because the image fills the top bar rather than a compact thumbnail surface.
- Focused JVM compilation and library UI tests pass; full platform verification is run or any blocker is recorded exactly.
