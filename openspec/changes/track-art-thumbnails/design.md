# Design: Track Art Thumbnail Decoding

## Root-cause evidence

The lag report is consistent with full artwork decoding on the scroll path. Current code decodes row artwork inside `AlbumMark` with `remember(track.artworkBytes) { track.artworkBytes?.decodeArtwork() }`. That avoids repeat decoding only while the same item remains composed; when rows leave/re-enter the lazy list, full embedded images can be decoded again. The existing `ArtworkCache` and `decodeArtworkCached()` helper already solve part of this for album and artist surfaces, but they are not used by `TrackRow`/`AlbumMark` or `NowPlayingBar`.

## Decision

Add a thumbnail-specific cached decode helper and route small UI surfaces through it:

- `ArtworkCache` stores images by content hash plus requested size bucket, so full-size and thumbnail entries do not collide.
- `ByteArray.decodeArtworkThumbnailCached(maxPixelSize: Int = 128)` returns a bounded-size `ImageBitmap` suitable for row thumbnails and compact now-playing art.
- `AlbumMark` uses thumbnail decode for `TrackRow` artwork.
- `NowPlayingBar` uses thumbnail decode because its artwork surface is compact.
- `NowPlayingScreen` keeps full-size `decodeArtwork()` so expanded artwork does not lose detail.
- Album and artist cards may continue using the existing cached decode path in this change because they are less likely to decode per-row during fast song-list scrolling; converting them can be a later measured optimization.

## Implementation approach

For Android, use `BitmapFactory.Options.inJustDecodeBounds` to read dimensions, calculate an integer sample size, then decode with `inSampleSize` before converting to Compose `ImageBitmap`. This prevents allocating a full-resolution bitmap for row artwork.

For iOS/JVM, use Skia to decode the embedded image, draw it into a small raster `Surface` with `SamplingMode.LINEAR`, snapshot the surface, and convert that thumbnail image to Compose `ImageBitmap`. If thumbnail generation fails, return null so existing fallback text remains.

Common tests should cover deterministic cache behavior without needing platform image fixtures: full-size and thumbnail entries use distinct cache keys; repeated thumbnail puts/gets reuse the same bucket; clearing cache resets state. Platform compile/tests prove actual thumbnail decoders resolve on supported targets.

## Risks and trade-offs

- An in-memory thumbnail cache improves scrolling after first decode but is not persistent across app launches. This is intentional for a small, low-risk fix.
- Content-hash keys can collide in theory. This matches the existing cache behavior and remains acceptable for this scoped change.
- Thumbnail generation still costs one decode the first time a track artwork is encountered. Persisted scan-time thumbnails may be considered later if runtime profiling still shows lag.

## Verification

- Add common tests for artwork cache key separation and thumbnail cache lookup.
- Run focused common JVM tests for the artwork cache.
- Run `./gradlew :shared:compileKotlinJvm --configuration-cache`.
- Run broad JVM/desktop/Android verification before completion.
- Run iOS simulator tests if shared expect/actual thumbnail code changes compile successfully for Darwin targets.
