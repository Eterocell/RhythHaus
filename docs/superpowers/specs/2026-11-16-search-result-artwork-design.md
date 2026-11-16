# Search Result Artwork Design

> Status: Design and implementation plan approved by the user on 2026-11-16; subagent-driven execution selected. This is a narrow follow-up to the active `studio-ui-redesign` change, not acceptance of the remaining Studio manual matrix.

## Intent

Search results currently show a neutral music note for every row, even when the `LibraryTrack` already contains embedded artwork. Render that artwork so listeners can visually identify tracks while scanning results.

## Current Behavior

`SearchResultRow` in `feature/search/.../SearchScreen.kt` always supplies `SearchNeutralArtwork()` to `HausSongRow`. `LibraryTrack.artworkBytes` carries embedded artwork, and the repository's `tracks()` projection includes it. Search already depends on `:core:ui`, whose `ArtworkImage` handles image decoding, caching, and a composable fallback.

## Approved Approach

Pass `track.artworkBytes` to `ArtworkImage` using the existing thumbnail role and preserve `SearchNeutralArtwork()` as the fallback for absent or undecodable bytes. Keep the artwork decorative to accessibility; the row's existing title, artist/album, selection, playback, and more-action semantics remain authoritative.

An artwork-loader callback was considered but rejected for this correction: Search already receives the embedded bytes, so a new public callback and Shared adapter would expand the feature boundary without addressing an additional requirement.

## Goals

- Display available embedded artwork in Search results.
- Preserve the neutral fallback for missing or invalid image data.
- Keep current row sizing, crop behavior, semantics, and interactions unchanged.
- Verify rendered output with a known-color image fixture, not source-wiring assertions alone.

## Non-Goals

- Add lazy artwork loading or a new Search API parameter.
- Change repository projections, artwork storage, cache policy, search ordering, selection, playback, or localized copy.
- Add dependencies or change platform-specific code.

## Constraints

- Use the existing `core:ui` `ArtworkImage` and `ArtworkImageRole.Thumbnail`.
- Keep Search independent of `feature:library:impl`.
- Keep the image decorative so it does not create a competing screen-reader stop.
- Preserve the row's existing 40dp desktop / 48dp compact artwork sizing from `HausSongRow`.

## Verification

Add Search JVM Compose tests that render a valid known-color PNG and assert the artwork pixels, then verify the neutral fallback for both missing and undecodable bytes. Run the focused regressions, the complete `:feature:search:jvmTest` suite, and `:desktopApp:compileKotlin`.
