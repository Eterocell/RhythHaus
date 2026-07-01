# Proposal: Track Art Thumbnails

## Summary

Reduce track-list scroll lag by avoiding repeated full-size embedded artwork decodes in list rows. Add a cached thumbnail decode path for small artwork surfaces and keep full-size artwork for expanded Now Playing surfaces.

## Problem

Source inspection shows the likely performance hotspot is artwork decoding during composition:

- `TrackRow` renders `AlbumMark`, which calls `track.artworkBytes?.decodeArtwork()` for every visible row.
- `NowPlayingBar` also decodes the selected track artwork directly.
- Album and artist surfaces already use `decodeArtworkCached()`, but track rows do not.
- Embedded artwork can be much larger than the 54dp row mark, so decoding the full image while scrolling wastes CPU/memory and can jank Compose list scrolling.

## Goals

- Add a small cached thumbnail decode path for list-sized artwork.
- Use thumbnails for track rows and the compact now-playing bar.
- Preserve full-size decode for the expanded now-playing screen.
- Keep artwork bytes available for platform media metadata and full-screen artwork.
- Avoid new dependencies and avoid database/schema changes.

## Non-goals

- No persistent on-disk thumbnail cache in this change.
- No scanner/TagLib/native metadata extraction changes.
- No SQLDelight schema migration.
- No native SwiftUI migration.
- No visual redesign of track rows, album cards, artist rows, or now-playing surfaces.
- No live FPS/device-performance claim without runtime profiling evidence.
