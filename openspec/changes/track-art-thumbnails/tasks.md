# Tasks

## 1. Thumbnail cache and decoders

- [ ] Add common cache-key support so full-size artwork and thumbnail entries are stored separately.
- [ ] Add `decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?` as an expect/actual helper.
- [ ] Add `decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap?` in common code.
- [ ] Cover cache-key separation and thumbnail cache reuse with common tests.
- [ ] Verify with focused JVM tests and JVM compilation.

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
