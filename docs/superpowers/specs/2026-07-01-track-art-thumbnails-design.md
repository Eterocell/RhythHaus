# Track Art Thumbnails Design

## Context

Scrolling down in the Songs/track list is still laggy. Source inspection points to artwork decode work on the scroll path: `TrackRow` renders `AlbumMark`, and `AlbumMark` currently calls `track.artworkBytes?.decodeArtwork()` for each composed row. That means full embedded artwork can be decoded for a 54dp row mark whenever lazy-list rows enter composition. Album/artist surfaces already use the existing cached helper, but track rows and the compact now-playing bar do not.

## Design

Add a small in-memory thumbnail decode path for compact artwork surfaces:

- Keep original `artworkBytes` on `LibraryTrack`, `Track`, and `PlayableTrack` unchanged.
- Keep `decodeArtwork()` as the full-size path for expanded artwork and platform media metadata.
- Add `decodeArtworkThumbnail(maxPixelSize: Int)` as an expect/actual platform helper.
- Add `decodeArtworkThumbnailCached(maxPixelSize: Int = 128)` in common code.
- Make `ArtworkCache` key entries by both artwork content hash and size bucket so full-size and thumbnail images do not overwrite each other.
- Use thumbnails in `AlbumMark` and `NowPlayingBar`.
- Keep `NowPlayingScreen` on full-size decode for detail quality.

## Platform details

Android should use `BitmapFactory.Options.inJustDecodeBounds` to read the embedded artwork dimensions, compute an integer `inSampleSize`, decode a smaller bitmap, and convert it with `asImageBitmap()`.

JVM/macOS and hosted iOS Compose should use Skia: decode the embedded image, draw it into a small raster `Surface` with linear sampling, snapshot it, and convert to Compose `ImageBitmap`.

If thumbnail decode fails, return null and let the existing gradient/text fallback render.

## Scope

In scope:

- Memory-only thumbnail cache.
- Compact track row and now-playing bar artwork decode path.
- Common cache tests and compile/build verification.

Out of scope:

- Persistent disk thumbnail cache.
- Scanner-time thumbnail generation.
- SQLDelight schema migration.
- TagLib/native metadata extraction changes.
- Native SwiftUI migration.
- Visual redesign.
- Claims about measured FPS improvement without runtime profiling.

## Verification

- Common tests for cache-key separation and thumbnail reuse.
- `openspec validate track-art-thumbnails --strict`.
- Focused JVM tests for the artwork cache.
- `./gradlew :shared:compileKotlinJvm --configuration-cache`.
- Broad JVM/desktop/Android verification.
- iOS simulator tests because expect/actual image code changes for Darwin targets.
