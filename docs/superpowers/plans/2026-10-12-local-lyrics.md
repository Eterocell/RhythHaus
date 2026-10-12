# Implementation plan: Local and embedded lyrics

This is the executable plan for the approved design in `docs/superpowers/specs/2026-10-12-local-lyrics-design.md` and OpenSpec change `local-lyrics`. Implement one task group at a time; do not advance from an unreviewed persistence or cross-platform API checkpoint.

## 1. TagLib embedded lyric extraction

**Files:** `taglib/src/commonMain`, `taglib/src/jvmMain`, `taglib/src/androidMain`, `taglib/src/iosMain`, `taglib/native`, their reader tests; `feature/library/impl/.../AudioMetadata.kt` and tests.

1. Add failing common/JVM tests for `LYRICS` property normalization and missing/blank values. Add a native temporary-file fixture with a `LYRICS` property, read through JVM and Android-native bridge. Add an iOS mapping regression at the exposed C metadata boundary.
2. Extend `TagMetadata` and native result mapping with nullable lyrics. For JVM/Android, read TagLib's `LYRICS` property while retaining existing `readProperties` behavior. For iOS, export the property through the existing `RhTagLibMetadata` C result and map to Kotlin.
3. Normalize non-empty UTF-8 strings against 512 KiB before returning `AudioMetadata`; preserve all other fields if the property is absent or invalid.
4. Run `:taglib:jvmTest`, `:taglib:androidHostTest`, native CTest, and iOS compilation/tests. Review C/JNI/Native ABI ownership before the next group.

## 2. Bounded exact-sibling scan lookup

**Files:** `AudioScanCandidate`, common scan helpers, JVM/iOS/Android scanner adapters, `LibraryScanner` and scanner tests.

1. Add RED tests for exact sibling matching, `.LRC` case-insensitive extension, absent sibling, read exception, exact 512 KiB acceptance, 512 KiB+1 rejection, and fallback from rejected sidecar to embedded text.
2. Add an optional `sidecarLyrics` candidate value. The platform scanner supplies text only after bounded reads. JVM/iOS form the same-stem sibling path under the already-authorized directory. Android SAF matches the same-directory `DocumentFile` before opening its descriptor and reads no more than the configured bound plus one byte. MediaStore candidates always omit sidecar text.
3. Resolve sidecar-vs-embedded precedence in LibraryScanner after metadata extraction. Ensure readable candidates refresh metadata/lyrics during explicit rescans even when known audio size/time are unchanged, while retaining track IDs and existing user-owned state. Preserve current unreadable audio identity behavior.
4. Run Library scanner/source JVM, Android-host and iOS tests. Review handle/path boundaries and ensure no copy, permission, or network behavior was added.

## 3. SQLDelight and Library persistence

**Files:** `LibraryTrack.sq`, next `migrations/*.sqm`, database factory/version bootstrap, `LibraryTrack`, repository mapper/API, real-file database and scanner tests.

1. Add failing lifecycle tests before persistence code: migration from current v8 and legacy version-zero state, preserving sources/tracks/favorites/history/metadata overrides/playlists; lyrics null for old rows; write/update/clear lyric text and verify after close/reopen.
2. Add nullable `lyrics TEXT` and update every SQL projection and `upsertTrack`. Add the next migration and update user-version initialization for version-zero databases without changing earlier migration meaning.
3. Carry the selected lyric text into `LibraryTrack`; update every repository mapping and test fixture. Existing track identity remains keyed by source plus local key.
4. Run `:core:database:jvmTest :feature:library:impl:jvmTest :core:database:linkDebugFrameworkIosArm64` or the repository's supported iOS database task. Review generated SQLDelight mappings, migration proof, and all constructors.

## 4. Shared projection and parser

**Files:** core shared `Track`/`PlayableTrack`, Library conversion and `AudioMetadata`, new common parser plus common tests.

1. Add RED parser tests for `[mm:ss.xx]`, `[mm:ss.xxx]`, repeated timestamps, positive/negative offsets, stable ties, malformed syntax, negative adjusted times, empty text and untimed lines.
2. Implement the bounded immutable parser and add nullable lyrics to shared projections; thread through Library-to-Track and Library-to-PlayableTrack conversion.
3. Verify sorting does not reorder the source document except for timed events as specified, and plain text is preserved as-is after normalization.
4. Run parser/common model tests, Library conversion tests, Core Playback tests unaffected by lyrics, and Shared JVM/iOS compilation.

## 5. Now Playing presentation

**Files:** `feature/nowplaying` content/adaptive layout/resources/semantic tests; Shared route mapping only if needed.

1. Add production-composable RED tests for action absent without lyrics; dialog present with plain lyrics; correct active line at a supplied position; compact/split scroll reachability; accessible selected/current-line semantics; and unchanged playback state/queue/position before and after opening/closing.
2. Implement localized English/Simplified Chinese lyrics action and scrollable surface. Timed lines highlight from current `PlaybackState.positionMillis` without effects that seek. Keep no-lyrics content out of accessibility and pointer trees.
3. Run all Now Playing JVM/common tests, Shared tests, resource ownership/localization checks, desktop compilation, Android Shared compilation, and iOS framework/test-code compilation.
4. Obtain independent UI/code review and repair/re-review any findings.

## 6. Integration acceptance and closeout

1. Run focused TagLib/native, database, Library, parser, Now Playing, Shared, Android host, desktop, Android main, and iOS compilation/test matrix. Run `spotlessApply`, `spotlessCheck`, `detekt`, and `architectureCheck` separately. Run `openspec validate local-lyrics --strict`, `openspec validate --specs`, and `git diff --check`.
2. Create a temporary tagged audio fixture plus a same-stem `.lrc`, exercise actual scan -> SQLDelight persistence -> close/reopen -> Shared track projection -> parser. Assert sidecar wins, parser emits expected timed lines, ID remains stable on rescan, bytes are unchanged, and oversized sidecar falls back to embedded. Remove all throwaway code/media after proof.
3. Update implementation task evidence and obtain stable-revision final independent review. Do not claim full `./init.sh`, Android APK, physical device, visual, or listening acceptance unless each is actually run.
4. Sync canonical `local-lyrics` and `local-library-scanning` specs, archive the OpenSpec change, update `roadmap.md` and `progress.md`, and commit the completed OpenSpec workflow with a conventional message. Record user manual acceptance as pending.

## Release acceptance owned by the user

- EN and ZH Now Playing lyrics reachability and accessibility on compact and wide layouts.
- Timed line follows real playback without seeking or affecting playback controls.
- Rescan and restart preserve selected local lyrics; unavailable/malformed/oversized sources degrade to embedded or no lyrics.
- Android SAF sidecar acceptance and iOS/desktop sibling-file acceptance where devices/windows are available.
