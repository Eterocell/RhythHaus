# Design

## Context

See [proposal.md](proposal.md) for motivation and scope. Library scanning already reads embedded TagLib metadata into `AudioMetadata`, persists `LibraryTrack` through SQLDelight, and projects shared `Track`/`PlayableTrack` objects. Android SAF and desktop/iOS filesystem scans have sibling access; Android MediaStore exposes readable audio descriptors but no general sidecar contract. TagLib has a properties-map API on JVM/Android; its iOS implementation currently returns an empty map. Now Playing is a feature leaf fed by Shared and already observes playback position.

## Goals / Non-Goals

**Goals:**
- Select one bounded lyric document per stable library track, with same-stem `.lrc` taking precedence over embedded lyrics.
- Preserve the existing audio source key and playback path; lyric access must not be needed during playback.
- Parse timed and untimed LRC in common code and render it with the current Now Playing position.
- Preserve behavior for tracks without lyrics and for every pre-lyrics database.

**Non-Goals:**
- Remote lyrics, account/network access, lyric editing, media-file writes, lyric search, or multiple sidecar/language selection.
- Android MediaStore sibling sidecar lookup.
- Automatic discovery outside an explicit library scan.
- Support for arbitrary embedded lyric formats beyond TagLib's normalized LYRICS property and the existing cross-platform TagLib reader contract.

## Decisions

1. **Persist selected text on `library_track`.** Add nullable `lyrics TEXT` with the next SQLDelight migration. The value is selected at scan time, not resolved from a path during playback; this avoids holding SAF security access, adding a second file lifecycle, or making Now Playing perform I/O. A successful scan refreshes or clears lyrics according to current readable candidates; an unreadable audio candidate retains its prior track through existing scan preservation behavior.
2. **Bound before persistence.** Accept only non-empty normalized UTF-8 lyric text up to 512 KiB. Oversized/unreadable sidecars are unavailable and allow embedded fallback. Do not truncate; truncation can remove final timing/offset metadata and misrepresent content.
3. **Sidecar precedence and access.** Candidate name is the audio filename with its final extension replaced by `.lrc`, matched case-insensitively in the same directory. Do not recurse or search album folders. Filesystem scanners read only that sibling. SAF scanning already has each directory's `DocumentFile` children and can pass an exact sibling match; it must read at most 512 KiB plus one byte. MediaStore does not query arbitrary non-audio rows. iOS and desktop resolve the sibling through the already-authorized scanned directory.
4. **Embedded extraction.** Normalize the TagLib `LYRICS` property into `TagMetadata.lyrics` for Android/JVM and extend the native metadata result for iOS so all scanned targets use the same field. Android URI metadata continues to use the already-opened descriptor and the existing descriptor lifetime; no URI is copied to disk. Missing/malformed optional lyrics do not invalidate other metadata.
5. **Common projection and parser.** Add nullable lyrics to `AudioMetadata`, `LibraryTrack`, persisted mapper, and shared `Track`/`PlayableTrack` projections. Parse in a new common playback/UI utility into immutable `(timestampMillis?, text, sourceOrder)` entries. Support `[mm:ss.xx]`, `[mm:ss.xxx]`, multiple timestamps per line, and `[offset:+/-N]` milliseconds. Malformed timed syntax does not discard the raw line; valid timestamps sort by effective time and retain source order for ties. Clamp negative effective times to zero. Plain lines remain visible.
6. **Now Playing interaction.** Add an explicit lyrics action only when non-empty lyrics exist. Opening exposes a vertically scrollable dialog/surface with timed-line semantics and current-line highlight driven by `playbackState.positionMillis`; it neither owns playback nor writes playback state. Keep the existing compact/split adaptive shell and expose only feature-owned EN/ZH copy.
7. **Migration and versioning.** Add nullable text to the current library track table and a schema migration at the next version. Update database version/bootstrap detection and real migration/reopen tests. Existing databases migrate to `lyrics = NULL`; no historical sidecar rescan or content write is performed during migration.
8. **Test boundaries.** Test the common parser with real multiline strings; test TagLib native fixtures for embedded properties; test scanner sibling precedence, size boundaries, and failure fallback using temporary files/SAF fixtures; test SQL migration/reopen; test the production Now Playing UI at timed and untimed states and verify no playback action occurs from lyric interaction.

## Risks / Trade-offs

- TagLib property normalization varies by container. Tests should include at least a supported property fixture and the iOS native ABI mapping; unsupported formats simply produce no embedded lyrics.
- Persisting lyric text increases per-track database storage, bounded at 512 KiB. The limit is high enough for ordinary lyrics but prevents unbounded metadata blobs.
- Sidecar bytes can change without audio-file metadata changing. Explicit rescans must reread sibling lyrics for existing tracks even when the audio identity is unchanged; the scanner's existing unchanged-track fast path must not bypass lyric refresh.
- Android SAF sidecar access can fail independently of audio access. Such failure is optional and must not produce a failed audio candidate.
- Timed line scrolling and accessibility semantics can vary across compact/wide layouts; both must be covered with the real production composable.
- Database version-zero detection has prior version-specific bootstrapping rules; test migration from the current version and legacy version-zero databases to prevent incorrect migration routing.
