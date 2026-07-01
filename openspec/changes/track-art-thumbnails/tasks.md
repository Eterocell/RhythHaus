# Tasks

## 1. Thumbnail cache and decoders

- [x] Add common cache-key support so full-size artwork and thumbnail entries are stored separately.
  - Evidence: added `ArtworkCacheKey` with nullable size bucket and routed `ArtworkCache.get/put` through it.
- [x] Add `decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?` as an expect/actual helper.
  - Evidence: common expect plus Android BitmapFactory and JVM/iOS Skia actuals added.
- [x] Add `decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap?` in common code.
  - Evidence: common cached thumbnail helper added in `ArtworkDecoder.kt`.
- [x] Cover cache-key separation and thumbnail cache reuse with common tests.
  - Evidence: `ArtworkCacheTest` covers full-size vs thumbnail and thumbnail-size key separation without initializing Skiko native image objects.
- [x] Verify with focused JVM tests and JVM compilation.
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache` passed; `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.

## 2. Route compact artwork surfaces through thumbnails

- [ ] Change `AlbumMark`/`TrackRow` artwork rendering to use `decodeArtworkThumbnailCached()`.
- [ ] Change `NowPlayingBar` compact artwork rendering to use `decodeArtworkThumbnailCached()`.
- [ ] Keep `NowPlayingScreen` on full-size `decodeArtwork()`.
- [ ] Confirm source search shows the intended direct/full decode call sites only.
- [ ] Verify with broad JVM/desktop/Android build/test command and iOS simulator tests.

## 3. Handoff and commit

- [ ] Run `openspec validate track-art-thumbnails --strict`.
- [ ] Update `progress.md` with route, evidence, changed files, and remaining manual performance-validation note.
- [ ] Commit the completed OpenSpec + Superpowers workflow with a semantic message.
