# Tasks

## 1. Embedded lyrics extraction

- [x] 1.1 Add failing native/tag-reader regressions proving `LYRICS` is returned through JVM, Android host, and iOS metadata ABI.
- [x] 1.2 Implement bounded normalized embedded lyric mapping through TagLib and `AudioMetadata`, preserving existing metadata behavior on unsupported or malformed fields.
- [x] 1.3 Run TagLib JVM and Android-host tests and iOS framework/test-code compilation; native CTest did not have a separately exercised lyrics case, so native fixture coverage is through the real JVM TagLib reader.

## 2. Sidecar scan selection

- [x] 2.1 Add scanner regressions for same-stem case-insensitive `.lrc` precedence, blank/oversized fallback, sidecar scan filtering, and source-local identity retention.
- [x] 2.2 Add an optional bounded sidecar-text projection to scan candidates; implement exact-sibling reading for JVM, iOS app-local, and Android SAF sources, excluding Android MediaStore.
- [x] 2.3 Ensure every readable candidate refreshes lyrics on explicit rescan even when audio size/modification facts are unchanged; keep optional sidecar failures from failing or skipping audio.
- [x] 2.4 Run focused scanner, platform source, and metadata tests; review the access/identity boundary.

## 3. Persistent lyrics field

- [x] 3.1 Add nullable lyrics to SQLDelight track schema, upsert and all track projections, with migration 9 and current schema version 10 legacy bootstrap.
- [x] 3.2 Add database lifecycle tests proving migration preserves existing track/user state, lyrics persist across close/reopen, and lyric clearing/updating works without identity changes.
- [x] 3.3 Thread selected sidecar-or-embedded text through LibraryScanner into repository; bound values before persistence.
- [x] 3.4 Run database and Library implementation JVM/iOS test-code checks and review persistence/migration evidence.

## 4. Shared projection and LRC parser

- [x] 4.1 Add parser tests for centisecond/millisecond timestamps, multiple timestamps per line, offsets, malformed lines, source-order ties, zero clamp, and plain text.
- [x] 4.2 Implement the common immutable lyric parser and nullable lyrics in shared track/playable projections.
- [x] 4.3 Run parser, conversion, play-queue projection, and Shared compilation tests.

## 5. Now Playing lyrics surface

- [x] 5.1 Add production-composable semantic regressions for no-lyrics action absence, active timed-line state, scroll reachability at compact and split sizes, and modal dismissal.
- [x] 5.2 Implement the optional localized lyrics dialog/surface driven by current playback position; lyrics actions do not invoke playback or mutate playback state.
- [x] 5.3 Run Now Playing semantics/JVM tests and EN/ZH resource verification; independent UI-boundary review is in progress.

## 6. Change acceptance and lifecycle

- [x] 6.1 Run the affected database, TagLib, Library, Shared, Now Playing, desktop, Android-host, and iOS compile/test matrix; run Spotless, Detekt, architecture, strict OpenSpec, and diff checks separately.
- [x] 6.2 Run a permanent temporary-media regression using a real tagged WAV and sidecar through native TagLib, JVM scanner, database close/reopen, and lyrics parser; assert source media hash remains unchanged. Shared playable projection is covered by the separate mapping test.
- [x] 6.3 Obtain independent stable-revision review and fix/re-review findings.
- [x] 6.4 Sync canonical `local-lyrics` specification, archive the OpenSpec change, and update roadmap/progress with manual acceptance assigned to the user.
