# Tasks

## 1. Thumbnail cache and decoders

- [x] Add common cache-key support so full-size artwork and thumbnail entries are stored separately.
  - Evidence: added `ArtworkCacheKey` with nullable size bucket and routed `ArtworkCache.get/put` through it.
- [x] Add `decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?` as an expect/actual helper.
  - Evidence: common expect plus Android BitmapFactory and JVM/iOS Skia actuals added.
- [x] Add `decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap?` in common code.
  - Evidence: common cached thumbnail helper added in `ArtworkDecoder.kt`.
- [x] Cover cache-key separation and thumbnail cache reuse with common tests.
  - Evidence: `ArtworkCacheTest` covers full-size vs thumbnail key separation, thumbnail-size key separation, empty-cache miss/size behavior, and rectangular thumbnail dimension bounds without initializing Skiko native image objects.
- [x] Verify with focused JVM tests and JVM compilation.
  - Evidence: after review fixes, `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache` passed; `./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain --configuration-cache` passed.

## 2. Route compact artwork surfaces through thumbnails

- [x] Change `AlbumMark`/`TrackRow` artwork rendering to use `decodeArtworkThumbnailCached()`.
  - Evidence: `AlbumMark` in `App.kt` now decodes compact row artwork via `decodeArtworkThumbnailCached()` while preserving selected-row overlay and fallback text.
- [x] Change `NowPlayingBar` compact artwork rendering to use `decodeArtworkThumbnailCached()`.
  - Evidence: compact `NowPlayingBar` artwork now decodes via `decodeArtworkThumbnailCached()` while preserving bar layout, controls, callbacks, icons, and progress rendering.
- [x] Keep `NowPlayingScreen` on full-size `decodeArtwork()`.
  - Evidence: source search confirms `NowPlayingScreen.kt` still calls `track.artworkBytes?.decodeArtwork()` for expanded artwork.
- [x] Confirm source search shows the intended direct/full decode call sites only.
  - Evidence: `rg 'decodeArtwork\(' ...` shows platform actual decoder implementations, `NowPlayingScreen`, the legacy `NowPlayingCard`, and common cached full-size helper; `rg 'decodeArtworkThumbnailCached' ...` shows `AlbumMark`, `NowPlayingBar`, and the helper.
- [x] Verify with focused JVM artwork-cache test and JVM compilation.
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache` passed; `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.

## 3. Handoff and commit

- [ ] Run `openspec validate track-art-thumbnails --strict`.
- [ ] Update `progress.md` with route, evidence, changed files, and remaining manual performance-validation note.
- [ ] Commit the completed OpenSpec + Superpowers workflow with a semantic message.
