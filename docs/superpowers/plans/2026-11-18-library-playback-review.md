# Library and playback review implementation plan

**Goal:** Fix the eight verified integration and scalability issues. **Spec:** [approved design](../specs/2026-11-18-library-playback-review-design.md). Execute independently reviewable changes against the real production boundary; final verification and migrations belong to the integration owner. Do not treat visual/device acceptance as automated evidence.

1. Gate the JVM task selector, desktop tests, Android unit tests and included convention tests in CI. Verify valid new translated Search keys, missing keys, duplicate owners and foreign imports through the resource audit.
2. Add the SQLDelight session schema and a transactional legacy import. Reopen a queue with 10,001 entries including duplicate tracks, then replace it with a small queue. Retry interrupted import without duplicates or loss.
3. Emit position-only progress and preserve structural writes across coalescing. Exercise progress followed by replacement, stale events, pause and flush; steady playback must not rewrite occurrences.
4. Cache directory entries and index `.lrc` siblings during Android SAF traversal. Count listings for mixed audio/non-audio files, nested names and optional-lyric failures.
5. Load stored covers through Shared into visible Search rows. Exercise the actual list query, loader and image renderer with valid, missing and corrupt payloads.
6. Add adjustable semantics and keyboard input to the existing scrubber. Verify bounds, absent duration, one seek per action and cancelled drag.
7. Give each responsive album row its own lazy list item. Verify bounded initial artwork requests, scrolling a distant release into view and resizing.
8. Add album artist in a separate migration; refresh old rows without repeatedly reading untagged files. Update Library routes/selection and CarPlay together. Exercise same-title albums, compilations, untagged fallback, order and smart-playlist isolation.

Run SQLDelight generation/migration checks, the affected behavior tests, supported platform compiles and strict OpenSpec validation on the integrated tree. Record the exact evidence and any unverified runtime paths in `progress.md`.
